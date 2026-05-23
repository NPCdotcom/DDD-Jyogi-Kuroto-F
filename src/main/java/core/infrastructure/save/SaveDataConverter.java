package core.infrastructure.save;

import core.domain.card.Card;
import core.domain.card.CardId;
import core.domain.card.CardPileState;
import core.domain.card.DiscardPile;
import core.domain.card.DrawPile;
import core.domain.card.Hand;
import core.domain.entity.EnemyKind;
import core.domain.entity.Player;
import core.domain.entity.Stats;
import core.domain.equipment.Equipment;
import core.domain.equipment.EquipmentId;
import core.domain.equipment.EquipmentSlot;
import core.domain.meta.Bestiary;
import core.domain.tree.NodeId;
import core.domain.tree.SoulTree;
import core.infrastructure.bootstrap.InitialStateFactory;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * live 状態 ({@link core.presentation.screen.DddGame} のフィールド群) と {@link SaveData} の双方向変換を担う。
 *
 * <p>ドメインオブジェクトは immutable record なので変換は純関数として実装できる。 変換に失敗する可能性がある箇所 (カタログ未登録の ID 等) は graceful
 * に扱い、 ログを出してデフォルト値にフォールバックする (Fonts の欠損フォールバックと同じ思想)。
 */
public final class SaveDataConverter {

  private static final Logger LOG = Logger.getLogger(SaveDataConverter.class.getName());

  private SaveDataConverter() {}

  // =========================================================================
  // live → SaveData (セーブ)
  // =========================================================================

  /**
   * DddGame の live 状態を {@link SaveData} に変換する (Wave 6 W6-β で bestiary / tutorialSeen を追加)。
   *
   * @param player 現在のプレイヤー (ラン中の状態)
   * @param nextLayerNumber ロード後に進入する層番号
   * @param soulTotal ラン外ソウル合計
   * @param runCount 完了ラン数
   * @param soulTree ソウルツリー
   * @param obtainedCards カード図鑑
   * @param loadout 装備ロードアウト
   * @param bestiary 撃破済敵種 (§15-5、Wave 6 W6-β で永続化対象)
   * @param tutorialSeen チュートリアル既読フラグ (§15-10、Wave 6 W6-β で永続化対象)
   * @return 保存用 SaveData
   */
  public static SaveData toSaveData(
      Player player,
      int nextLayerNumber,
      int soulTotal,
      int runCount,
      SoulTree soulTree,
      Set<CardId> obtainedCards,
      Map<EquipmentSlot, Equipment> loadout,
      Bestiary bestiary,
      boolean tutorialSeen) {

    Stats stats = player.stats();

    // デッキ全カード (山札 + 手札 + 捨て札) を ID 文字列に変換
    List<String> deck = new ArrayList<>();
    for (Card c : player.cardPileState().drawPile().cards()) {
      deck.add(c.id().value());
    }
    for (Card c : player.cardPileState().hand().cards()) {
      deck.add(c.id().value());
    }
    for (Card c : player.cardPileState().discardPile().cards()) {
      deck.add(c.id().value());
    }

    // 解放済みノード ID (root は常に存在するが保存しておく)
    List<String> unlockedNodeIds = new ArrayList<>();
    for (NodeId nodeId : soulTree.unlockedNodes()) {
      unlockedNodeIds.add(nodeId.value());
    }

    // 入手済みカード ID
    List<String> obtainedCardIds = new ArrayList<>();
    for (CardId cardId : obtainedCards) {
      obtainedCardIds.add(cardId.value());
    }

    // 装備ロードアウト (EquipmentSlot.name() → EquipmentId.value())
    Map<String, String> loadoutMap = new HashMap<>();
    for (Map.Entry<EquipmentSlot, Equipment> entry : loadout.entrySet()) {
      loadoutMap.put(entry.getKey().name(), entry.getValue().id().value());
    }

    // Wave 6 W6-β: Bestiary を EnemyKind.name() の List<String> に直列化
    // (unlockedNodeIds / obtainedCardIds と同型のパターン)
    List<String> defeatedEnemyKinds = new ArrayList<>();
    for (EnemyKind kind : bestiary.defeatedKinds()) {
      defeatedEnemyKinds.add(kind.name());
    }

    return new SaveData(
        SaveData.CURRENT_SCHEMA_VERSION,
        nextLayerNumber,
        stats.currentHp(),
        stats.maxHp(),
        stats.speed(),
        stats.physicalAttack(),
        stats.magicalAttack(),
        stats.physicalDefense(),
        stats.magicalDefense(),
        deck,
        soulTotal,
        runCount,
        unlockedNodeIds,
        obtainedCardIds,
        loadoutMap,
        defeatedEnemyKinds,
        tutorialSeen);
  }

  // =========================================================================
  // SaveData → live 状態 (ロード)
  // =========================================================================

