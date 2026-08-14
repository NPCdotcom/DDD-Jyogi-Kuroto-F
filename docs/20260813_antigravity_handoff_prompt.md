# Antigravity 引き継ぎプロンプト

- 作成日: 2026-08-13
- 目的: DDD-Jyogi-Kuroto-F の開発・修正を次世代 AI エージェント（Antigravity）へ正確かつコンテキストの欠落なく引き継ぐための指示書。

---

## タスク

DDD-Jyogi-Kuroto-F (Java 25 + LibGDX の 2D ローグライト) の残課題の実装および保存層の敵対的レビューで発見された重大欠陥の修正を行ってください。

### 作業環境

- **リポジトリ**: `C:\.program\DDD-Jyogi-Kuroto-F` (Windows ローカル)
- **作業ブランチ**: `feat/consistency-sprint-batch1`。未 push なのでリモートには存在しません。ローカルで `git switch feat/consistency-sprint-batch1` を実行して開始してください。
- このブランチは `docs/consistency-sprint-plan` から分岐し、そこは `main` から分岐しています。
- **ビルド・検証コマンド**: `.\gradlew.bat --no-daemon check` (PowerShell。`JAVA_HOME`: `C:\Program Files\Java\jdk-25.0.3`)
- **現状の基準線**: **875 テスト、失敗 0**。この数値を下回った場合は退行とみなします。

> **最初にやること**:
> `git switch feat/consistency-sprint-batch1` を実行後、`.\gradlew.bat --no-daemon check` を実行し、875 テスト・失敗 0 が通過することを実測確認してください。ここが通らない場合は環境問題であるため、実装前に解決が必要です。

### スコープ（対応範囲と禁止範囲）

**対象に含まれるもの**:
1. 敵対的コードレビュー (2026-08-13) で判明した保存層の Critical / Required 欠陥の修正
2. 下記「実装タスク」 5 件
3. 保存層の未達要件 3 件

**対象外（絶対に着手・変更しないこと）**:

| 範囲外項目 | 理由・制限 |
|---|---|
| カード画像の生成・差し替え (CARD-01〜06、ART-01〜15) | 人間の承認ゲート **G0 はコード描画化により撤回**。**G1 (権利・モデル規約確認)**、**G3 (全59枚のレアリティ確定)** が未通過であり本番接続 **NO-GO 判定**。 |
| 素材台帳 (ASSET-01)、依存ライセンス (DEPENDENCY-01) | 人間による権利確認・ライセンス規約確定待ち |
| 音源の置換 (AUDIO-01/02)、クレジット (CREDIT-01) | 同上 |
| OS マトリクス CI (RELEASE-01) | 対応 OS の決定待ち |
| 敵の改名 (`SKELETON` / `GOBLIN` / `DRAGON`) | `tasks/m2_backlog.md` に破壊的変更として別登録 |
| プロジェクト名・JAR 名の変更 | 新名称が未決 |

---

## 必読ドキュメント（参照順序）

1. `CLAUDE.md` — 設計原則 8 項目、層分離ルール、命名規約
2. `docs/GAME_DESIGN.md` — 仕様の単一ソース (SSoT)
3. `tasks/todo.md` — 残課題・進捗の一覧
4. `docs/20260812_development_task_plan.md` — 33 タスクの実装計画
5. `docs/20260813_manual_findings_plan.md` — 手動確認 5 件の方針
6. `docs/20260812_comprehensive_adversarial_review.md` — 敵対的レビューの記録

---

## 絶対遵守事項

1. **TDD (テスト駆動開発)**: 必ずテストを先に書き、意図通りの理由で失敗することを確認してから実装コードを書いてください。
2. **クリーンアーキテクチャ・層分離**: `core.domain.*` は `java.*` のみに依存させてください。Jackson は保存層限定。LibGDX は `presentation` / `infrastructure` のみに閉じること。
3. **トートロジー・ダミーテストの禁止**: 常に true になるテストや、失敗を再現しない「偽のテスト」を書かないこと。
4. **`gradlew check` の常時緑維持**: 各タスクの完了ごとにすべてのテストを通してください。
5. **既存テストの保護**: 875 件以上のテストが通過し続けること。
6. **`main` への直接コミット禁止**

---

## 最優先課題: 敵対的レビューで発見された重大欠陥 (Critical / Required)

保存層におけるデータ損失・破壊に直結する以下の欠陥を優先して修正してください。

### 【Critical】欠陥 A: 未来スキーマ / 破損セーブデータ存在時に新ラン開始で既存ファイルが破壊・上書きされる

