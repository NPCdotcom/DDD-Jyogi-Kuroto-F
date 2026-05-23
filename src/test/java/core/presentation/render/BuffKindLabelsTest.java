package core.presentation.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import core.domain.card.CardEffect.BuffKind;
import org.junit.jupiter.api.Test;

/**
 * {@link BuffKindLabels} の単体テスト (Wave 7 W7-γ)。
 *
 * <p>5 種類の {@link BuffKind} すべてが Strings.Ja / Strings.En の対応キーを返すことを検証する。 switch の網羅性は record /
 * sealed 系列と異なり enum 用なので、enum 値が増えたときに各 case で漏れがあれば コンパイル時に検知できる。本テストは「現状のラベル一致」を回帰防止として固定する。
 */
class BuffKindLabelsTest {

  @Test
  void physicalAttackUp_ja() {
    assertEquals(
        Strings.Ja.BUFF_KIND_PHYSICAL_ATTACK,
        BuffKindLabels.labelOf(BuffKind.PHYSICAL_ATTACK_UP, true));
  }

  @Test
  void physicalAttackUp_en() {
    assertEquals(
        Strings.En.BUFF_KIND_PHYSICAL_ATTACK,
        BuffKindLabels.labelOf(BuffKind.PHYSICAL_ATTACK_UP, false));
  }

  @Test
  void magicalAttackUp_ja() {
    assertEquals(
        Strings.Ja.BUFF_KIND_MAGICAL_ATTACK,
        BuffKindLabels.labelOf(BuffKind.MAGICAL_ATTACK_UP, true));
  }

  @Test
  void magicalAttackUp_en() {
    assertEquals(
        Strings.En.BUFF_KIND_MAGICAL_ATTACK,
        BuffKindLabels.labelOf(BuffKind.MAGICAL_ATTACK_UP, false));
  }

  @Test
  void physicalDefenseUp_ja() {
    assertEquals(
        Strings.Ja.BUFF_KIND_PHYSICAL_DEFENSE,
        BuffKindLabels.labelOf(BuffKind.PHYSICAL_DEFENSE_UP, true));
  }

  @Test
  void physicalDefenseUp_en() {
    assertEquals(
        Strings.En.BUFF_KIND_PHYSICAL_DEFENSE,
        BuffKindLabels.labelOf(BuffKind.PHYSICAL_DEFENSE_UP, false));
  }

  @Test
  void magicalDefenseUp_ja() {
    assertEquals(
        Strings.Ja.BUFF_KIND_MAGICAL_DEFENSE,
        BuffKindLabels.labelOf(BuffKind.MAGICAL_DEFENSE_UP, true));
  }

  @Test
  void magicalDefenseUp_en() {
    assertEquals(
        Strings.En.BUFF_KIND_MAGICAL_DEFENSE,
        BuffKindLabels.labelOf(BuffKind.MAGICAL_DEFENSE_UP, false));
  }

  @Test
  void speedUp_ja() {
    assertEquals(Strings.Ja.BUFF_KIND_SPEED, BuffKindLabels.labelOf(BuffKind.SPEED_UP, true));
  }

  @Test
  void speedUp_en() {
    assertEquals(Strings.En.BUFF_KIND_SPEED, BuffKindLabels.labelOf(BuffKind.SPEED_UP, false));
  }

  @Test
  void japaneseAndEnglishLabelsDiffer() {
    // Ja / En のラベルは同じではない (国際化されていることの確認、回帰防止)
    for (BuffKind kind : BuffKind.values()) {
      assertNotEquals(
          BuffKindLabels.labelOf(kind, true),
          BuffKindLabels.labelOf(kind, false),
          kind.name() + " の Ja / En ラベルは異なる");
    }
  }
}
