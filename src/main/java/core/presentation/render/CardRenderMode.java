package core.presentation.render;

/**
 * カードの描画モード (Phase 5-3)。
 *
 * <ul>
 *   <li>{@link #FULL}: 手札 (120x168) や解放確認モーダル (240x336) 等のフル描画 (枠・ヘッダー・コスト・名前・詳細・アート)
 *   <li>{@link #THUMBNAIL}: カード図鑑等 (32x42) のサムネイル縮小描画 (枠・属性色・コスト・アート)
 *   <li>{@link #ICON}: ソウルツリー等 (64x64) の正方形アイコン描画
 * </ul>
 */
public enum CardRenderMode {
  FULL,
  THUMBNAIL,
  ICON
}