  /** {@link SaveData} からソウルツリーを復元する。未定義の NodeId は警告ログを出してスキップする (graceful)。 */
  public static SoulTree toSoulTree(SaveData data) {
    Set<NodeId> unlocked = new HashSet<>();
    unlocked.add(SoulTree.ROOT); // root は必ず含む
    for (String id : data.unlockedNodeIds()) {
      try {
        NodeId nodeId = NodeId.of(id);
        if (SoulTree.allNodes().containsKey(nodeId)) {
          unlocked.add(nodeId);
        } else {
          LOG.warning("Unknown NodeId in save data, skipping: " + id);
        }
      } catch (IllegalArgumentException e) {
        LOG.warning("Invalid NodeId in save data, skipping: " + id + " - " + e.getMessage());
      }
    }
    // totalSpentSoul は再計算 (保存値は不要、allNodes から算出できる)
    int spent = 0;
    for (NodeId nodeId : unlocked) {
      if (!nodeId.equals(SoulTree.ROOT) && SoulTree.allNodes().containsKey(nodeId)) {
        spent += SoulTree.allNodes().get(nodeId).soulCost();
      }
    }
    return new SoulTree(unlocked, spent);
  }

  /** {@link SaveData} から装備ロードアウトを復元する。未定義の装備 ID は警告ログを出してスキップし、 ロードアウトが空になる場合はデフォルト (ぼろい短剣) を使う。 */
  public static Map<EquipmentSlot, Equipment> toLoadout(SaveData data) {
    Map<EquipmentSlot, Equipment> result = new HashMap<>();
    for (Map.Entry<String, String> entry : data.loadout().entrySet()) {
      try {
        EquipmentSlot slot = EquipmentSlot.valueOf(entry.getKey());
        Equipment equipment =
            InitialStateFactory.equipmentCatalog().get(EquipmentId.of(entry.getValue()));
        result.put(slot, equipment);
      } catch (IllegalArgumentException e) {
        LOG.warning(
            "Unknown slot or equipment id in save data, skipping: "
                + entry
                + " - "
                + e.getMessage());
      }
    }
    // ロードアウトが空 → デフォルトに戻す (空デッキ防止)
    if (result.isEmpty()) {
      Equipment dagger = InitialStateFactory.tatteredDagger();
      result.put(dagger.slot(), dagger);
    }
    return result;
  }

  /** {@link SaveData} から入手済みカード ID 集合を復元する。 */
  public static Set<CardId> toObtainedCards(SaveData data) {
    Set<CardId> result = new HashSet<>();
    for (String id : data.obtainedCardIds()) {
      result.add(CardId.of(id));
    }
    return result;
  }

  /**
   * {@link SaveData} から {@link Bestiary} を復元する (Wave 6 W6-β、§15-5)。
   *
   * <p>各 EnumKind.name() 文字列を {@link EnemyKind#valueOf} で復元。未知名 (将来の Enum 改変で削除された敵種 / typo 等) は
   * {@link IllegalArgumentException} を try-catch で受け、WARN ログ + graceful skip で続行する。 これは {@link
   * #toLoadout} / {@link #toSoulTree} と同型の「未知 ID は WARN + skip」パターン。
   *
   * <p>v1 セーブ (defeatedEnemyKinds 欠落) は {@link SaveData} の compact constructor で空リストに正規化されるため、
   * 結果として空の Bestiary が返る (graceful migration)。
   */
  public static Bestiary toBestiary(SaveData data) {
    Set<EnemyKind> kinds = EnumSet.noneOf(EnemyKind.class);
    for (String name : data.defeatedEnemyKinds()) {
      try {
        kinds.add(EnemyKind.valueOf(name));
      } catch (IllegalArgumentException e) {
        LOG.warning("Unknown EnemyKind in save data, skipping: " + name);
      }
    }
    return new Bestiary(kinds);
  }

  /**
   * {@link SaveData} の Stats 部分からデッキを組み立て、{@link Player} の {@link CardPileState} を復元する。
   *
   * <p>未定義のカード ID は graceful にスキップ (警告ログ)。山札はシャッフル済み前提で全カードを DrawPile に積む。
   *
   * <p><b>初期ドローはこのメソッドでは行わない</b>。呼び出し元 {@code InitialStateFactory.fromSaveData} (line 450 付近) が
   * {@code cardPileState.drawN(CardPileState.initialDrawCount(data.deck().size()), rng)} で
   * 上位フローで初期手札を引く設計。ロード後の最初の PLAYER_TURN 開始時には手札に初期ドロー枚数が入った 状態でゲームが始まる (devils-advocate 検証済
   * 2026-05-23)。
   */
  public static CardPileState toCardPileState(SaveData data) {
    List<Card> cards = new ArrayList<>();
    for (String id : data.deck()) {
      try {
        Card card = InitialStateFactory.resolveCard(CardId.of(id));
        cards.add(card);
      } catch (IllegalArgumentException e) {
        LOG.warning("Unknown card id in save data, skipping: " + id);
      }
    }
    // 全カードを山札に積む (手札は空、捨て札は空 — 初期ドローは上位 InitialStateFactory が担当)
    DrawPile drawPile = new DrawPile(cards);
    return new CardPileState(drawPile, Hand.empty(), DiscardPile.empty());
  }
}
