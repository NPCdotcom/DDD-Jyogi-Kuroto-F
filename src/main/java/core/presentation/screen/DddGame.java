package core.presentation.screen;

import com.badlogic.gdx.Game;
import core.application.GameContext;
import core.application.TurnDirector;
import core.domain.dungeon.DungeonState;
import core.domain.entity.Player;
import core.domain.layer.LayerEndNode;
import core.domain.meta.Soul;
import core.domain.tree.NodeId;
import core.domain.tree.SoulTree;
import core.infrastructure.bootstrap.InitialStateFactory;
import core.presentation.render.Fonts;
import java.util.Objects;
import java.util.Random;

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

  /**
   * ソウルツリー (§15-7 / E-2)。ラン跨ぎで持続する永続強化状態。E-9 セーブ未実装のため
   * JVM 終了でリセット。タイトル画面のソウルツリー画面で操作する。
   */
  private SoulTree soulTree = SoulTree.empty();

  /**
   * ラン外のプレイヤー所持ソウル (§15-2 / §15-7)。ラン終了時に Player.soul から書き戻され、
   * 次ラン開始時に新 Player.soul として注入される。タイトル画面のソウルツリー解放でも消費する。
   * E-9 セーブ未実装のため JVM 終了でリセット (デフォルト 0)。
   */
  private Soul playerSoul = Soul.zero();

  public GameContext context() {
    return context;
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
  }

  /**
   * 階段踏破後 (CLEARED 状態) に次の層へ進む (§15-6 / ADR-23)。
   *
   * <p>infrastructure 層の {@link InitialStateFactory#advanceLayer} を呼んで次層 state を生成し、application 層の
   * {@link TurnDirector#advanceFloor} に委譲する。application が infrastructure を直接 import しないための依存逆転を本クラス
   * (presentation 層、両方を import 可能) が担当する。
   */
  public void advanceFloor() {
    director.advanceFloor(InitialStateFactory.advanceLayer(context.state()));
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
   * <p>CLEARED 以外の状態で呼ばれた場合、効果は player に適用されるが {@link TurnDirector#advanceFloor} の no-op ガードで層遷移は起きない
   * (二重ガード)。 ただし通常は DungeonScreen 側で CLEARED 時にのみ呼ばれるよう制御する。
   */
  public void resolveLayerEndChoice(LayerEndNode choice) {
    Objects.requireNonNull(choice, "choice");
    DungeonState current = context.state();
    Player upgraded = choice.apply(current.player());
    DungeonState withUpgrade = current.withPlayer(upgraded);
    director.advanceFloor(InitialStateFactory.advanceLayer(withUpgrade));
  }

  @Override
  public void create() {
    fonts = new Fonts();
    startNewRun();
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
