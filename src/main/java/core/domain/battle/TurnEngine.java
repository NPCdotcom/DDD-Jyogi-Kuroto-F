package core.domain.battle;

import core.domain.card.Card;
import core.domain.card.CardEffect;
import core.domain.card.CardPileState;
import core.domain.card.TrapLifetime;
import core.domain.common.Direction;
import core.domain.common.Position;
import core.domain.dungeon.DungeonState;
import core.domain.dungeon.PlacedTrap;
import core.domain.dungeon.Tile;
import core.domain.entity.ActorId;
import core.domain.entity.Enemy;
import core.domain.entity.Player;
import core.domain.entity.Stats;
import core.domain.meta.Soul;
import core.domain.skill.Skill;
import core.domain.skill.SkillEffect;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * 戦闘・移動・ターン進行を司る純関数ユーティリティ。
 *
 * <p>I/O・乱数・時間に依存しない。入力 (DungeonState + BattleAction) から、新しい DungeonState と 発生イベントのリストを {@link
 * StepResult} として返す。
 *
 * <p>原則:
 *
 * <ul>
 *   <li>状態を渡された時点のスナップショットとして扱い、副作用を起こさない
 *   <li>失敗した行動 (AP 不足・行き先ブロック・フェーズ不一致など) も状態を変えず {@link BattleEvent.ActionRejected} のみを返す
 *   <li>switch 式の網羅性で全アクションを処理する (sealed の意義)
 * </ul>
 *
 * <p>§15-3 / ADR-18 で `BattleAction.UseCard` を追加、プレイヤーがカードを使って敵を殴れる。本 PR では Damage カードのみ実装し、
 * Move / Buff / Trap は `ActionRejected` で「未実装のカード効果」を明示 (sealed 網羅性は維持)。ダメージ計算は ADR-17 通り
 * `CardEffect.Damage.resolve(Stats, Stats, CardElement)` に委譲、本クラスは結果の int を Stats に反映するだけ。
 */
public final class TurnEngine {

  private TurnEngine() {}

  // ===================================================================================
  //  プレイヤーターン処理
  // ===================================================================================

  /** プレイヤーのアクションを 1 つ解決する。 */
  public static StepResult resolvePlayerAction(DungeonState state, BattleAction action) {
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(action, "action");
    if (state.phase() != TurnPhase.PLAYER_TURN) {
      return reject(state, state.player().id(), "プレイヤーのターンではない");
    }
    return switch (action) {
      case BattleAction.Move move -> applyPlayerMove(state, move.direction());
      case BattleAction.UseSkill use -> applyPlayerSkill(state, use.slotIndex());
      case BattleAction.UseCard use -> applyPlayerUseCard(state, use);
      case BattleAction.Wait ignored -> applyPlayerWait(state);
      case BattleAction.EndTurn ignored -> endPlayerTurn(state);
    };
  }

  /**
   * 敵ターン → プレイヤーターン遷移時に、プレイヤーの AP リフィル + 手札補充ドロー (§15-3 / ADR-19)。
   *
   * <p>処理順:
   *
   * <ol>
   *   <li>AP を速度ステ分まで全リセット (使い切り型、ADR-01)
   *   <li>山札から 1 枚ドロー (手札上限 9 枚で停止、山札切れ時は捨て札を {@code rng} で再シャッフル)
   * </ol>
   *
   * <p>Random は引数注入 (ドメイン副作用ゼロ、ADR-16 §4 と整合)。テスト時は固定シードで決定的に検証可能。
   *
   * @param state 遷移前の DungeonState (ENEMY_TURN 状態であること前提、検証は呼出側 TurnDirector に委ねる)
   * @param rng カード再シャッフル / 将来の確率処理に使う乱数源
   * @return AP リフィル + 1 枚ドロー済の新 DungeonState + TurnPhaseChanged イベント
   */
  public static StepResult startPlayerTurn(DungeonState state, Random rng) {
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(rng, "rng");
    Player p = state.player();
    // ADR-21: pendingMoveCount はターンまたぎ持ち越さない (移動権は当該ターンに使い切る)。
    Player refreshed =
        p.withActionPoints(p.actionPoints().refilledTo(p.stats().speed()))
            .withCardPileState(p.cardPileState().drawN(1, rng))
            .withPendingMoveCount(0);
    // ADR-22: Turns 罠の remaining-- + expired (=0) を除去。UntilStepped 罠は据置。
    List<PlacedTrap> aliveTraps = new ArrayList<>(state.placedTraps().size());
    for (PlacedTrap t : state.placedTraps()) {
      PlacedTrap decremented = t.decrementedLifetime();
      if (decremented.isAlive()) {
        aliveTraps.add(decremented);
      }
    }
    DungeonState ns =
        state.withPlayer(refreshed).withPlacedTraps(aliveTraps).withPhase(TurnPhase.PLAYER_TURN);
    return new StepResult(ns, List.of(new BattleEvent.TurnPhaseChanged(TurnPhase.PLAYER_TURN)));
  }

