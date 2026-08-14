# 敵対的コードレビュー・品質検証レポート (2026-08-14)

## 概要

本ドキュメントは、Phase 0 〜 Phase 6 で実施された「カード描画の再構築と保存層 P0 の解消」に関する全変更に対するクリティカル・敵対的検証（Adversarial Verification）の評価記録である。

---

## 1. 総合判定

| 項目 | 判定 | 概要 |
|---|---|---|
| **全体判定** | **PASS（マージ可能品質）** | P0 重大欠陥は全件根治され、924 テスト全件 PASS |
| **P0 / Critical 指摘** | **0 件** | リリース遮断・データ破壊・無限ループ等の致命的問題なし |
| **Warning（要検討・推奨対応）** | **3 件** | 極端なエッジケースおよび境界値の防衛的強化 |
| **Info（改善提案）** | **3 件** | 将来スプリント（M2 等）での UI/描画ブラッシュアップ |

---

## 2. 領域別詳細検証結果

### (1) 保存層 & Legacy マイグレーション (Save & Soul Settlement)
* **検証対象**: `SoulSettlement.java`, `LegacySaveMapper.java`, `ProfileDataMapper.java`, `DddGame.java`
* **検証結果**:
  * `SoulSettlement.settle`: 純関数化され、異常 Checkpoint（`finalRunSoul < initialRunSoul`）時に `OptionalInt.empty()` を返却して Profile/Checkpoint の破壊を確実に遮断している。
  * `LegacySaveMapper`: v1, v2, v3 の全バージョンにおいて、旧 `slot_expand` 返金（1個につき 40 Soul）を含めた Player Soul の同値性が数学的・テスト的に固定されている。
  * `initialRunSoul`: 必須化され、省略オーバーロードの全廃により 0 注入の温床が根絶された。
* **指摘**:
  * `[Warning]` **Soul 計算における int オーバーフロー防止**: `SoulSettlement.settle` において、`previousTotal + netEarned` が極端な値（チートや想定外の巨大値）で `Integer.MAX_VALUE` を超えた場合、負値へラップアラウンドする理論的余地がある。`Math.min((long) previousTotal + netEarned, Integer.MAX_VALUE)` 等の飽和加算（Saturating Addition）への防衛的対応を推奨。

### (2) 装備システム & 所有権不変条件 (Equipment & Ownership)
* **検証対象**: `EquipmentOwnership.java`, `PlayerProgress.java`, `EquipmentScreen.java`, `DddGame.java`
* **検証結果**:
  * `PlayerProgress`: コンパクトコンストラクタで `loadout` が `equipmentOwnership.ownedIds()` の部分集合であることを厳格にバリデーションしている。
  * `DddGame.equipInLoadout`: 未所有の装備 ID が渡された場合に処理を即時拒否するガードが確立された。
  * `EquipmentScreen`: `availableEquipment()` により Profile の所有装備のみを一覧表示・選択対象とするよう修正された。
* **指摘**:
  * `[Warning]` **行クリック判定の厳密化**: `EquipmentScreen.handleClick()` の `Math.round((LIST_TOP_Y + scrollOffset - w.y) / ROW_HEIGHT)` は、行と行の隙間をクリックした際に最近接行へ丸め込まれる。所有装備が少ない場合に余白部分をクリックして誤装着されるのを防ぐため、明示的な行境界 `bounds.contains(w.x, w.y)` 判定へのリファクタリングが推奨される。
  * `[Info]` **保護指定トグル UI**: ドメイン層（`EquipmentOwnership`）には保護枠管理が備わっているが、`EquipmentScreen` の UI 上での保護 ON/OFF 切替表示は今後の UI スプリントでの実装課題。

### (3) ソウルツリー幾何学 & 座標 SSoT (SoulTree Layout SSoT)
* **検証対象**: `SoulTreeLayout.java`, `TreeLayoutGeometry.java`, `SoulTreeScreen.java`, `tree.json`
* **検証結果**:
  * `SoulTreeLayout`: 純粋 Java 側に座標 SSoT が新設され、二重管理が根絶された。
  * `TreeLayoutGeometryTest`: 全 22 ノード（root 1 + 内輪 6 + 中輪 5 + 魂の刻印 2 + 外輪 6 + 層拡張 2）に対して、ノード間最小距離 `100.0px` 以上および前提線の中間ノード貫通 0 件が自動幾何テストで検証されている。
