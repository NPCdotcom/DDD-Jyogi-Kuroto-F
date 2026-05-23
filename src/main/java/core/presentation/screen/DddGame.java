package core.presentation.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import core.application.GameContext;
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
import core.domain.meta.Bestiary;
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

  private GameContext context;
  private TurnDirector director;
  private Fonts fonts;
  private SoundManager soundManager;

  /** カード画像レジストリ (§15-3、カード ID → Texture、未マッピング/欠損は test.png fallback)。 */
  private CardImageRegistry cardImageRegistry;

  /**
   * ランごとの乱数源 (ADR-19)。{@link #startNewRun()} で新しいシードに切り替え、{@link DungeonScreen} へ注入する。 同一シードを渡すことで
   * テストでの再現が可能。本番プレイは非再現的。
   */
  private Random runRng;

  /** ソウルツリー (§15-7 / E-2)。ラン跨ぎで持続する永続強化状態。セーブは §15-11 で実装済み、層境界ごとにセーブされる。タイトル画面のソウルツリー画面で操作する。 */
  private SoulTree soulTree = SoulTree.empty();

  /**
   * ラン外のプレイヤー所持ソウル (§15-2 / §15-7)。ラン終了時に Player.soul から書き戻され、 次ラン開始時に新 Player.soul
   * として注入される。タイトル画面のソウルツリー解放でも消費する。 E-9 セーブ未実装のため JVM 終了でリセット (デフォルト 0)。
   */
  private Soul playerSoul = Soul.zero();

  /**
   * チュートリアル既読フラグ (§15-10 / E-10)。初回起動時のタイトル画面で TutorialOverlay を 1 回表示し、 閉じたら true 化する。E-9
   * セーブ未実装のため JVM 終了でリセット (起動ごとに 1 回表示で許容)。
   */
  private boolean tutorialSeen = false;

  /**
   * 周回数 (= 完了したラン数、§15-7 / E-2)。{@link #onRunEnded()} で +1 される。1 以上で タイトル画面のソウルツリー動線 (T キー) を解禁する
   * (1 周目はツリー非表示、1 周目終了時に 初公開)。
   */
  private int runCount = 0;

  /** セーブ / ロードを担当する (§15-11)。 */
  private final SaveManager saveManager = new SaveManager();

  /** 設定の永続化を担当する (§15-1)。 */
  private final SettingsManager settingsManager = new SettingsManager();

  /** 現在の設定 (§15-1)。起動時にロードし、SettingsScreen で更新する。 */
  private Settings settings = Settings.DEFAULT;

  /**
   * これまでに入手したカードの ID 集合 (§15-3 カード図鑑)。カード図鑑の解放判定に使う (入手済 = 解放)。 初期デッキ / 強化個体報酬 /
   * 層末ショップで入手するたびに記録する。E-9 セーブ未実装のため JVM 終了でリセット。
   */
  private final Set<CardId> obtainedCards = new HashSet<>();

  /**
   * 撃破済み敵種の Bestiary (§15-5 / E-7、ラン内記憶)。次行動予告 UI は M2 送りだが、撃破記録は本セッションで通す。 SaveData への永続化は未対応 (M2
   * 申し送り)。
   */
  private Bestiary bestiary = Bestiary.empty();

  /**
   * 装備ロードアウト (§15-9)。装備スロット → 装備。EquipmentScreen で編集し、{@link #startNewRun()} で Player に
   * 反映する。デフォルトはぼろい短剣のみ。E-9 セーブ未実装のため JVM 終了でリセット。
   */
  private final Map<EquipmentSlot, Equipment> loadout = defaultLoadout();

  private static Map<EquipmentSlot, Equipment> defaultLoadout() {
    Equipment dagger = InitialStateFactory.tatteredDagger();
    Map<EquipmentSlot, Equipment> m = new HashMap<>();
    m.put(dagger.slot(), dagger);
    return m;
  }

  public GameContext context() {
    return context;
  }

  /** 現在の装備ロードアウト (装備画面表示用、防御コピー)。 */
  public Map<EquipmentSlot, Equipment> loadout() {
    return Map.copyOf(loadout);
  }

  /**
   * 現在のロードアウトから UI テーマを動的に決定する (§7-2 / W4-ε)。
   *
   * <p>主武器優先で {@code themeName} が設定されている装備を探し、{@link UiThemeResolver} で解決する。 全装備が {@code
   * themeName=empty} の場合は {@link UiTheme#defaultTheme()} にフォールバック。
   */
  public UiTheme activeUiTheme() {
    return UiThemeResolver.resolve(loadout);
  }

  /** 装備をそのスロットに装着する (同スロットの既存装備は置き換え、§15-9、次ラン開始時に反映)。 */
  public void equipInLoadout(Equipment equipment) {
    Objects.requireNonNull(equipment, "equipment");
    loadout.put(equipment.slot(), equipment);
  }

  /**
   * 指定スロットの装備を外す (§15-9、次ラン開始時に反映)。
   *
   * <p>ロードアウトを空にはできない (最後の 1 個は外せない)。空ロードアウト → 空デッキでラン開始すると 攻撃手段ゼロで詰むため。
   */
  public void unequipSlot(EquipmentSlot slot) {
    Objects.requireNonNull(slot, "slot");
    if (loadout.size() <= 1) {
      return; // 最後の装備は外さない (空デッキ防止)
    }
    loadout.remove(slot);
  }

  /** これまでに入手したカード ID の集合 (カード図鑑の解放判定用、防御コピー)。 */
  public Set<CardId> obtainedCards() {
    return Set.copyOf(obtainedCards);
  }

  /** 撃破済敵種の Bestiary (§15-5、不変 record、防御コピー不要)。 */
  public Bestiary bestiary() {
    return bestiary;
  }

  /**
   * 敵を撃破したことを Bestiary に記録する (§15-5 / E-7、DungeonScreen の ActorDied ハンドラから呼ぶ)。
   *
   * @param kind 撃破した敵の種別
   */
  public void recordEnemyDefeated(EnemyKind kind) {
    this.bestiary = bestiary.withDefeated(kind);
  }

  /** 現在の Player が保持する全カード (山札 + 手札 + 捨て札) の ID を入手済として記録する。 */
  private void recordObtainedCards() {
    if (context == null) {
      return;
    }
    CardPileState piles = context.state().player().cardPileState();
    for (Card c : piles.drawPile().cards()) {
      obtainedCards.add(c.id());
    }
    for (Card c : piles.hand().cards()) {
      obtainedCards.add(c.id());
    }
    for (Card c : piles.discardPile().cards()) {
      obtainedCards.add(c.id());
    }
  }

  public TurnDirector director() {
    return director;
  }

  public Fonts fonts() {
    return fonts;
  }

  /** サウンドマネージャ (BGM / SE 再生の窓口)。 */
  public SoundManager soundManager() {
    return soundManager;
  }

  /** カード画像レジストリ (CardCollectionScreen / HudRenderer から参照)。 */
  public CardImageRegistry cardImageRegistry() {
    return cardImageRegistry;
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

  public SoulTree soulTree() {
    return soulTree;
  }

  public Soul playerSoul() {
    return playerSoul;
  }

  public boolean isTutorialSeen() {
    return tutorialSeen;
  }

  /** §15-10 / E-10: チュートリアル overlay を閉じた時に呼び、次回以降の自動表示を抑制する。 */
  public void markTutorialSeen() {
    this.tutorialSeen = true;
  }

  /** 完了したラン数 (§15-7 / E-2: 1 以上でソウルツリー動線を解禁)。 */
  public int runCount() {
    return runCount;
  }

  /** ラン外のソウルツリー画面でノード解放した結果を受け取る (§15-7)。 */
  public void unlockTreeNode(NodeId nodeId) {
    SoulTree.UnlockResult result = soulTree.unlock(nodeId, playerSoul);
    this.soulTree = result.newTree();
    this.playerSoul = result.newSoul();
  }

  /** ラン外のソウルツリー画面でリセットボタン押下時に呼ぶ (ADR-09)。 */
  public void resetTree() {
    SoulTree.ResetResult result = soulTree.reset();
    this.soulTree = result.newTree();
    this.playerSoul = playerSoul.add(result.refundedSoul());
  }

  /** ラン終了時 (GameOverScreen 表示時) に Player.soul をラン外保持に書き戻す (§15-7)。 */
  public void preserveSoulFromRun() {
    if (context != null) {
      this.playerSoul = context.state().player().soul();
    }
  }

  /**
   * ラン終了時 (GameOverScreen 表示時) に呼ぶ (§15-7 / E-2)。{@link #preserveSoulFromRun()} で
   * 獲得ソウルをラン外保持に退避し、{@link #runCount} を 1 増やす。runCount が 1 以上になることで 以降ソウルツリー動線 (タイトルの T キー) が解禁される。
   */
  public void onRunEnded() {
    preserveSoulFromRun();
    runCount++;
  }

  /** 現在の {@link Random} (ダンジョン生成・ターン進行用、{@link DungeonScreen} への注入に使う)。 */
  public Random runRng() {
    return runRng;
  }

  /** 新しいラン (= ダンジョン挑戦) を開始する。context と director を作り直す。 */
  public void startNewRun() {
    // ADR-19: Random は引数注入で再現性を呼出元に委ねる (初期手札シャッフル + 毎ターンドロー)。
    // ラン毎に new Random() で異なるシード = 本番プレイは非再現的 (テストでは固定シードを渡す)。
    runRng = new Random();
    // §15-6 / SoulTree LayerExtend: 解放済みの LayerExtendEffect の合計値からラン最大層数を算出。
    // generateLayerState がボス配置のために最大層数を必要とするため、firstFloor 前に計算する。
    int extendAmount = totalLayerExtendAmount();
    int maxLayer = InitialStateFactory.DEFAULT_MAX_LAYER + extendAmount;
    DungeonState state = InitialStateFactory.firstFloor(runRng, loadout, maxLayer);
    // §15-7: ソウルツリーの解放済み効果を Player に適用 (素ステ補正 / カード追加 / 枠拡張)
    // LayerExtendEffect は Player に副作用がないため、ループは no-op (副作用は GameContext.maxLayer 側で集約済)。
    Player applied = soulTree.applyTo(state.player(), InitialStateFactory::resolveCard);
    // §15-2 / §15-7: ラン外のソウル保持を Player に注入 (前回ランからの持ち越し)
    Player withSoul = applied.addSoul(playerSoul);
    // 注入後は外部保持を 0 に (重複加算防止、preserveSoulFromRun でラン終了時に書き戻る)
    this.playerSoul = Soul.zero();
    this.context = GameContext.startNewRun(state.withPlayer(withSoul));
    if (extendAmount > 0) {
      this.context.extendMaxLayer(extendAmount);
    }
    this.director = new TurnDirector(this.context, runRng);
    recordObtainedCards(); // §15-3: 初期デッキを図鑑に記録
  }

  /**
   * 解放済みノードの {@link core.domain.tree.NodeEffect.LayerExtendEffect} の amountToAdd 合計を返す (SoulTree →
   * GameContext.maxLayer 反映用ヘルパ、Task B)。
   */
  private int totalLayerExtendAmount() {
    int total = 0;
    for (NodeId id : soulTree.unlockedNodes()) {
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
    DungeonState current = context.state();
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
    director.advanceFloor(
        InitialStateFactory.advanceLayer(withUpgrade, runRng, context.maxLayer()));
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
    DungeonState current = context.state();
    Player upgraded = choice.apply(current.player(), nodeResolveContext());
    context.applyResult(new StepResult(current.withPlayer(upgraded), java.util.List.of()));
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
    if (soundManager != null) {
      soundManager.applySettings(newSettings);
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
    if (context == null) {
      return;
    }
    DungeonState state = context.state();
    // ロード後に進入する層番号 = 現在の次層番号 (advanceFloor 済みなので state.layer() が次層)
    int nextLayerNumber = state.layer().number();
    SaveData data =
        SaveDataConverter.toSaveData(
            state.player(),
            nextLayerNumber,
            playerSoul.amount(),
            runCount,
            soulTree,
            obtainedCards,
            loadout,
            bestiary,
            tutorialSeen);
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

    // メタ進捗を復元
    this.runCount = data.runCount();
    this.soulTree = SaveDataConverter.toSoulTree(data);
    this.loadout.clear();
    this.loadout.putAll(SaveDataConverter.toLoadout(data));
    this.obtainedCards.clear();
    this.obtainedCards.addAll(SaveDataConverter.toObtainedCards(data));
    // Wave 6 W6-β: bestiary + tutorialSeen を復元 (v1 セーブは graceful に空 / false)
    this.bestiary = SaveDataConverter.toBestiary(data);
    this.tutorialSeen = data.tutorialSeen();

    // ラン状態を復元: 指定層からの新規マップ生成 + プレイヤーステ / デッキ注入
    runRng = new Random();
    // §15-6 / SoulTree LayerExtend: ロード再開でも最大層数を再計算 (SaveData の SoulTree から派生)。
    int extendAmount = totalLayerExtendAmount();
    int maxLayer = InitialStateFactory.DEFAULT_MAX_LAYER + extendAmount;
    DungeonState baseState = InitialStateFactory.restoreLayer(data, loadout, runRng, maxLayer);
    // §15-7 CRITICAL FIX: ロード時にソウルツリー効果を再適用しない (SaveData は補正済 Stats/Deck/SkillSlot を
    // 保存しているため、再適用すると HP / カード / スキル枠が二重加算される)。
    Player withTree = baseState.player();
    // ランに保持するソウルは SaveData.soulTotal から復元 (playerSoul → Player に注入)
    Soul savedSoul = new Soul(data.soulTotal());
    Player withSoul = withTree.addSoul(savedSoul);
    this.playerSoul = Soul.zero(); // 注入済みなのでラン外保持は 0

    DungeonState restoredState = baseState.withPlayer(withSoul);
    this.context = GameContext.startNewRun(restoredState);
    if (extendAmount > 0) {
      this.context.extendMaxLayer(extendAmount);
    }
    this.director = new TurnDirector(this.context, runRng);
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
    fonts =
        new Fonts(
            core.infrastructure.bootstrap.InitialStateFactory.cardCatalog(),
            core.infrastructure.bootstrap.InitialStateFactory.equipmentCatalog());
    // §15-1: 設定をロード (ファイルなし時は DEFAULT)
    this.settings = settingsManager.load();
    // §15-5: サウンドマネージャを初期化 (ファイル欠損時は no-op で継続)
    soundManager = new SoundManager(settings);
    // §15-3: カード画像レジストリ (起動時に全 PNG をロード、欠損は test.png fallback)
    cardImageRegistry = CardImageRegistry.load();
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
    if (fonts != null) {
      fonts.dispose();
    }
    if (soundManager != null) {
      soundManager.dispose();
    }
    if (cardImageRegistry != null) {
      cardImageRegistry.dispose();
    }
  }
}