  // ===================================================================================
  //  敵ターン処理
  // ===================================================================================

  /** 敵 1 体のアクションを解決する。 */
  public static StepResult resolveEnemyAction(
      DungeonState state, ActorId enemyId, BattleAction action) {
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(enemyId, "enemyId");
    Objects.requireNonNull(action, "action");
    if (state.phase() != TurnPhase.ENEMY_TURN) {
      return reject(state, enemyId, "敵のターンではない");
    }
    Optional<Enemy> enemyOpt = state.findEnemy(enemyId);
    if (enemyOpt.isEmpty()) {
      return reject(state, enemyId, "敵が存在しない");
    }
    Enemy enemy = enemyOpt.get();
    return switch (action) {
      case BattleAction.Move move -> applyEnemyMove(state, enemy, move.direction());
      case BattleAction.UseSkill use -> applyEnemySkill(state, enemy, use.slotIndex());
      // §15-3 / ADR-18: 敵はカードを使わない (Skill ベース)。誤って UseCard が渡されたら reject。
      case BattleAction.UseCard ignored -> reject(state, enemy.id(), "敵はカードを使えない");
      case BattleAction.Wait ignored -> applyEnemyWait(state, enemy);
      case BattleAction.EndTurn ignored -> new StepResult(state, List.of());
    };
  }

  /** プレイヤーターン終了時に、全敵の AP を回復させて ENEMY_TURN に遷移させる。 */
  private static StepResult endPlayerTurn(DungeonState state) {
    List<Enemy> regenerated = new ArrayList<>(state.enemies().size());
    for (Enemy e : state.enemies()) {
      regenerated.add(e.withActionPoints(e.actionPoints().refilledTo(e.stats().speed())));
    }
    DungeonState ns = state.withEnemies(regenerated).withPhase(TurnPhase.ENEMY_TURN);
    return new StepResult(ns, List.of(new BattleEvent.TurnPhaseChanged(TurnPhase.ENEMY_TURN)));
  }

  // ===================================================================================
  //  プレイヤー個別アクション
  // ===================================================================================

  /**
   * プレイヤー移動 (§15-5 / ADR-21)。{@code pendingMoveCount > 0} なら AP 消費なし (移動権使用)、それ以外は AP 1 消費 (通常移動)。
   *
   * <p>ADR-20 / ADR-21 「移動カード切る → distance ぶん AWSD で連続移動」を本メソッドで実装。
   */
  private static StepResult applyPlayerMove(DungeonState state, Direction direction) {
    Player player = state.player();
    boolean usingPendingMove = player.pendingMoveCount() > 0;
    if (!usingPendingMove && !player.actionPoints().canSpend(1)) {
      return reject(state, player.id(), "AP 不足");
    }
    Position next = player.position().move(direction);
    if (!state.map().isWalkable(next) || state.findEnemyAt(next).isPresent()) {
      return reject(state, player.id(), "そこへは移動できない");
    }
    Player moved;
    if (usingPendingMove) {
      // 移動権消費: AP 据置、pendingMoveCount を 1 減らす。
      moved = player.withPosition(next).withPendingMoveCount(player.pendingMoveCount() - 1);
    } else {
      // 通常移動: AP 1 消費。
      moved = player.withPosition(next).withActionPoints(player.actionPoints().spend(1));
    }
    DungeonState afterMove = state.withPlayer(moved);
    List<BattleEvent> events = new ArrayList<>();
    events.add(new BattleEvent.Moved(player.id(), player.position(), next));
    // 階段に到達したらフロア踏破 (CLEARED)。敵全滅では遷移しない。
    if (state.map().tileAt(next) == Tile.STAIRS_DOWN) {
      afterMove = afterMove.withPhase(TurnPhase.CLEARED);
      events.add(new BattleEvent.TurnPhaseChanged(TurnPhase.CLEARED));
    }
    // ADR-22: 罠踏み判定 (Player が罠タイルに進入したか)。CLEARED 時もダメージは入る (踏破直前の罠で死亡もあり得る)。
    afterMove = checkAndTriggerTrap(afterMove, moved.id(), next, true, events);
    return new StepResult(afterMove, events);
  }

