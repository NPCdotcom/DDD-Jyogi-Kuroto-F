# Antigravity 引き継ぎプロンプト

作成 2026-08-13。以下の `---` から下をそのまま貼って使う。

---

## タスク

DDD-Jyogi-Kuroto-F (Java 25 + LibGDX の 2D ローグライト) の残課題を実装してください。

### 作業環境

- リポジトリ: `C:\.program\DDD-Jyogi-Kuroto-F` (Windows ローカル)
- **作業ブランチ: `feat/consistency-sprint-batch1`**。未 push なのでリモートには無い。ローカルで `git switch feat/consistency-sprint-batch1` して始めること
- このブランチは `docs/consistency-sprint-plan` から分岐し、そこは `main` から分岐している。`main` には未反映の作業が 27 コミットある
- ビルド: `.\gradlew.bat --no-daemon check` (PowerShell。JAVA_HOME は `C:\Program Files\Java\jdk-25.0.3`)
- 現状の基準線: **875 テスト、失敗 0**。この数を下回ったら退行

> **最初にやること**: `git switch feat/consistency-sprint-batch1` して `.\gradlew.bat --no-daemon check` を実行し、875 テスト・失敗 0 を自分の目で確認する。ここが緑でなければ環境の問題なので、実装を始める前に解決すること。

### このプロンプトの範囲

**含む**: 下記「実装タスク」の 5 件と、保存層の未達 3 件。

**含まない** (別途対応するので手を出さないこと):

| 範囲外 | 理由 |
|---|---|
| カード画像の生成・差し替え (CARD-01〜06、ART-01〜15) | 人間の承認ゲート G0 (フレーム制作) と G1 (画像生成サービスの権利確認) が未通過。`docs/20260813_image_generation_brief.md` 参照 |
| 素材台帳 (ASSET-01)、依存ライセンス (DEPENDENCY-01) | 人間による権利確認が必要 |
| 音源の置換 (AUDIO-01/02)、Credits (CREDIT-01) | 同上 |
| OS マトリクス CI (RELEASE-01) | 対応 OS の決定待ち |
| 敵の改名 (`SKELETON` / `GOBLIN` / `DRAGON`) | `tasks/m2_backlog.md` に破壊的変更として別登録 |
| プロジェクト名・JAR 名の変更 | 新名称が未決 |

### 必読ドキュメント (この順で)

1. `CLAUDE.md` — 設計原則 8 項目、層分離、命名規約
2. `docs/GAME_DESIGN.md` — 仕様の単一ソース (SSoT)
3. `tasks/todo.md` — 残課題の一覧
4. `docs/20260812_development_task_plan.md` — 33 タスクの実装計画
5. `docs/20260813_manual_findings_plan.md` — 手動確認 5 件の方針
6. `docs/20260812_comprehensive_adversarial_review.md` — 元レビュー

---

## 絶対に守ること

1. **TDD**: テストを先に書き、意図した理由で失敗することを確認してから実装する
2. **層分離**: `core.domain.*` は `java.*` のみ依存。Jackson は保存層限定。LibGDX は `presentation` / `infrastructure` のみ
3. **トートロジー禁止**: 常に true になるテスト、失敗を再現しない「失敗テスト」を書かない
4. **`gradlew check` を緑に保つ**: 各タスク完了時に全テストが通ること
5. **既存テストを壊さない**: 875 件が通り続けること
6. **`main` へ直接コミットしない**

---

## 実装タスク (優先順)

### 1. 【P1】SWIFT_SLIME がほぼ攻撃しないバグ

**症状**: 手動プレイで「スケルトンの AI 挙動がおかしい」。

**原因**: `EnemyKind.SWIFT_SLIME` だけ `EnemyAiProfile.CAUTIOUS`。`EnemyAi.cautiousAction` は隣接時に逃げるが、距離 2-3 では `hasRangedSkill` が `false` 固定のため `aggressiveAction` へ落ちて詰め直す。結果、AP を往復移動で使い切り攻撃しない。

`docs/GAME_DESIGN.md:566` は「はやスライム: 素早い個体。AP = 層番号 +1、HP 低。**手数で攻める**」と定めており、CAUTIOUS は仕様違反。

**やること**:
- `EnemyKind.SWIFT_SLIME` の `aiProfile` を `AGGRESSIVE` へ
- `EnemyAiProfile.CAUTIOUS` は残すが javadoc に「敵側 range 実装まで未使用」と明記

**完了条件**:
- 開けた床にプレイヤー隣接・AP 2 で `SWIFT_SLIME` を置き、`EnemyAi.decide` を AP が尽きるまで回すと `UseSkill` が 1 回以上返る回帰テスト
- 既存の `EnemyAi` テストが壊れない

