package core.presentation.render;

import core.domain.card.CardEffect;

/**
 * {@link CardEffect.BuffKind} の日英表示ラベル集約 (DRY)。
 *
 * <p>HudRenderer のログ描画と CardDescriber のカード説明描画で重複していたラベルマッピングを 1 か所に統合する。新規 BuffKind 追加時はここの switch
 * を更新するだけで全ての表示箇所に 反映される。
 */
public final class BuffKindLabels {

  private BuffKindLabels() {}

  /**
   * 指定 BuffKind の表示ラベルを返す。
   *
   * @param kind バフ種別
   * @param jp true なら日本語ラベル、false なら英語ラベル
   * @return ローカライズ済みラベル文字列
   */
  public static String labelOf(CardEffect.BuffKind kind, boolean jp) {
    return switch (kind) {
      case PHYSICAL_ATTACK_UP ->
          jp ? Strings.Ja.BUFF_KIND_PHYSICAL_ATTACK : Strings.En.BUFF_KIND_PHYSICAL_ATTACK;
      case MAGICAL_ATTACK_UP ->
          jp ? Strings.Ja.BUFF_KIND_MAGICAL_ATTACK : Strings.En.BUFF_KIND_MAGICAL_ATTACK;
      case PHYSICAL_DEFENSE_UP ->
          jp ? Strings.Ja.BUFF_KIND_PHYSICAL_DEFENSE : Strings.En.BUFF_KIND_PHYSICAL_DEFENSE;
      case MAGICAL_DEFENSE_UP ->
          jp ? Strings.Ja.BUFF_KIND_MAGICAL_DEFENSE : Strings.En.BUFF_KIND_MAGICAL_DEFENSE;
      case SPEED_UP -> jp ? Strings.Ja.BUFF_KIND_SPEED : Strings.En.BUFF_KIND_SPEED;
    };
  }
}