  private static StepResult applyPlayerSkill(DungeonState state, int slotIndex) {
    Player player = state.player();
    Optional<Skill> skillOpt = player.skillSlot().at(slotIndex);
    if (skillOpt.isEmpty()) {
      return reject(state, player.id(), "スキル枠が空");
    }
    Skill skill = skillOpt.get();
    if (!player.actionPoints().canSpend(skill.apCost())) {
      return reject(state, player.id(), "AP 不足");
    }
    Optional<Enemy> targetOpt = findAdjacentEnemy(state, player.position());
    if (targetOpt.isEmpty()) {
      return reject(state, player.id(), "対象がいない");
    }
    Player afterCost = player.withActionPoints(player.actionPoints().spend(skill.apCost()));
    DungeonState afterCostState = state.withPlayer(afterCost);
    return applyEffectByPlayer(afterCostState, skill, targetOpt.get());
  }

  /**
   * カード使用 (§15-3 / ADR-18)。本 PR では Damage カードのみ実装、Move / Buff / Trap は ActionRejected で明示。
   *
   * <p>処理シーケンス: 手札範囲 → AP コスト → CardEffect で switch → 各効果。
   */
  private static StepResult applyPlayerUseCard(DungeonState state, BattleAction.UseCard action) {
    Player player = state.player();
    CardPileState piles = player.cardPileState();
    int handIndex = action.handIndex();
    if (handIndex >= piles.hand().size()) {
      return reject(state, player.id(), "手札範囲外");
    }
    Card card = piles.hand().get(handIndex);
    if (!player.actionPoints().canSpend(card.apCost())) {
      return reject(state, player.id(), "AP 不足");
    }
    return switch (card.effect()) {
      case CardEffect.Damage dmg ->
          resolveCardDamage(state, card, dmg, action.direction(), handIndex);
      case CardEffect.Move move -> resolveCardMove(state, card, move, handIndex);
      case CardEffect.Buff ignored -> reject(state, player.id(), "未実装のカード効果 (Buff)");
      case CardEffect.Trap trap ->
          resolveCardTrap(state, card, trap, action.direction(), handIndex);
    };
  }

  /**
   * Trap カード解決 (§15-3 / ADR-22)。指定方向の隣接マスに罠を設置 (壁不可、同座標既存罠は上書き)。
   *
   * <p>処理:
   *
   * <ol>
   *   <li>設置先タイル walkable チェック (壁不可)
   *   <li>同座標既存罠を除去 (上書き、3 並列レビュー結論「驚き最小 = 最新が優先」)
   *   <li>新 PlacedTrap を placedTraps に追加
   *   <li>AP 消費 + Hand→Discard
   *   <li>SkillUsed + TrapPlaced イベント発火
   * </ol>
   */
  private static StepResult resolveCardTrap(
      DungeonState state, Card card, CardEffect.Trap trap, Direction direction, int handIndex) {
    Player player = state.player();
    Position trapPos = player.position().move(direction);
    if (!state.map().isWalkable(trapPos)) {
      return reject(state, player.id(), "そこには罠を設置できない");
    }
    // 同座標重複は上書き (新規優先)
    List<PlacedTrap> newTraps = new ArrayList<>(state.placedTraps().size() + 1);
    for (PlacedTrap t : state.placedTraps()) {
      if (!t.position().equals(trapPos)) {
        newTraps.add(t);
      }
    }
    newTraps.add(new PlacedTrap(trapPos, trap.baseValue(), trap.lifetime(), card.element()));

    Player afterUse =
        player
            .withActionPoints(player.actionPoints().spend(card.apCost()))
            .withCardPileState(player.cardPileState().playFromHand(handIndex));
    DungeonState ns = state.withPlayer(afterUse).withPlacedTraps(newTraps);
    List<BattleEvent> events = new ArrayList<>();
    events.add(new BattleEvent.SkillUsed(player.id(), card.displayName()));
    events.add(new BattleEvent.TrapPlaced(player.id(), trapPos, trap.baseValue()));
    return new StepResult(ns, events);
  }