- **対象箇所**: `SaveManager.java`, `RunLifecycle.java`, `DddGame.java`
- **問題**: `SaveManager.read()` が未来バージョン (`version > supportedVersion`) や破損した `profile.json` を読込拒否 (`Optional.empty()`) した場合、`DddGame` は `ProfileData.initial()` を使用します。この状態でプレイヤーがタイトルから「新規開始 (ENTER)」を押すと、`beginRun` が初期化プロファイルで既存の `profile.json` を原子置換（上書き）してしまい、プレイヤーの過去の恒久進捗が全破壊されます (Contract 3 違反)。
- **対策**: `SaveManager` において「ファイル不在」と「読込拒否（未来スキーマ/破損）」を区別し、読込拒否時は `beginRun` や `saveProfile` による上書き操作をブロックするか、タイトル起動時にエラーを出して進行をストップしてください。

### 【Critical】欠陥 B: `onRunEnded()` における `runSession` 非存在時のサイレントな保存スキップ

- **対象箇所**: `DddGame.onRunEnded()`
- **問題**: `onRunEnded()` 内部で `runSession.ifPresent(...)` により精算保存が実行されているため、`runSession` が empty の状態で精算（死亡・クリア）画面へ入った場合、メモリ上の進捗のみが書き換わり、`profile.json` へのディスク書き込みが行われません (Contract 4 違反)。
- **対策**: `runSession` の有無に関わらず、精算発生時は必ず `lifecycle.endRun(settled)` を呼び出して `profile.json` に永続化してください。

### 【Required】欠陥 C: 旧 `save.json` 移行時の Hash / Journal 検証不全

- **対象箇所**: `LegacySaveMigrator.java`
- **問題**: 移行時の検証 `verify()` が `migrationId` を照合しておらず、途中で旧ファイルが置き換わった場合の整合性が担保されていません。
- **対策**: `migrationId` の検証の厳密化を行ってください。

---

## 実装タスク (優先順)

### 1. 【P1】SWIFT_SLIME がほぼ攻撃しないバグ

- **症状**: 手動プレイで「スケルトンの AI 挙動がおかしい」。
- **原因**: `EnemyKind.SWIFT_SLIME` だけ `EnemyAiProfile.CAUTIOUS`。`EnemyAi.cautiousAction` は隣接時に逃げるが、距離 2-3 では `hasRangedSkill` が `false` 固定のため `aggressiveAction` へ落ちて詰め直す。結果、AP を往復移動で使い切り攻撃しない。`docs/GAME_DESIGN.md:566` の「手数で攻める」仕様に違反。
- **やること**:
  - `EnemyKind.SWIFT_SLIME` の `aiProfile` を `AGGRESSIVE` へ変更
  - `EnemyAiProfile.CAUTIOUS` は残すが javadoc に「敵側 range 実装まで未使用」と明記
- **完了条件**:
  - 開けた床にプレイヤー隣接・AP 2 で `SWIFT_SLIME` を置き、`EnemyAi.decide` を AP が尽きるまで回すと `UseSkill` が 1 回以上返る回帰テスト
  - 既存の `EnemyAi` テストが壊れないこと

---

### 2. 【P1】敵ターンの表示が分かりにくい

- **症状**: 敵ターン中に入力できない時間が長いのに、HUD の小さい文字しか出ない（層 3 で 2.6〜3.0 秒）。
- **検討案**:
  - **案 A**: 敵ターン総時間を約 1.2 秒に固定し、待ち時間そのものを短くする。
  - **案 B**: 敵ターン開始時に画面中央へ大きく短時間表示してフェードアウト。
- **案 A の実装上の注意**: **「残アクション数」を返す API は存在しません**。`TurnDirector.stepEnemyTurnOnce()` は boolean を返すのみです。総アクション数は `ENEMY_TURN` 進入時に `state.enemies()` の AP 合計から自分で計算してください (3 行程度)。
- **案 B の実装上の注意**:
  - `ScreenEffects.showFlash` を**再利用しないこと**（既存メッセージを上書きしカード獲得通知等を潰すため）。
  - 独立フィールド + 独立 draw メソッドを追加する。
  - トリガは `BattleEvent.TurnPhaseChanged` を `DungeonScreen.processNewEvents` で拾うこと。
- **完了条件**: 敵ターン中に「待たされている」ことが分かり、表示の重複がないこと。

---

### 3. 【P1】ソウルツリーが「一個飛ばし」に見える

