package core.presentation.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import core.application.GameContext;
import core.application.RunSession;
import core.application.TurnDirector;
import core.domain.battle.TurnEngine.StepResult;
import core.domain.card.Card;
import core.domain.card.CardId;
import core.domain.card.CardPileState;
import core.domain.dungeon.DungeonState;
import core.domain.entity.EnemyKind;
import core.domain.entity.Player;
import core.domain.equipment.Equipment;
import core.domain.equipment.EquipmentSlot;
import core.domain.layer.LayerEndNode;
import core.domain.meta.PlayerProgress;
import core.domain.meta.Soul;
import core.domain.tree.NodeId;
import core.domain.tree.SoulTree;
import core.infrastructure.audio.SoundManager;
import core.infrastructure.bootstrap.CardImageRegistry;
import core.infrastructure.bootstrap.InitialStateFactory;
import core.infrastructure.save.SaveData;
import core.infrastructure.save.SaveDataConverter;
import core.infrastructure.save.SaveManager;
import core.infrastructure.save.Settings;
import core.infrastructure.save.SettingsManager;
import core.presentation.render.Fonts;
import core.presentation.render.GameResources;
import core.presentation.render.UiTheme;
import core.presentation.render.UiThemeResolver;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * LibGDX の {@link Game} を継承したエントリポイント。
 *
 * <p>presentation 層に置く理由: LibGDX 依存 (Game 継承 + Screen 切替) を application 層から 切り離すため。application 層は
 * LibGDX に依存しない {@link GameContext} と {@link TurnDirector} のみで構成される。
 *
 * <p>{@link GameContext} / {@link TurnDirector} / {@link Fonts} を 1 つの所有者 (この Game) に集約する
 * ことで、Screen 側が古い参照を保持して新ラン後に状態を取りこぼす事故を構造的に防ぐ。Screen からは {@link #context()} / {@link #director()}
 * / {@link #fonts()} で都度取得する。
 */
public final class DddGame extends Game {

  /**
   * ラン単位の application 層状態の集約 (Wave 9 W9-α、{@link RunSession} record)。
   *
   * <p>従来 3 個別フィールド (context / director / runRng) として保持していたものを 1 つの Optional に置換。 ラン未開始の意味を {@code
   * Optional.empty()} で型表現することで、{@code if (context == null) return} 等の null チェック散在を撤廃する。
   *
   * <p>Screen 側からのアクセスは {@link #requireRunSession()} を推奨 ({@link IllegalStateException} を含み、
   * ラン未開始時の NoSuchElementException 即死を回避してデバッグ性を高める)。
   */
  private Optional<RunSession> runSession = Optional.empty();

  /**
   * LibGDX リソース系 3 コンポーネント (Fonts / SoundManager / CardImageRegistry) の集約 (Wave 8 W8-β)。
   *
   * <p>create() で {@link GameResources#load} により一括初期化、dispose() で防衛的解放。 旧 3 個別フィールドへの直接参照は中継 getter
   * ({@link #fonts()} 等) 経由に置換済。
   */
  private GameResources resources;

  /**
   * ラン外で持続するプレイヤー進捗を集約した不変 record (§15-2/-5/-7/-9/-10/-11、Wave 6 W6-γ で導入)。
   *
   * <p>従来 DddGame に散在していた 7 個別フィールド (soulTree / playerSoul / tutorialSeen / runCount /
   * obtainedCards / bestiary / loadout) を 1 集約に置換。各 setter は {@code progress =
   * progress.withXxx(...)} で新インスタンスを返す純関数 + 代入で更新する (Escape Analysis でチェイン生成は GC 負荷なし)。
   *
   * <p>Wave 7 W7-β で {@link #progress()} を公開し、旧 7 中継 getter (playerSoul / runCount / soulTree /
   * bestiary / loadout / obtainedCards / isTutorialSeen) は撤去済。Screen 側は {@code
   * game.progress().playerSoul()} 等で field にアクセスする。
   */
  private PlayerProgress progress = PlayerProgress.initial(defaultLoadout());

  /** セーブ / ロードを担当する (§15-11)。 */
  private final SaveManager saveManager = new SaveManager();

  /** 設定の永続化を担当する (§15-1)。 */
  private final SettingsManager settingsManager = new SettingsManager();

  /** 現在の設定 (§15-1)。起動時にロードし、SettingsScreen で更新する。 */
  private Settings settings = Settings.DEFAULT;

  private static Map<EquipmentSlot, Equipment> defaultLoadout() {
    Equipment dagger = InitialStateFactory.tatteredDagger();
    Map<EquipmentSlot, Equipment> m = new HashMap<>();
    m.put(dagger.slot(), dagger);
    return m;
  }

  /**
   * 現在のラン状態を返す (Wave 9 W9-α、ラン未開始時は {@link Optional#empty()})。内部利用 (saveAtLayerBoundary 等)
   * で「ラン未開始ならスキップ」を表現するときに使う。Screen 側は通常 {@link #requireRunSession()} を使うこと。
   */
  public Optional<RunSession> runSession() {
    return runSession;
  }

  /**
   * 現在のラン状態を返す。ラン未開始時は {@link IllegalStateException} を投げる (Wave 9 W9-α、CTO レビュー反映 #2)。
   *
   * <p>Screen 側から `game.requireRunSession().context()` 等で参照する。`.get()` で NoSuchElementException 即死を
   * 回避し、IllegalStateException + 明示メッセージでデバッグ性を高める。
   */
  public RunSession requireRunSession() {
    return runSession.orElseThrow(() -> new IllegalStateException("Run session not active"));
  }

  /**
   * ラン外進捗 record を返す (Wave 7 W7-β で公開化)。Screen 側からは {@code game.progress().playerSoul()} 等で
   * フィールドにアクセスする。本メソッド経由でアクセスを集約することで、DddGame の API 表面積を縮小する (旧中継 getter 7 件を撤去)。
   */
  public PlayerProgress progress() {
    return progress;
  }

  /**
   * 現在のロードアウトから UI テーマを動的に決定する (§7-2 / W4-ε)。
   *
   * <p>主武器優先で {@code themeName} が設定されている装備を探し、{@link UiThemeResolver} で解決する。 全装備が {@code
   * themeName=empty} の場合は {@link UiTheme#defaultTheme()} にフォールバック。
   */
  public UiTheme activeUiTheme() {
    return UiThemeResolver.resolve(progress.loadout());
  }

  /** 装備をそのスロットに装着する (同スロットの既存装備は置き換え、§15-9、次ラン開始時に反映)。 */
  public void equipInLoadout(Equipment equipment) {
    Objects.requireNonNull(equipment, "equipment");
    Map<EquipmentSlot, Equipment> next = new HashMap<>(progress.loadout());
    next.put(equipment.slot(), equipment);
    progress = progress.withLoadout(next);
  }

  /**
   * 指定スロットの装備を外す (§15-9、次ラン開始時に反映)。
   *
   * <p>ロードアウトを空にはできない (最後の 1 個は外せない)。空ロードアウト → 空デッキでラン開始すると 攻撃手段ゼロで詰むため。
   */
  public void unequipSlot(EquipmentSlot slot) {
    Objects.requireNonNull(slot, "slot");
    if (progress.loadout().size() <= 1) {
      return; // 最後の装備は外さない (空デッキ防止)
    }
    Map<EquipmentSlot, Equipment> next = new HashMap<>(progress.loadout());
    next.remove(slot);
    progress = progress.withLoadout(next);
  }

  /**
   * 敵を撃破したことを Bestiary に記録する (§15-5 / E-7、DungeonScreen の ActorDied ハンドラから呼ぶ)。
   *
   * @param kind 撃破した敵の種別
   */
  public void recordEnemyDefeated(EnemyKind kind) {
    progress = progress.withBestiary(progress.bestiary().withDefeated(kind));
  }

  /** 現在の Player が保持する全カード (山札 + 手札 + 捨て札) の ID を入手済として記録する。 */
  private void recordObtainedCards() {
    if (runSession.isEmpty()) {
      return;
    }
    CardPileState piles = runSession.get().context().state().player().cardPileState();
    Set<CardId> next = new HashSet<>(progress.obtainedCards());
    for (Card c : piles.drawPile().cards()) {
      next.add(c.id());
    }
    for (Card c : piles.hand().cards()) {
      next.add(c.id());
    }
    for (Card c : piles.discardPile().cards()) {
      next.add(c.id());
    }
    progress = progress.withObtainedCards(next);
  }

  public Fonts fonts() {
    return resources.fonts();
  }

  /** サウンドマネージャ (BGM / SE 再生の窓口)。 */
  public SoundManager soundManager() {
    return resources.soundManager();
  }

  /** カード画像レジストリ (CardCollectionScreen / HudRenderer から参照)。 */
  public CardImageRegistry cardImageRegistry() {
    return resources.cardImageRegistry();
  }

  /**
   * カードマスタ (§15-3)。presentation 層が {@code InitialStateFactory.cardCatalog()} を直接呼ぶと presentation →
   * infrastructure 依存方向違反になるため、DddGame 経由で間接化する窓口。
   */
  public core.infrastructure.bootstrap.CardCatalog cardCatalog() {
    return core.infrastructure.bootstrap.InitialStateFactory.cardCatalog();
  }

  /**
   * 装備マスタ (§15-9)。{@link #cardCatalog()} と同型の窓口 (Wave 3 Task A で追加)。 NodeResolveContext 組み立てや装備画面が
   * presentation → infrastructure の直接参照を避けるために使う。
   */
  public core.infrastructure.bootstrap.EquipmentCatalog equipmentCatalog() {
    return core.infrastructure.bootstrap.InitialStateFactory.equipmentCatalog();
  }

  /**
   * 層末ノード解決用の {@link core.domain.layer.NodeResolveContext} を組み立てて返す (Wave 3 Task A)。 cards /
   * equipments の resolver はマスタ {@code get} method reference で構成、ドメイン層は Function のみ参照する。
   */
  public core.domain.layer.NodeResolveContext nodeResolveContext() {
    return new core.domain.layer.NodeResolveContext(
        id -> cardCatalog().get(id), id -> equipmentCatalog().get(id));
  }

  /** §15-10 / E-10: チュートリアル overlay を閉じた時に呼び、次回以降の自動表示を抑制する。 */
  public void markTutorialSeen() {
    progress = progress.withTutorialSeen(true);
  }

  /** ラン外のソウルツリー画面でノード解放した結果を受け取る (§15-7)。 */
  public void unlockTreeNode(NodeId nodeId) {
    SoulTree.UnlockResult result = progress.soulTree().unlock(nodeId, progress.playerSoul());
    progress = progress.withSoulTree(result.newTree()).withPlayerSoul(result.newSoul());
  }

  /** ラン外のソウルツリー画面でリセットボタン押下時に呼ぶ (ADR-09)。 */
  public void resetTree() {
    SoulTree.ResetResult result = progress.soulTree().reset();
    progress =
        progress
            .withSoulTree(result.newTree())
            .withPlayerSoul(progress.playerSoul().add(result.refundedSoul()));
  }

  /** ラン終了時 (GameOverScreen 表示時) に Player.soul をラン外保持に書き戻す (§15-7)。 */
  public void preserveSoulFromRun() {
    runSession.ifPresent(
        session -> progress = progress.withPlayerSoul(session.context().state().player().soul()));
  }

  /**
   * ラン終了時 (GameOverScreen 表示時) に呼ぶ (§15-7 / E-2)。{@link #preserveSoulFromRun()} で 獲得ソウルをラン外保持に退避し、
   * runCount を 1 増やす。runCount が 1 以上になることで 以降ソウルツリー動線 (タイトルの T キー) が解禁される。
   *
   * <p>Wave 9 W9-α CTO レビュー反映 #1: ラン揮発状態を {@link Optional#empty()} で明示クリアし、前回ランの GameContext が
   * 次回ランにリバウンドする事故を構造的に防ぐ。Android (Phase D) のメモリ圧迫回避にも寄与。
   */
  public void onRunEnded() {
    preserveSoulFromRun();
    progress = progress.withRunCount(progress.runCount() + 1);
    runSession = Optional.empty();
  }

  /** 新しいラン (= ダンジョン挑戦) を開始する。RunSession を作り直す。 */
  public void startNewRun() {
    // ADR-19: Random は引数注入で再現性を呼出元に委ねる (初期手札シャッフル + 毎ターンドロー)。
    // ラン毎に new Random() で異なるシード = 本番プレイは非再現的 (テストでは固定シードを渡す)。
    Random newRng = new Random();
    // §15-6 / SoulTree LayerExtend: 解放済みの LayerExtendEffect の合計値からラン最大層数を算出。
    // generateLayerState がボス配置のために最大層数を必要とするため、firstFloor 前に計算する。
    int extendAmount = totalLayerExtendAmount();
    int maxLayer = InitialStateFactory.DEFAULT_MAX_LAYER + extendAmount;
    DungeonState state = InitialStateFactory.firstFloor(newRng, progress.loadout(), maxLayer);
    // §15-7: ソウルツリーの解放済み効果を Player に適用 (素ステ補正 / カード追加 / 枠拡張)
    // LayerExtendEffect は Player に副作用がないため、ループは no-op (副作用は GameContext.maxLayer 側で集約済)。
    Player applied = progress.soulTree().applyTo(state.player(), InitialStateFactory::resolveCard);
    // §15-2 / §15-7: ラン外のソウル保持を Player に注入 (前回ランからの持ち越し)
    Player withSoul = applied.addSoul(progress.playerSoul());
    // 注入後は外部保持を 0 に (重複加算防止、preserveSoulFromRun でラン終了時に書き戻る)
    progress = progress.withPlayerSoul(Soul.zero());
    GameContext newContext = GameContext.startNewRun(state.withPlayer(withSoul));
    if (extendAmount > 0) {
      newContext.extendMaxLayer(extendAmount);
    }
    TurnDirector newDirector = new TurnDirector(newContext, newRng);
    runSession = Optional.of(new RunSession(newContext, newDirector, newRng));
    recordObtainedCards(); // §15-3: 初期デッキを図鑑に記録
  }

  /**
   * 解放済みノードの {@link core.domain.tree.NodeEffect.LayerExtendEffect} の amountToAdd 合計を返す (SoulTree →
   * GameContext.maxLayer 反映用ヘルパ、Task B)。
   */
  private int totalLayerExtendAmount() {
    int total = 0;
    for (NodeId id : progress.soulTree().unlockedNodes()) {
      core.domain.tree.TreeNode node = SoulTree.allNodes().get(id);
      if (node == null) {
        continue;
      }
      if (node.effect() instanceof core.domain.tree.NodeEffect.LayerExtendEffect le) {
        total += le.amountToAdd();
      }
    }
    return total;
  }

  /**
   * 層末ノードを 1 つ選択 → 効果適用 → 次層へ進む (§15-8 / E-6)。
   *
   * <p>処理:
   *
   * <ol>
   *   <li>{@link LayerEndNode#apply(Player, core.domain.layer.NodeResolveContext)} で Player を強化
   *       (純関数)
   *   <li>強化済の Player を含む新 state を {@link InitialStateFactory#advanceLayer} に渡し、次層 state を生成
   *   <li>{@link TurnDirector#advanceFloor} に委譲し、AP リフィル + 1 枚ドロー + イベント発火
   * </ol>
   *
   * <p>CLEARED 以外の状態で呼ばれた場合、効果は player に適用されるが {@link TurnDirector#advanceFloor} の no-op
   * ガードで層遷移は起きない (二重ガード)。 ただし通常は DungeonScreen 側で CLEARED 時にのみ呼ばれるよう制御する。
   */
  public void resolveLayerEndChoice(LayerEndNode choice) {
    Objects.requireNonNull(choice, "choice");
    RunSession session = requireRunSession();
    DungeonState current = session.context().state();
    Player before = current.player();
    // Wave 3 Task A: Shop は CardId 保持なので cards / equipments resolver を context 経由で渡す
    Player upgraded = choice.apply(before, nodeResolveContext());
    // Wave 3 Task B: ShopEquipment は apply 内で Gold 消費のみ行う純関数。Gold が減っていれば
    // 購入成功 = ロードアウトに装着する (装着は次ラン反映、ADR-25 / ADR-26)。
    if (choice instanceof LayerEndNode.ShopEquipment se
        && before.gold().amount() != upgraded.gold().amount()) {
      Equipment eq = equipmentCatalog().get(se.equipmentId());
      equipInLoadout(eq);
    }
    DungeonState withUpgrade = current.withPlayer(upgraded);
    // GameContext.maxLayer を渡し、SoulTree.LayerExtendEffect で拡張済の最終層番号を反映
    session
        .director()
        .advanceFloor(
            InitialStateFactory.advanceLayer(
                withUpgrade, session.rng(), session.context().maxLayer()));
    recordObtainedCards(); // §15-3: ショップノードで入手したカードを図鑑に記録
    // §15-11: 層境界 (次層に進入する前) でセーブする。
    saveAtLayerBoundary();
  }

  /**
   * §15-3 / §15-6: 強化個体撃破時にカード追加効果を Player に適用する (層遷移は伴わない)。
   *
   * <p>{@link LayerEndNode#apply(Player, core.domain.layer.NodeResolveContext)} を呼ぶだけで、{@link
   * core.application.TurnDirector#advanceFloor} は呼ばない (戦闘継続中のため)。
   */
  public void applyEliteCardReward(LayerEndNode choice) {
    Objects.requireNonNull(choice, "choice");
    RunSession session = requireRunSession();
    DungeonState current = session.context().state();
    Player upgraded = choice.apply(current.player(), nodeResolveContext());
    session
        .context()
        .applyResult(new StepResult(current.withPlayer(upgraded), java.util.List.of()));
    recordObtainedCards(); // §15-3: 強化個体報酬で入手したカードを図鑑に記録
  }

  // =========================================================================
  // §15-11 セーブ / ロード
  // =========================================================================

  /** セーブマネージャを返す (TitleScreen のセーブ存在判定に使う)。 */
  public SaveManager saveManager() {
    return saveManager;
  }

  /** 設定マネージャを返す (初回起動判定などに使う)。 */
  public SettingsManager settingsManager() {
    return settingsManager;
  }

  /** 現在の設定を返す (§15-1)。 */
  public Settings settings() {
    return settings;
  }

  /**
   * 設定を更新してフルスクリーンを即時反映する (§15-1)。
   *
   * <p>保存は {@link #saveSettings()} を別途呼ぶこと (SettingsScreen が ESC 時に呼ぶ)。
   *
   * @param newSettings 新しい設定
   */
  public void applySettings(Settings newSettings) {
    java.util.Objects.requireNonNull(newSettings, "newSettings");
    this.settings = newSettings;
    // フルスクリーン切替を実機反映
    if (newSettings.fullscreen()) {
      com.badlogic.gdx.Graphics.DisplayMode mode = com.badlogic.gdx.Gdx.graphics.getDisplayMode();
      com.badlogic.gdx.Gdx.graphics.setFullscreenMode(mode);
    } else {
      // DesktopLauncher と同じ 1920×1080 に戻す (1280×720 ハードコードだと解像度が縮む)。
      com.badlogic.gdx.Gdx.graphics.setWindowedMode(1920, 1080);
    }
    // §15-5: サウンドマネージャに音量を即時反映する。
    if (resources != null && resources.soundManager() != null) {
      resources.soundManager().applySettings(newSettings);
    }
  }

  /**
   * 現在の設定をファイルに保存する (§15-1)。
   *
   * <p>SettingsScreen の ESC 、初回プリセット選択後に呼ぶ。
   */
  public void saveSettings() {
    settingsManager.save(settings);
  }

  /**
   * 層境界 (層末ノード解決直後) に呼ぶ。現在の live 状態を {@link SaveData} に変換して保存する。
   *
   * <p>context が null (ラン未開始) の場合は何もしない (ガード)。
   */
  public void saveAtLayerBoundary() {
    if (runSession.isEmpty()) {
      return;
    }
    DungeonState state = runSession.get().context().state();
    // ロード後に進入する層番号 = 現在の次層番号 (advanceFloor 済みなので state.layer() が次層)
    int nextLayerNumber = state.layer().number();
    SaveData data =
        SaveDataConverter.toSaveData(
            state.player(),
            nextLayerNumber,
            progress.playerSoul().amount(),
            progress.runCount(),
            progress.soulTree(),
            progress.obtainedCards(),
            progress.loadout(),
            progress.bestiary(),
            progress.tutorialSeen());
    saveManager.save(data);
  }

  /**
   * セーブデータからランを復元する (§15-11)。
   *
   * <p>ロード後は InitialStateFactory で指定層番号のマップを新規生成し、プレイヤーのステ / デッキ / メタ進捗を復元する。 セーブデータが存在しない /
   * 破損している場合は何もしない。
   *
   * @return ロードに成功した場合 true
   */
  public boolean loadFromSave() {
    Optional<SaveData> optData = saveManager.load();
    if (optData.isEmpty()) {
      return false;
    }
    SaveData data = optData.get();

    // メタ進捗を一括復元 (Wave 6 W6-γ: PlayerProgress 1 record で集約)
    // playerSoul はラン状態の Player にこの直後注入するため一旦 zero。SaveData の soulTotal は
    // savedSoul として下方の Player.addSoul で注入される。
    progress =
        new PlayerProgress(
            Soul.zero(),
            data.runCount(),
            data.tutorialSeen(),
            SaveDataConverter.toObtainedCards(data),
            SaveDataConverter.toBestiary(data),
            SaveDataConverter.toLoadout(data),
            SaveDataConverter.toSoulTree(data));

    // ラン状態を復元: 指定層からの新規マップ生成 + プレイヤーステ / デッキ注入
    Random newRng = new Random();
    // §15-6 / SoulTree LayerExtend: ロード再開でも最大層数を再計算 (SaveData の SoulTree から派生)。
    int extendAmount = totalLayerExtendAmount();
    int maxLayer = InitialStateFactory.DEFAULT_MAX_LAYER + extendAmount;
    DungeonState baseState =
        InitialStateFactory.restoreLayer(data, progress.loadout(), newRng, maxLayer);
    // §15-7 CRITICAL FIX: ロード時にソウルツリー効果を再適用しない (SaveData は補正済 Stats/Deck/SkillSlot を
    // 保存しているため、再適用すると HP / カード / スキル枠が二重加算される)。
    Player withTree = baseState.player();
    // ランに保持するソウルは SaveData.soulTotal から復元 (playerSoul → Player に注入)
    Soul savedSoul = new Soul(data.soulTotal());
    Player withSoul = withTree.addSoul(savedSoul);
    // playerSoul はもう Soul.zero() (前段の new PlayerProgress で初期化済)、注入後の保持は維持

    DungeonState restoredState = baseState.withPlayer(withSoul);
    GameContext newContext = GameContext.startNewRun(restoredState);
    if (extendAmount > 0) {
      newContext.extendMaxLayer(extendAmount);
    }
    TurnDirector newDirector = new TurnDirector(newContext, newRng);
    runSession = Optional.of(new RunSession(newContext, newDirector, newRng));
    // セーブデータは引き継がず: ロードして再開したら次層進入時に上書きセーブされる
    return true;
  }

  /**
   * 画面を切り替え、旧 Screen を dispose する。
   *
   * <p>LibGDX の {@link Game#setScreen} は旧 Screen を dispose しないためリークする。本ヘルパー経由で 全ての画面遷移を行うことで、旧
   * Screen のリソース解放を保証する。
   */
  public void changeScreen(Screen next) {
    Screen old = getScreen();
    setScreen(next);
    if (old != null) {
      old.dispose();
    }
  }

  @Override
  public void create() {
    // §設計原則 / Wave 5 W5-γ: SoulTree のノードマスタ Supplier を最初に注入
    // (domain → infrastructure 依存方向違反を解消、Logger 級の単発 init setter)。
    SoulTree.setNodeProvider(core.infrastructure.bootstrap.InitialStateFactory::soulTreeNodes);
    // §15-1: 設定をロード (ファイルなし時は DEFAULT、resources 初期化前に確定)
    this.settings = settingsManager.load();
    // Wave 8 W8-β: LibGDX リソース 3 件 (Fonts / SoundManager / CardImageRegistry) を GameResources
    // で一括初期化
    this.resources =
        GameResources.load(
            settings,
            core.infrastructure.bootstrap.InitialStateFactory.cardCatalog(),
            core.infrastructure.bootstrap.InitialStateFactory.equipmentCatalog());
    // §15-7 / E-2: startNewRun() はラン開始の瞬間 (TitleScreen の ENTER) でのみ呼ぶ。
    // ここで呼ぶと獲得前の playerSoul が Player に注入・ゼロ化され、ソウルツリーで使えなく
    // なる (ソウル消失バグの根治)。context / director は最初のラン開始まで null。
    // §15-1: 初回起動 (settings.json 未存在) は UI プリセット選択画面を最初に表示する。
    if (!settingsManager.exists()) {
      changeScreen(new FirstRunPresetScreen(this));
    } else {
      changeScreen(new TitleScreen(this));
    }
  }

  @Override
  public void dispose() {
    super.dispose();
    // Wave 8 W8-β: GameResources で防衛的 dispose (try-catch + null check 内包)
    if (resources != null) {
      resources.dispose();
    }
  }
}