  /**
   * Move カード解決 (§15-5 / ADR-21)。AP コスト消費 + Hand→Discard + pendingMoveCount = distance 設定。
   *
   * <p>本メソッドではカード使用時点で移動は行わない (= 距離 1 でも UseCard 後の AWSD 押下で 1 マス進む)。これにより「カードを切る → 方向キーで動く」操作を
   * 明示的に分離 (移動 α 案、ADR-20)。
   */
  private static StepResult resolveCardMove(
      DungeonState state, Card card, CardEffect.Move move, int handIndex) {
    Player player = state.player();
    Player afterUse =
        player
            .withActionPoints(player.actionPoints().spend(card.apCost()))
            .withCardPileState(player.cardPileState().playFromHand(handIndex))
            .withPendingMoveCount(move.distance());
    DungeonState ns = state.withPlayer(afterUse);
    List<BattleEvent> events = new ArrayList<>();
    // ADR-18 / ADR-21: 使用宣言は SkillUsed 流用、追加で MovementGranted を発火 (HUD 表示用)。
    events.add(new BattleEvent.SkillUsed(player.id(), card.displayName()));
    events.add(new BattleEvent.MovementGranted(player.id(), move.distance()));
    return new StepResult(ns, events);
  }

  /**
   * Damage カードの解決。方向指定で隣接マスを取り、敵不在なら reject、存在すれば
   *
   * <ol>
   *   <li>{@link CardEffect.Damage#resolve} で最終ダメージを確定 (ADR-17)
   *   <li>AP 消費 + Hand→Discard 移動 (純関数操作)
   *   <li>{@link #resolveDamageToEnemy} 経由でダメージ反映 + 死亡判定
   * </ol>
   */
  private static StepResult resolveCardDamage(
      DungeonState state, Card card, CardEffect.Damage dmg, Direction direction, int handIndex) {
    Player player = state.player();
    Position targetPos = player.position().move(direction);
    Optional<Enemy> targetOpt = state.findEnemyAt(targetPos);
    if (targetOpt.isEmpty()) {
      return reject(state, player.id(), "対象がいない");
    }
    Enemy target = targetOpt.get();
    int finalDamage = dmg.resolve(player.stats(), target.stats(), card.element());
    Player afterAction =
        player
            .withActionPoints(player.actionPoints().spend(card.apCost()))
            .withCardPileState(player.cardPileState().playFromHand(handIndex));
    DungeonState afterCostState = state.withPlayer(afterAction);
    List<BattleEvent> events = new ArrayList<>();
    // ADR-18: BattleEvent.CardUsed は新設せず SkillUsed を流用 (displayName で識別、YAGNI)。
    events.add(new BattleEvent.SkillUsed(player.id(), card.displayName()));
    return resolveDamageToEnemy(afterCostState, finalDamage, target, events);
  }

  private static StepResult applyPlayerWait(DungeonState state) {
    Player player = state.player();
    if (!player.actionPoints().canSpend(1)) {
      return reject(state, player.id(), "AP 不足");
    }
    Player after = player.withActionPoints(player.actionPoints().spend(1));
    return new StepResult(state.withPlayer(after), List.of());
  }

  // ===================================================================================
  //  敵個別アクション
  // ===================================================================================

  private static StepResult applyEnemyMove(DungeonState state, Enemy enemy, Direction direction) {
    if (!enemy.actionPoints().canSpend(1)) {
      return reject(state, enemy.id(), "AP 不足");
    }
    Position next = enemy.position().move(direction);
    if (!state.map().isWalkable(next) || state.isPositionOccupied(next)) {
      return reject(state, enemy.id(), "そこへは移動できない");
    }
    Enemy moved = enemy.withPosition(next).withActionPoints(enemy.actionPoints().spend(1));
    DungeonState afterMove = state.withEnemyReplaced(moved);
    List<BattleEvent> events = new ArrayList<>();
    events.add(new BattleEvent.Moved(enemy.id(), enemy.position(), next));
    // ADR-22: 罠踏み判定 (Enemy が罠タイルに進入したか)。
    afterMove = checkAndTriggerTrap(afterMove, moved.id(), next, false, events);
    return new StepResult(afterMove, events);
  }