**やらないこと**: 敵の改名 (`SKELETON` 等)。`tasks/m2_backlog.md:85` に破壊的変更として別登録済み。

---

### 2. 【P1】敵ターンの表示が分かりにくい

**症状**: 敵ターン中に入力できない時間が長いのに、HUD の小さい文字しか出ない。

**所要時間**: `DungeonScreen.ENEMY_STEP_INTERVAL = 0.10f` ごとに **1 アクション**進む。所要は `0.10 × Σ(各敵の AP)`。層 3 で 2.6〜3.0 秒、層拡張後は 4 秒超。

**2 案あるので両方試して比較する**:

- **案 A**: 敵ターン総時間を約 1.2 秒に固定し、待ち時間そのものを短くする。UI を足さずに問題を消す
- **案 B**: 敵ターン開始時に画面中央へ大きく短時間表示してフェードアウト

**案 A の実装上の注意**: 「残アクション数」を返す API は**存在しない**。`TurnDirector.stepEnemyTurnOnce()` は「まだ続くか」の boolean を返すだけ。総アクション数は `ENEMY_TURN` 進入時に `state.enemies()` の AP 合計から自分で計算する必要がある (3 行程度)。1 行では済まないので注意。既存の `ENEMY_STEP_INTERVAL = 0.10f` は定数なので、フィールド化して毎ターン再計算する形になる。

**案 B を採る場合の注意**:
- `ScreenEffects.showFlash` を**再利用しないこと**。既存メッセージを上書きする実装なので、Elite 撃破のカード獲得通知を潰す
- 独立フィールド + 独立 draw メソッドを追加する
- トリガは `BattleEvent.TurnPhaseChanged` を `DungeonScreen.processNewEvents` で拾う (phase のポーリングにしない)
- 敵ターン表示は**現時点で 2 か所ある** (`HudRenderer` のフェーズ行とヒント行)。バナーを足すと 3 か所になるので、どれを消すか決める

**完了条件**: 敵ターン中に「待たされている」ことが分かる。表示の重複がない。

---

### 3. 【P1】ソウルツリーが「一個飛ばし」に見える

**症状**: ノードを解放すると 1 つ飛ばしで解放されたように見える。

**原因**: `SoulTreeScreen.positions()` はハードコードした極座標。枝線は `tree.json` の `prerequisites` から描く。同一角度上に 4 ノードが並ぶスポークが 3 本あり、r200 → r700 の枝線が中間の r380 / r540 を**物理的に貫通**する。

| 角度 | r200 | r380 | r540 | r700 (前提は r200) |
|---|---|---|---|---|
| 120 | `phys_atk_up_1` | `card_grant_strong_strike` | `slot_expand_1` | `phys_atk_up_2` |
| 240 | `phys_def_up_1` | `card_grant_iron_skin` | `slot_expand_4` | `phys_def_up_2` |
| 300 | `mag_def_up_1` | `card_grant_arcane_veil` | `slot_expand_5` | `mag_def_up_2` |

**解放判定は正しい。レイアウトが誤解を生んでいる。**

**やること**: 3 ノードの角度を変える。

- `phys_atk_up_2`: `polar(120, 700)` → `polar(105, 700)`
- `phys_def_up_2`: `polar(240, 700)` → `polar(225, 700)`
- `mag_def_up_2`: `polar(300, 700)` → `polar(315, 700)`

**完了条件**:
- 幾何判定を LibGDX 非依存の純粋クラスへ切り出し、単体テストする
- depth 差 1 かつ単一親の辺が、両端以外のノード円 (半径 32) を通過しない
- 全ノード間距離が 2 × `NODE_RADIUS` (= 100 px) 以上

**やらないこと**: 座標をデータ導出する大改修。角度割当が未定義で、`static final` 初期化の失敗経路もあり、SOUL-02 で全角度が動く。

---

### 4. 【P0】装備が全開放されている

`EquipmentScreen.java:68` が `equipmentCatalog().all()` を所有判定なしで表示する。

`docs/20260812_development_task_plan.md` の **EQUIP-01 → EQUIP-02** を実装してください。EQUIP-02 の完了条件「一覧に所有装備だけを表示する」で解消します。

**依存が 3 段ある。計画書で各タスクの節を読んでから着手すること。**

| 順 | タスク | 内容 (計画書に完了条件あり) |
|---|---|---|
| 1 | **SOUL-02** | 廃止済みの `slot_expand_1〜5` ノードを削除し、装備保護枠ノード「魂の刻印 I/II」(30/60 Soul) を追加 |
| 2 | **SOUL-03** | 解放状態から保護枠 0〜2 を導出し、ソウルツリー画面へ接続 |
| 3 | **EQUIP-01** | Profile へ所有装備 ID と保護指定を追加し、編成が所有一覧を越えない不変条件 |
| 4 | **EQUIP-02** | 装備画面が所有装備だけを表示・装着する ← **ここで症状が消える** |

