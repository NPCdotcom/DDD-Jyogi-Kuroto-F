/**
 * 状態フィードバック演出 (HP 低下警告、装備テーマ変動、ヒットエフェクト等) の格納先。
 *
 * <p>MVP では空。{@link core.presentation.screen.DungeonScreen} の描画ループから呼び出される、 「ドメイン状態 →
 * 視覚効果」へのアダプタを置く想定。
 *
 * <p>着手予定: <a href="../../../../../docs/SystemSummary.md">SystemSummary.md §8 Phase A-4</a> (HP
 * 低下警告演出)。
 */
package core.presentation.effect;