  private static StepResult applyEnemySkill(DungeonState state, Enemy enemy, int slotIndex) {
    Optional<Skill> skillOpt = enemy.skillSlot().at(slotIndex);
    if (skillOpt.isEmpty()) {
      return reject(state, enemy.id(), "スキル枠が空");
    }
    Skill skill = skillOpt.get();
    if (!enemy.actionPoints().canSpend(skill.apCost())) {
      return reject(state, enemy.id(), "AP 不足");
    }
    if (!enemy.position().isAdjacentTo(state.player().position())) {
      return reject(state, enemy.id(), "対象がいない");
    }
    Enemy afterCost = enemy.withActionPoints(enemy.actionPoints().spend(skill.apCost()));
    DungeonState afterCostState = state.withEnemyReplaced(afterCost);
    return applyEffectByEnemy(afterCostState, skill, enemy.id());
  }

  private static StepResult applyEnemyWait(DungeonState state, Enemy enemy) {
    if (!enemy.actionPoints().canSpend(1)) {
      return reject(state, enemy.id(), "AP 不足");
    }
    Enemy after = enemy.withActionPoints(enemy.actionPoints().spend(1));
    return new StepResult(state.withEnemyReplaced(after), List.of());
  }

  // ===================================================================================
  //  効果適用 (Damage etc.)
  // ===================================================================================

  private static StepResult applyEffectByPlayer(DungeonState state, Skill skill, Enemy target) {
    List<BattleEvent> events = new ArrayList<>();
    events.add(new BattleEvent.SkillUsed(state.player().id(), skill.displayName()));
    return switch (skill.effect()) {
      // Skill 経路: 固定ダメ (§15-3 設計、ADR-18 で意味論分離確認)。
      case SkillEffect.Damage dmg -> resolveDamageToEnemy(state, dmg.amount(), target, events);
    };
  }

  private static StepResult applyEffectByEnemy(
      DungeonState state, Skill skill, ActorId attackerId) {
    List<BattleEvent> events = new ArrayList<>();
    events.add(new BattleEvent.SkillUsed(attackerId, skill.displayName()));
    return switch (skill.effect()) {
      case SkillEffect.Damage dmg ->
          resolveDamageToPlayer(state, dmg.amount(), attackerId, events);
    };
  }

  /**
   * 敵にダメージを適用する共通ヘルパ (ADR-18 で int finalDamage 受け取りに統一)。
   *
   * <p>Skill 経路 (固定ダメ) と Card 経路 (CardEffect.Damage.resolve 計算結果) のどちらからも呼ばれる。
   */
  private static StepResult resolveDamageToEnemy(
      DungeonState state, int finalDamage, Enemy target, List<BattleEvent> events) {
    Stats damagedStats = target.stats().damaged(finalDamage);
    events.add(
        new BattleEvent.DamageDealt(
            state.player().id(), target.id(), finalDamage, damagedStats.currentHp()));
    if (!damagedStats.isAlive()) {
      events.add(new BattleEvent.ActorDied(target.id()));
      int reward = target.kind().soulReward();
      Player rewardedPlayer = state.player().addSoul(new Soul(reward));
      events.add(new BattleEvent.SoulGained(rewardedPlayer.id(), reward));
      DungeonState ns = state.withEnemyRemoved(target.id()).withPlayer(rewardedPlayer);
      // CLEARED への遷移は階段踏破で行う (applyPlayerMove)。
      // 敵全滅では CLEARED にしない: 敵 1 体撃破で即クリアになるのを避けるため。
      return new StepResult(ns, events);
    }
    Enemy hit = target.withStats(damagedStats);
    return new StepResult(state.withEnemyReplaced(hit), events);
  }

  /** プレイヤーにダメージを適用する共通ヘルパ (int finalDamage 受け取り、Skill / Card 両経路兼用)。 */
  private static StepResult resolveDamageToPlayer(
      DungeonState state, int finalDamage, ActorId attackerId, List<BattleEvent> events) {
    Player player = state.player();
    Stats damagedStats = player.stats().damaged(finalDamage);
    events.add(
        new BattleEvent.DamageDealt(
            attackerId, player.id(), finalDamage, damagedStats.currentHp()));
    Player hit = player.withStats(damagedStats);
    if (!damagedStats.isAlive()) {
      events.add(new BattleEvent.ActorDied(player.id()));
      DungeonState ns = state.withPlayer(hit).withPhase(TurnPhase.GAME_OVER);
      events.add(new BattleEvent.TurnPhaseChanged(TurnPhase.GAME_OVER));
      return new StepResult(ns, events);
    }
    return new StepResult(state.withPlayer(hit), events);
  }

