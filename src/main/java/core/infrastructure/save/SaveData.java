package core.infrastructure.save;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 層単位セーブの永続データ (§15-11)。Jackson でフラットな JSON にシリアライズする。
 *
 * <p>セーブは層と層の間 (戦闘外) にのみ行うため、DungeonState / DungeonMap を保持しない。 ロード後は次の層に進入する際に InitialStateFactory
 * で通常生成する。スロットは 1 つ。
 *
 * <p>互換性維持のため {@code schemaVersion} を持つ。将来フィールド追加時は version を上げ、 ロード側で旧バージョンの graceful な変換を実装する。
 *
 * <p>Jackson の record デシリアライズは全引数コンストラクタを自動検出する (jackson-databind 2.12+)。 {@link
 * com.fasterxml.jackson.databind.ObjectMapper} に {@code
 * com.fasterxml.jackson.module.paramnames.ParameterNamesModule} が不要なのは、 {@link JsonProperty}
 * アノテーションで各引数名を明示するため。
 */
public record SaveData(
    @JsonProperty("schemaVersion") int schemaVersion,
    @JsonProperty("nextLayerNumber") int nextLayerNumber,
    // プレイヤーステータス (7 ステ)
    @JsonProperty("currentHp") int currentHp,
    @JsonProperty("maxHp") int maxHp,
    @JsonProperty("speed") int speed,
    @JsonProperty("physicalAttack") int physicalAttack,
    @JsonProperty("magicalAttack") int magicalAttack,
    @JsonProperty("physicalDefense") int physicalDefense,
    @JsonProperty("magicalDefense") int magicalDefense,
    // デッキ (山札 + 手札 + 捨て札の全カード ID)
    @JsonProperty("deck") List<String> deck,
    // メタ進捗
    @JsonProperty("soulTotal") int soulTotal,
    @JsonProperty("runCount") int runCount,
    @JsonProperty("unlockedNodeIds") List<String> unlockedNodeIds,
    @JsonProperty("obtainedCardIds") List<String> obtainedCardIds,
    // EquipmentSlot.name() → EquipmentId.value()
    @JsonProperty("loadout") Map<String, String> loadout,
    // §15-5 / E-7 (Wave 6 W6-β) 撃破済敵種 (EnemyKind.name())
    @JsonProperty("defeatedEnemyKinds") List<String> defeatedEnemyKinds,
    // §15-10 / E-10 (Wave 6 W6-β) チュートリアル既読フラグ
    @JsonProperty("tutorialSeen") boolean tutorialSeen) {

  /**
   * 現在のスキーマバージョン。フィールド構造が変わった時に上げる。
   *
   * <p>v1 (旧): defeatedEnemyKinds / tutorialSeen なし。Wave 6 W6-β で 2 フィールド追加。 v1 セーブのロード時は Jackson
   * が欠落フィールドを null / false で埋め、compact constructor で graceful migration: defeatedEnemyKinds →
   * List.of()、tutorialSeen → false に正規化する。
   */
  public static final int CURRENT_SCHEMA_VERSION = 2;

  public SaveData {
    if (schemaVersion < 1) {
      throw new IllegalArgumentException("schemaVersion must be >= 1: " + schemaVersion);
    }
    if (nextLayerNumber < 1) {
      throw new IllegalArgumentException("nextLayerNumber must be >= 1: " + nextLayerNumber);
    }
    Objects.requireNonNull(deck, "deck");
    Objects.requireNonNull(unlockedNodeIds, "unlockedNodeIds");
    Objects.requireNonNull(obtainedCardIds, "obtainedCardIds");
    Objects.requireNonNull(loadout, "loadout");
    deck = List.copyOf(deck);
    unlockedNodeIds = List.copyOf(unlockedNodeIds);
    obtainedCardIds = List.copyOf(obtainedCardIds);
    loadout = Map.copyOf(loadout);
    // v1 graceful migration: 欠落 (null) は空リストに正規化、Jackson が null を渡しても破綻しない
    defeatedEnemyKinds = defeatedEnemyKinds != null ? List.copyOf(defeatedEnemyKinds) : List.of();
  }
}
