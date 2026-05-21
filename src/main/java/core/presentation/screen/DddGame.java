package core.presentation.screen;

import com.badlogic.gdx.Game;
import core.application.GameContext;
import core.application.TurnDirector;
import core.domain.card.Card;
import core.domain.card.CardId;
import core.domain.card.CardPileState;
import core.domain.dungeon.DungeonState;
import core.domain.entity.Player;
import core.domain.layer.LayerEndNode;
import core.domain.meta.Soul;
import core.domain.tree.NodeId;
import core.domain.tree.SoulTree;
import core.infrastructure.bootstrap.InitialStateFactory;
import core.presentation.render.Fonts;
import java.util.HashSet;
import java.util.Objects;
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

  /** ソウルツリー (§15-7 / E-2)。ラン跨ぎで持続する永続強化状態。E-9 セーブ未実装のため JVM 終了でリセット。タイトル画面のソウルツリー画面で操作する。 */
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
   * (1 周目はツリー非表示、1 周目終了時に 初公開)。E-9 セーブ未実装のため JVM 終了でリセット。
   */
  private int runCount = 0;

  /**
   * これまでに入手したカードの ID 集合 (§15-3 カード図鑑)。カード図鑑の解放判定に使う (入手済 = 解放)。 初期デッキ / 強化個体報酬 /
   * 層末ショップで入手するたびに記録する。E-9 セーブ未実装のため JVM 終了でリセット。
   */
  private final Set<CardId> obtainedCards = new HashSet<>();

  public GameContext context() {
    return context;
  }

  /** これまでに入手したカード ID の集合 (カード図鑑の解放判定用、防御コピー)。 */
  public Set<CardId> obtainedCards() {
    return Set.copyOf(obtainedCards);
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

  /** 新しいラン (= ダンジョン挑戦) を開始する。context と director を作り直す。 */
  public void startNewRun() {
    // ADR-19: Random は引数注入で再現性を呼出元に委ねる (初期手札シャッフル + 毎ターンドロー)。
    // ラン毎に new Random() で異なるシード = 本番プレイは非再現的 (テストでは固定シードを渡す)。
    DungeonState state = InitialStateFactory.firstFloor(new Random());
    // §15-7: ソウルツリーの解放済み効果を Player に適用 (素ステ補正 / カード追加 / 枠拡張)
    Player applied = soulTree.applyTo(state.player(), InitialStateFactory::resolveCard);
    // §15-2 / §15-7: ラン外のソウル保持を Player に注入 (前回ランからの持ち越し)
    Player withSoul = applied.addSoul(playerSoul);
    // 注入後は外部保持を 0 に (重複加算防止、preserveSoulFromRun でラン終了時に書き戻る)
    this.playerSoul = Soul.zero();
    this.context = GameContext.startNewRun(state.withPlayer(withSoul));
    this.director = new TurnDirector(this.context, new Random());
    recordObtainedCards(); // §15-3: 初期デッキを図鑑に記録
  }

  /**
   * 層末ノードを 1 つ選択 → 効果適用 → 次層へ進む (§15-8 / E-6)。
   *
   * <p>処理:
   *
   * <ol>
   *   <li>{@link LayerEndNode#apply(Player)} で Player を強化 (純関数)
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
    Player upgraded = choice.apply(current.player());
    DungeonState withUpgrade = current.withPlayer(upgraded);
    director.advanceFloor(InitialStateFactory.advanceLayer(withUpgrade, new Random()));
    recordObtainedCards(); // §15-3: ショップノードで入手したカードを図鑑に記録
  }

  /**
   * §15-3 / §15-6: 強化個体撃破時にカード追加効果を Player に適用する (層遷移は伴わない)。
   *
   * <p>{@link LayerEndNode#apply(Player)} を呼ぶだけで、{@link core.application.TurnDirector#advanceFloor}
   * は呼ばない (戦闘継続中のため)。
   */
  public void applyEliteCardReward(LayerEndNode choice) {
    Objects.requireNonNull(choice, "choice");
    DungeonState current = context.state();
    Player upgraded = choice.apply(current.player());
    context.applyResult(
        new core.domain.battle.TurnEngine.StepResult(
            current.withPlayer(upgraded), java.util.List.of()));
    recordObtainedCards(); // §15-3: 強化個体報酬で入手したカードを図鑑に記録
  }

  @Override
  public void create() {
    fonts = new Fonts();
    // §15-7 / E-2: startNewRun() はラン開始の瞬間 (TitleScreen の ENTER) でのみ呼ぶ。
    // ここで呼ぶと獲得前の playerSoul が Player に注入・ゼロ化され、ソウルツリーで使えなく
    // なる (ソウル消失バグの根治)。context / director は最初のラン開始まで null。
    setScreen(new TitleScreen(this));
  }

  @Override
  public void dispose() {
    super.dispose();
    if (fonts != null) {
      fonts.dispose();
    }
  }
}