**重要な落とし穴**: SOUL-02 だけを単独でコミットしないこと。`tree.json` に刻印ノードが増えても `SoulTreeScreen.positions()` に座標が無いと、null ガードで `continue` され**枝線もアイコンもクリック判定も出ない不可視ノード**になる。エラーも出ないので気付けない。SOUL-02 と SOUL-03 は同じ PR に入れること。

**タスク 3 (ツリーの角度修正) は SOUL-03 と同時に行うと効率が良い** (どちらも `SoulTreeScreen.java` を触る)。

---

### 5. 【P2】壊れる壁が有用でない位置に置かれる

`InitialStateFactory.placeBreakableWalls` は Wave 16 で「対向 2 方向が同時に FLOOR」= ショートカット壁優先の改善済みだが、体感で効いていない。

**推測で直さないこと。まず計測する。**

BSP マップは元から全域が連結しているので、連結成分では測れない。壁の両側 2 マス間の**破壊前の迂回距離**を局所 BFS で測り、2 なら L 字迂回で無意味、4 以上なら真のショートカット。

**完了条件**: 30 シードで、配置された壁の過半数が迂回距離 4 以上。

---

## 保存層の未達 (計画書の受入条件を満たしていない)

第2バッチ (SAVE-01〜03B) は完了しているが、以下が未達のまま。

- **SAVE-02A**: カタログ外 ID をログへ残す処理が未実装 (`LegacySaveMapper` にカタログ照合が無い)
- **SAVE-01/02B**: `SaveManagerTest` の拡張、`src/test/resources/save/legacy-save-fixtures.json` が未着手

> **保存層に触るときの警告**: この領域は直前のバッチで P0 級の欠陥を 5 件作り込んだ場所 (下記「過去に踏んだ罠」)。既存の `SaveLifecycleContractTest` / `ProfileCheckpointRoundTripTest` / `ProfileDataMapperTest` を必ず先に読み、**壊さないこと**。データ損失に直結するので、迷ったら実装せず未達として記録する方が安全。

---

## 過去に踏んだ罠 (同じ失敗を繰り返さないこと)

第2バッチで P0 級の欠陥を 5 件作り込んだ。原因と対策:

1. **読込だけ新形式へ替え書込が旧形式のまま** → 読み書きの両側を同時に切り替える
2. **層境界セーブで持越しソウルが 0 に潰れる** → ラン中は `progress.playerSoul()` が 0。`ProfileDataMapper.forLayerBoundary` を使う
3. **放棄確認が「死亡と同じ扱い」と表示するのに精算していない** → 表示した約束をコードで守る
4. **起動時に `progress` を復元せず profile.json を全消去** → `toProfileData` は previous からラン ID しか引き継がない
5. **再開時のソウル二重計上** → `restoreLayer` が既に総量を入れているので加算しない

**共通原因**: 契約テストが `RunLifecycle` しか通っておらず、本番の呼出元 (`DddGame` / `TitleScreen`) を 1 行も通っていなかった。**配線を検証するテストを書くこと。**

---

## 検証

### 自動 (必須)

1. `.\gradlew.bat --no-daemon check` が緑。テスト数が 875 以上
2. 各タスクの完了条件に書いた回帰テストが存在し、実装前に**意図した理由で失敗する**ことを確認してから実装する

### 手動 (自動テストでは検出できない)

**この 3 件は自動テストで判定できないので、`.\gradlew.bat --no-daemon run` で実際に起動して目視すること。** 過去に P0 級の欠陥 4 件が自動テストをすり抜けた実績がある。

| 対象 | 見るもの |
|---|---|
| タスク 1 (敵 AI) | スケルトン (はやスライム) が隣接時に逃げずに攻撃してくるか |
| タスク 2 (敵ターン) | 敵ターン中に「待たされている」と分かるか。表示が重複していないか |
| タスク 3 (ツリー) | ノードを解放したとき「1 つ飛ばし」に見えないか。枝線が他ノードを貫通していないか |

**保存に関わる変更を入れた場合は追加で**: 保存先 (`%USERPROFILE%\.ddd-jyogi-kuroto-f\`) を削除 → 起動 → 1 層クリア → 終了 → 起動 → 「つづき」で再開 → 進捗が残っているか。

## 完了時にやること

1. 上記の自動・手動検証を通す
2. `tasks/todo.md` に実施内容と実測値 (テスト数・失敗数) を記録
3. 各タスクで「どの設計原則を選んだか」を 1 行記録
4. **満たせなかった完了条件があれば、満たしたことにせず未達として記録する**
