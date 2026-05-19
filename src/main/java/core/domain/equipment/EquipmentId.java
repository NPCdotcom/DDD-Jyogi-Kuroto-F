package core.domain.equipment;

import java.util.Objects;

/**
 * 装備の識別子 (§15-9 / ADR-25)。値オブジェクト。
 *
 * <p>{@link Equipment} と分離することで、装備マスタの参照やショップでの「同じ装備か」判定を {@code .equals()} 1 回で済ませる
 * (record の構造的等価)。
 */
public record EquipmentId(String value) {
  public EquipmentId {
    Objects.requireNonNull(value, "value");
    if (value.isBlank()) {
      throw new IllegalArgumentException("value must not be blank");
    }
  }

  public static EquipmentId of(String value) {
    return new EquipmentId(value);
  }
}