  // ===================================================================================
  //  Helpers
  // ===================================================================================

  private static StepResult reject(DungeonState state, ActorId who, String reason) {
    return new StepResult(state, List.of(new BattleEvent.ActionRejected(who, reason)));
  }

  /**
   * 罠踏み判定 + ダメージ適用の共通ヘルパ (§15-3 / ADR-22)。プレイヤー / 敵どちらの移動でも呼ばれる。
   *
   * <ol>
   *   <li>{@code at} に罠があるか {@link DungeonState#findTrapAt} で確認、無ければ state をそのまま返す
   *   <li>{@link PlacedTrap#resolveDamage(Stats)} で最終ダメージを確定 (element に応じた物防/魔防参照)
   *   <li>victim の Stats を damaged で減算、{@link BattleEvent.TrapTriggered} 発火
   *   <li>{@link TrapLifetime.UntilStepped} なら罠を除去、{@link TrapLifetime.Turns} なら維持
   *   <li>victim 死亡時は ActorDied + プレイヤーなら GAME_OVER、敵なら撃破報酬 + 除去
   * </ol>
   */
  private static DungeonState checkAndTriggerTrap(
      DungeonState state,
      ActorId victimId,
      Position at,
      boolean isPlayer,
      List<BattleEvent> events) {
    Optional<PlacedTrap> trapOpt = state.findTrapAt(at);
    if (trapOpt.isEmpty()) {
      return state;
    }
    PlacedTrap trap = trapOpt.get();
    Stats victimStats =
        isPlayer ? state.player().stats() : state.findEnemy(victimId).orElseThrow().stats();
    int damage = trap.resolveDamage(victimStats);
    Stats damagedStats = victimStats.damaged(damage);
    events.add(new BattleEvent.TrapTriggered(victimId, at, damage, damagedStats.currentHp()));

    // UntilStepped なら除去、Turns なら維持 (3 並列レビュー結論、物理/魔法の対比)。
    List<PlacedTrap> updatedTraps = new ArrayList<>(state.placedTraps().size());
    for (PlacedTrap t : state.placedTraps()) {
      if (t.position().equals(at)) {
        if (t.lifetime() instanceof TrapLifetime.Turns) {
          updatedTraps.add(t);
        }
        // UntilStepped は除去 (updatedTraps に追加しない)
      } else {
        updatedTraps.add(t);
      }
    }
    DungeonState ns = state.withPlacedTraps(updatedTraps);

    if (isPlayer) {
      Player victimPlayer = state.player().withStats(damagedStats);
      ns = ns.withPlayer(victimPlayer);
      if (!damagedStats.isAlive()) {
        events.add(new BattleEvent.ActorDied(victimPlayer.id()));
        ns = ns.withPhase(TurnPhase.GAME_OVER);
        events.add(new BattleEvent.TurnPhaseChanged(TurnPhase.GAME_OVER));
      }
    } else {
      Enemy victimEnemy = state.findEnemy(victimId).orElseThrow().withStats(damagedStats);
      if (!damagedStats.isAlive()) {
        events.add(new BattleEvent.ActorDied(victimId));
        int reward = victimEnemy.kind().soulReward();
        Player rewardedPlayer = ns.player().addSoul(new Soul(reward));
        events.add(new BattleEvent.SoulGained(rewardedPlayer.id(), reward));
        ns = ns.withEnemyRemoved(victimId).withPlayer(rewardedPlayer);
      } else {
        ns = ns.withEnemyReplaced(victimEnemy);
      }
    }
    return ns;
  }

  private static Optional<Enemy> findAdjacentEnemy(DungeonState state, Position from) {
    for (Enemy e : state.enemies()) {
      if (from.isAdjacentTo(e.position())) {
        return Optional.of(e);
      }
    }
    return Optional.empty();
  }

  /** 1 ステップの解決結果。state は常に有効で、accepted 判定は events 側の有無で行う。 */
  public record StepResult(DungeonState state, List<BattleEvent> events) {
    public StepResult {
      Objects.requireNonNull(state, "state");
      Objects.requireNonNull(events, "events");
      events = List.copyOf(events);
    }

    public boolean wasRejected() {
      return events.stream().anyMatch(e -> e instanceof BattleEvent.ActionRejected);
    }
  }
}