- **症状**: ノードを解放すると 1 つ飛ばしで解放されたように見える。
- **原因**: `SoulTreeScreen.positions()` のハードコードした極座標により、同一角度上に 4 ノードが並ぶスポークが 3 本あり、r200 → r700 の枝線が中間の r380 / r540 を物理的に貫通している。
- **やること**: 以下の 3 ノードの角度を変更する。
  - `phys_atk_up_2`: `polar(120, 700)` → `polar(105, 700)`
  - `phys_def_up_2`: `polar(240, 700)` → `polar(225, 700)`
  - `mag_def_up_2`: `polar(300, 700)` → `polar(315, 700)`
- **完了条件**:
  - 幾何判定を LibGDX 非依存の純粋クラスへ切り出し、単体テストすること
  - depth 差 1 かつ単一親の辺が、両端以外のノード円 (半径 32) を通過しないこと
  - 全ノード間距離が 2 × `NODE_RADIUS` (= 100 px) 以上であること

---

### 4. 【P0】装備が全開放されている

- **原因**: `EquipmentScreen.java:68` が `equipmentCatalog().all()` を所有判定なしで表示している。
- **対策**: **SOUL-02 → SOUL-03 → EQUIP-01 → EQUIP-02** の順に実装してください。
  1. **SOUL-02**: 廃止済みの `slot_expand_1〜5` ノードを削除し、装備保護枠ノード「魂の刻印 I/II」(30/60 Soul) を追加
  2. **SOUL-03**: 解放状態から保護枠 0〜2 を導出し、ソウルツリー画面へ接続
  3. **EQUIP-01**: Profile へ所有装備 ID と保護指定を追加し、編成が所有一覧を越えない不変条件を構築
  4. **EQUIP-02**: 装備画面が所有装備だけを表示・装着する（ここで症状が解消）
- **注意**: SOUL-02 だけを単独コミットしないでください。`SoulTreeScreen.positions()` に座標が無いと不可視ノードになりバグ化します。SOUL-02 と SOUL-03 は同一 PR / コミットで処理すること。

---

### 5. 【P2】壊れる壁が有用でない位置に置かれる

- **原因**: BSP マップの破壊可能壁が L 字迂回などの無意味な位置に置かれる。
- **対策**: 壁の両側 2 マス間の**破壊前の迂回距離**を局所 BFS で計測し、4 以上の真のショートカットになる位置に配置する。
- **完了条件**: 30 シードで、配置された壁の過半数が迂回距離 4 以上であること。

---

## 保存層の未達事項

- **SAVE-02A**: カタログ外 ID をログへ残す処理の追加
- **SAVE-01/02B**: `SaveManagerTest` 拡張および `src/test/resources/save/legacy-save-fixtures.json` の整備

---

## 過去に踏んだ罠 (再発防止)

1. 読込だけ新形式へ替え書込が旧形式のまま → 読み書きの両側を同時に切り替える
2. 層境界セーブで持越しソウルが 0 に潰れる → ラン中は `progress.playerSoul()` が 0 のため `ProfileDataMapper.forLayerBoundary` を使用する
3. 放棄確認が「死亡と同じ扱い」と表示するのに精算していない → コードで精算を必ず実行する
4. 起動時に `progress` を復元せず profile.json を全消去 → 起動時に必ず Profile から `progress` を正しく復元する
5. 再開時のソウル二重計上 → `restoreLayer` が既に総量を入れているため加算しない

---

## 検証手順

### 自動検証
- `.\gradlew.bat --no-daemon check` を実行し、875 テスト以上が失敗 0 で通ることを確認。

### 手動検証
- `.\gradlew.bat --no-daemon run` で実際にアプリを起動し、以下を目視確認すること：
  1. **敵 AI**: スケルトン（SWIFT_SLIME）が隣接時に逃げずに攻撃してくるか。
  2. **敵ターン表示**: 敵ターン中に状態が把握でき、表示の重複がないか。
  3. **ソウルツリー**: ノードの枝線が他ノードを貫通していないか。
  4. **セーブ＆ロード**: 保存先 (`%USERPROFILE%\.ddd-jyogi-kuroto-f\`) を削除 → 起動 → 1 層クリア → 終了 → 再起動 → 「つづき」で再開し、進捗が正しく残るか。

---

## 完了時チェックリスト

1. 自動・手動検証がすべて通過している。
2. `tasks/todo.md` に実施内容とテスト数・失敗数の実測値を記録した。
3. 未達の完了条件がある場合は隠さず明確に記録した。