* **指摘**:
  * `[Info]` **画面解像度とオフセット**: `SoulTreeScreen` は `FitViewport` (1920x1080) を採用しているため、解像度切替時も座標アライメントの一貫性が担保されている。

### (4) 敵ターンテンポ制御 (Enemy Turn Duration Control)
* **検証対象**: `DungeonScreen.java`, `EnemyTurnIntervalTest.java`
* **検証結果**:
  * `initialEnemyActionBudget` による 1 回確定と `1.2f / budget` の可変間隔演算により、AP が 1 でも 28 でも敵ターンの演出時間が約 1.2 秒以内に収まる。
  * `enemyStepTimer -= stepInterval` による余剰時間保持と、全 AP 消化後の即時 `PLAYER_TURN` 復帰処理により、タイマー待ちの無駄が完全に排除された。
  * 60fps / 144fps シミュレーションテストにより、不規則なフレームレートでも安定して 1.35 秒以内に完了することがテスト固定された。
* **指摘**:
  * `[Warning]` **低フレームレート時の安全ガード挙動**: `while` ループ内の `maxStepsPerFrame = 10` はハング防止として適切だが、フレームレートが極端に低下（15fps 以下等）した場合に 1 フレームで 10 ステップまでしか進まず、複数フレームに分散して 1.2 秒よりわずかに遅延する可能性がある。Graceful Degradation としては正常。

### (5) カード描画基盤 (Code-only Card Renderer)
* **検証対象**: `CardRenderer.java`, `CardRenderMode.java`, `CardRendererTest.java`
* **検証結果**:
  * 1x1 白テクスチャを用いた外枠・背景・属性ヘッダー・アート領域・テキストのコード合成描画が実装された。
  * Canonical Layout (120x168) に基づくスケーリングにより、手札・図鑑・ツリーアイコン等の各解像度に対応。
  * 描画終了時の `batch.setColor(Color.WHITE)` 徹底により、他コンポーネントへの色リークを防止。
* **指摘**:
  * `[Info]` **カード名テキストのオーバーフロー**: カード名が長い場合、ヘッダー領域の右端からはみ出る可能性があるため、将来的に文字列長に応じたフォントサイズ縮小やトリム処理の追加が推奨される。

---

## 3. 指摘一覧サマリー

| 重大度 | 対象ファイル | 指摘内容 | 推奨対応方針 |
|---|---|---|---|
| **Warning** | `SoulSettlement.java` | Soul 加算における極端な値での int オーバーフロー対策 | `Math.min((long) prev + diff, Integer.MAX_VALUE)` による飽和加算の適用 |
| **Warning** | `EquipmentScreen.java` | 装備一覧の行クリック判定における隙間丸め込みの厳密化 | `bounds.contains(x, y)` による厳密な領域内判定 |
| **Warning** | `DungeonScreen.java` | 敵ターン処理における超低フレームレート時のステップ分散 | 安全ガードとして許容（現状で問題なし） |
| **Info** | `CardRenderer.java` | 長いカード名におけるヘッダー帯のはみ出し防止 | 文字列長に応じたフォントスケールまたはトリム |
| **Info** | `EquipmentScreen.java` | 装備保護枠トグルの UI 提供 | M2 等の UI スプリントで追加 |
| **Info** | `SoulTreeScreen.java` | 解像度変更時のカメラ追従性 | FitViewport で保証済み |

---

## 4. 結論

今回の実装（Phase 0〜Phase 6）において、敵対的レビューで指摘されていた Critical 欠陥（Legacy Soul 精算の写像ズレ、0 注入、装備全開放、ソウルツリー座標二重管理、画像依存）はすべて完全に解消されました。残る指摘は軽微なエッジケース対策および将来の改善提案（Warning 3件、Info 3件）にとどまり、システムの堅牢性およびアーキテクチャの品質は極めて高い水準にあります。
