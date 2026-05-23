package core.domain.layer;

/**
 * {@link LayerEndNode.Event} の種別 (§15-8 / Wave 8 W8-α)。
 *
 * <p>従来は Event の {@code displayLabel: String} で日本語ラベルを外部注入していたが、 ドメイン層に日本語が混入する負債だったため Wave 8 で
 * Enum 化。各種別は固定の delta を保持し、 Event 生成時に kind と delta の整合性を compact constructor で検証する。
 *
 * <p>表示文字列の解決は presentation 層の {@code LayerEndNodeLabels} に委譲する (i18n 経由)。
 */
public enum EventKind {
  /** 治療の泉: HP +20 / ソウル -10。HP 回復のためにソウルを消費するトレードオフ。 */
  HEALING_SPRING(-10, 20, 0),
  /** 黄金の宝箱: 金貨 +50。純粋な報酬イベント。 */
  GOLDEN_CHEST(0, 0, 50),
  /** ソウルの祠: ソウル +30 / HP -5。リスクと引き換えにソウルを得る。 */
  SOUL_SHRINE(30, -5, 0);

  private final int defaultSoulDelta;
  private final int defaultHpDelta;
  private final int defaultGoldDelta;

  EventKind(int defaultSoulDelta, int defaultHpDelta, int defaultGoldDelta) {
    this.defaultSoulDelta = defaultSoulDelta;
    this.defaultHpDelta = defaultHpDelta;
    this.defaultGoldDelta = defaultGoldDelta;
  }

  public int defaultSoulDelta() {
    return defaultSoulDelta;
  }

  public int defaultHpDelta() {
    return defaultHpDelta;
  }

  public int defaultGoldDelta() {
    return defaultGoldDelta;
  }
}
