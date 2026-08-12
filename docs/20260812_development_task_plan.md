# Deep Dead Dungeons 整合性スプリント実装計画

> **実装担当エージェント向け**：`subagent-driven-development`または`executing-plans`を使用し、各タスクをチェックボックス単位で進める。
> 各タスクでテストを先に追加し、想定した理由で失敗することを確認してから最小実装へ進む。
> 複数タスクを同時に進める場合は担当ファイルを分け、共有契約を変更するタスクを先に統合する。

**Goal**：保存、装備ロスト、Soul進行、カード表示、素材来歴、配布説明を、Desktop向け戦術ローグライトという一つの契約へ揃える。

**Architecture**：恒久状態をProfile、進行中ランをRunCheckpointへ分け、装備精算はapplication層の純粋関数へ置く。
カードはデータ、フレーム、art-only画像を実行時合成し、資産証跡と依存通知をreleaseCheckで検証する。

**Tech Stack**：Java 25、Gradle 9.5、LibGDX 1.14、Jackson 2.21、JUnit 5、GitHub Actions。

---

## 1. 目標

本スプリントでは、作品を「死亡は痛いが、挑戦で得た経験と一部の価値は次のランへ残るDesktop向け戦術ローグライト」として整える。

追加コンテンツより先に、保存、装備所有、Soul進行、カード表示、素材来歴、配布説明の契約を一致させる。

## 2. 採択するゲーム規則

### 2.1 保存単位

**Profile**はラン外の恒久状態を保持する。

Profileには、保有Soul、周回数、ソウルツリー、所有装備、編成、保護指定、図鑑、取得カード、チュートリアル状態、アクティブなランID、最後に精算したランIDを保存する。

**RunCheckpoint**は進行中ランだけを保持する。

RunCheckpointには、ランID、次の層、現在能力値、デッキ、Gold、ラン中獲得Soul、持込装備、ラン中取得装備、現在装備、保護装備を保存する。

死亡またはクリアの精算後はRunCheckpointを削除する。

同じランIDを再度精算しても、Soulと周回数を二重加算しない。

RunCheckpointは、ProfileのアクティブなランIDと一致する場合だけ再開または精算できる。

RunCheckpointが存在する状態で新規ランを選ぶ場合は、「続行」または「放棄」を選ばせ、放棄を死亡と同じ規則で一度だけ精算してから次のランを開始する。

旧`save.json`の移行は`migration-state.json`をjournalとして使用し、Profile、RunCheckpoint、完了マーカーの各書込間で停止しても再開できるようにする。

旧バージョンのSoulは次の規則で移す。

| 旧schema | `soulTotal` | `currentRunSoul` | `currentRunGold` |
|---|---|---|---|
| v1 | Profileの保有Soul | 情報なしのため0。復元不能をログ表示 | 情報なしのため0。復元不能をログ表示 |
| v2 | Profileの保有Soul | 情報なしのため0。復元不能をログ表示 | 情報なしのため0。復元不能をログ表示 |
| v3 | Profileの保有Soul | RunCheckpointのラン中獲得Soul | RunCheckpointのGold |

### 2.2 装備の所有と死亡

初期装備の「ぼろい短剣」は常に再支給し、所有喪失、Soul変換、保護枠消費の対象外とする。

ラン中にGoldで購入した装備は**未確定品**としてRunInventoryへ入れる。

未確定品はクリア時に所有装備へ加え、死亡時にはSoulへ変換せず失う。

この規則により、Goldで装備を買って故意に死亡するGoldからSoulへの交換経路を遮断する。

ラン開始前に、装着中の所有装備から保護対象を指定する。

保護枠は初期0枠、ソウルツリーで最大2枠まで解放する。

死亡時は、持ち込んだ所有装備のうち保護品を維持し、未保護品を所有一覧から除いてSoulへ変換する。

装備の初期変換値は一律2 Soulとし、1回の死亡で受け取れる装備由来Soulを「2 × 踏破済み層数」までに制限する。

敵撃破とイベントでラン中に得たSoulは、死亡、放棄、クリアのいずれでも100%をProfileへ移す。

この値は最初のプレイ計測用であり、30固定シードのSoul毎分を測った後に変更できるよう、ゲーム規則へ集約する。

### 2.3 Soul報酬と解放

基本3層を全撃破した場合の敵由来Soulを12へ下げる。

初期値は通常スライム0、素早いスライム0、頑強なスライム1、強化スライム2、ボス5とする。

層別合計は1、4、7となる。

「ソウルの祠」は30 Soulから3 Soulへ変更し、HP消費5は維持する。

既存の無効な`slot_expand_1`から`slot_expand_5`は新効果へ読み替えない。

旧ノードを解放済みのProfileは、各40 Soulを返金して旧IDを削除する。

新規ノードは「魂の刻印 I」を30 Soul、「魂の刻印 II」を60 Soulとし、それぞれ装備保護枠を1枠増やす。

ソウルツリーをリセットしても魂の刻印IとIIは解放済みのまま残し、その購入額90 Soulを返金しない。

敵由来Soulを43から12へ下げる本改定は収入側だけの変更では完結しない。

[GAME_DESIGN.md](GAME_DESIGN.md)は「1ラン平均で10〜15ソウル獲得が目安。ソウルツリーの主要ノードコストと整合させる」と定めるため、ノード価格側もSOUL-04で再スケールする。

現行ツリーは25ノードで総額834 Soulであり、SlotExpand 5件200 Soulを刻印90 Soulへ置換しても724 Soul残る。

収入15 Soulのまま価格を据え置くと全解放に約50ラン必要になり、保護枠2枠を得るまでの装備ロストが無防備になるため、総額を300 ± 30 Soulへ引き下げる。

### 2.4 カード表示

カードの名称、AP、レアリティ、効果は`cards.json`とドメインモデルを唯一の値ソースとする。

NPCdotcom制作の固定フレーム、カードID別のイラスト専用PNG、実行時テキストを共通Rendererで合成する。

制作原本と実行用素材は分離する。

実行用のフレームとイラストは300×420の透過キャンバスへ統一し、手札では120×168へ等比縮小する。

手札には名称、AP、レアリティ、短い効果要約を表示し、選択詳細には省略しない効果全文を折り返して表示する。

### 2.5 配布対象と素材

現スプリントの配布対象はJava 25とLWJGL3を使用するDesktop版とする。

Windowsは実行検証済みとし、macOSとLinuxはCIだけなら「ビルド検証済み」と表記する。

macOSまたはLinuxを「実行検証済み」と表記するには、該当OSでnative読込とタイトル画面表示のスモーク試験を別途通す。

Androidと端末間セーブ同期は現行版の対象外と明記する。

OtoLogic由来を再同定できる15音源は、公式素材名、URL、ライセンス、改変内容を記録した後に保持する。

Springin’候補2件と同定不能3件は、再配布条件を記録できる代替音源へ置換する。

カードのAI原画を人間が加筆した場合も、AI原出力と人間の編集履歴を残す。

取得時の規約本文または保存写し、適用版、適用日、原本ハッシュ、同定方法、配布経路ごとの再配布条件を証跡へ含める。

作者名や編集者名を公開する場合は、公開名と公開同意を記録し、プロンプトとメタデータへ秘密情報や不要な個人情報を含めない。

HEADの置換だけで過去の再配布問題が消えたとは扱わず、コミット、タグ、Release、CI成果物、forkからの取得可能性を人間が判断する。

## 3. 依存関係

```text
BASE-01
  ├─ EQUIP-00 → SAVE-01 → SAVE-02A → SAVE-02B → SAVE-03A → SAVE-03B
  │                                                        ├─ EQUIP-01 ───────────────┐
  │                                                        └─ SOUL-01 → SOUL-02 → SOUL-03
  │                                                                       │            └→ EQUIP-02 → EQUIP-03 → SETTLE-01 → SETTLE-02
  │                                                                       └→ SOUL-04
  ├─ CARD-01 ─┐
  │           ├→ CARD-03 → CARD-04 ───────────────┐
  ├─ CARD-02 ─┘         └→ CARD-06                ├→ ART batches → CARD-05
  ├─ INPUT-01                                      │
  └─ PLATFORM-01
       └─ ASSET-01
            ├─ AUDIO-01 → AUDIO-02 → HISTORY-01 ─┐
            └─ DEPENDENCY-01 ────────────────────┼→ CREDIT-01 → RELEASE-01

ASSET-01 ─────────────────────────────────────────→ ART batches

EQUIP-01 + CARD-01 ─→ REWARD-01

SETTLE-02 + REWARD-01 + INPUT-01 + CARD-05 + CARD-06 + SOUL-04 + RELEASE-01
  └─ QA-01 → QA-02
```

`EQUIP-00`で所有とラン所持品の型を固定してから保存schemaを作る。

`SAVE-01`から`SAVE-03B`までは保存形式を共有するため直列で進める。

カード基盤と配布説明は、保存契約の実装と並行できる。

CARD-01とCARD-02は別の人間ゲートを持ち、CARD-03で合流する。

カード画像の量産は、1枚の表示スパイクとASSET-01の台帳契約を人間が承認してから開始する。

## 4. 実装タスク

### BASE-01 基準線と退行試験を固定する

**説明**：既存の761テスト、fat JAR、資産数を再計測し、以後の変更が退行を持ち込んでいないか判定できる基準線を記録する。

**本タスクでは失敗する試験を作らない**。回帰試験は各実装タスクの中でred → greenとして書く。基準線タスクが恒常的な赤を残すと、以後のタスクで「既知の赤」と「新しい退行」を区別できなくなり、バッチ完了時の`gradlew check`成功条件とも矛盾するためである。

**対象ファイル**：

- `tasks/todo.md`

**完了条件**：

- `gradlew check`が成功し、スイート数、テスト数、失敗数を記録する。基準は73スイート、761テスト、失敗0。
- `fatJar`が成功し、生成JARのサイズを記録する。基準は約33 MiB。
- 追跡バイナリ資産数を記録する。基準は99件。
- 上記を`tasks/todo.md`へ記録する。
- この時点で失敗している試験が存在しない。

**検証**：

```powershell
.\gradlew.bat --no-daemon check fatJar
```

**依存**：なし。

### EQUIP-00 所有とラン所持品のドメイン契約を先に固定する

**説明**：保存schemaを作る前に、所有装備、保護容量、持込品、未確定品、現在装備の不変条件を純粋な型として定義する。

**対象ファイル**：

- `src/main/java/core/domain/equipment/EquipmentOwnership.java`（新規）
- `src/main/java/core/domain/equipment/RunInventory.java`（新規）
- `src/main/java/core/domain/equipment/RetentionCapacity.java`（新規）
- `src/test/java/core/domain/equipment/EquipmentOwnershipTest.java`（新規）
- `src/test/java/core/domain/equipment/RunInventoryTest.java`（新規）

**完了条件**：

- 保護容量は0から2だけを許可する。
- 保護ID数は保護容量以下であり、持込品かつ所有品に限る。
- 初期短剣は所有一覧に必ず存在し、保護IDへ入らない。
- 未確定品と持込品を区別し、同じIDを一つのランで重複取得できない。
- 現在装備は持込品と未確定品の和集合に含まれる。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.domain.equipment.EquipmentOwnershipTest" --tests "core.domain.equipment.RunInventoryTest"
```

**依存**：BASE-01。

### SAVE-01 ProfileとRunCheckpointの保存器を追加する

**説明**：単一`save.json`へ混在している恒久状態と進行中ランを、`profile.json`と`run-checkpoint.json`へ分離する。

**対象ファイル**：

- `src/main/java/core/infrastructure/save/ProfileData.java`（新規）
- `src/main/java/core/infrastructure/save/RunCheckpoint.java`（新規）
- `src/main/java/core/infrastructure/save/SaveManager.java`
- `src/test/java/core/infrastructure/save/SaveManagerTest.java`
- `src/test/java/core/infrastructure/save/ProfileCheckpointRoundTripTest.java`（新規）

**完了条件**：

- ProfileとRunCheckpointを別々に保存、読込、存在確認、削除できる。
- ProfileとRunCheckpointの保護容量は0から2、保護ID数は容量以下であり、違反データを読込まない。
- 保存APIは`SaveManager.SaveResult`で成功と失敗を呼出側へ返し、I/O例外を成功扱いにしない。
- 保存は同じディレクトリの一時ファイルへ書いた後、原子的置換を試みる。
- 原子的置換が未対応の場合は旧世代を`.bak`として保持し、一時ファイルを置換して再読込検証した後だけ`.bak`を削除する。
- フォールバック中に失敗した場合は`.bak`を復元し、成功を返さない。
- 未来の`schemaVersion`は読込を拒否し、既存ファイルを上書きしない。
- 読取専用または書込不能な保存先で失敗を返し、既存ProfileとRunCheckpointを削除しない。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.infrastructure.save.SaveManagerTest" --tests "core.infrastructure.save.ProfileCheckpointRoundTripTest"
```

**依存**：EQUIP-00。

### SAVE-02A 旧save.jsonの値を新schemaへ写像する

**説明**：v1からv3の`SaveData`を、版別の変換表に従ってProfileとRunCheckpointへ純粋変換する。

**対象ファイル**：

- `src/main/java/core/infrastructure/save/SaveDataConverter.java`
- `src/main/java/core/infrastructure/save/LegacySaveMapper.java`（新規）
- `src/test/java/core/infrastructure/save/SaveDataConverterTest.java`
- `src/test/java/core/infrastructure/save/LegacySaveMapperTest.java`（新規）
- `src/test/resources/save/legacy-save-fixtures.json`（新規）

**完了条件**：

- 旧loadoutの全IDを所有装備へ移し、初期短剣を必ず所有一覧へ加える。
- `slot_expand_1`から`slot_expand_5`を削除し、1件につき40 Soulを一度だけ返金する。
- v1とv2の`soulTotal`はProfileへだけ移し、欠落していたランSoulとGoldは0として復元不能を通知する。
- v3の`soulTotal`をProfileへ、`currentRunSoul`と`currentRunGold`をRunCheckpointへ移し、Soul総量を二重計上しない。
- 旧loadoutを所有品、持込品、現在装備へ写し、未確定品と保護品は空で開始する。
- カタログ外IDはログへ残し、既知の装備と進捗を巻き添えで失わない。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.infrastructure.save.SaveDataConverterTest" --tests "core.infrastructure.save.LegacySaveMapperTest"
```

**依存**：SAVE-01。

### SAVE-02B 移行journalで書込間障害から復旧する

**説明**：Profile、RunCheckpoint、移行完了マーカーを別々に書く途中で停止しても、旧`save.json`から不足側だけを再生成する。

**対象ファイル**：

- `src/main/java/core/infrastructure/save/LegacyMigrationState.java`（新規）
- `src/main/java/core/infrastructure/save/LegacySaveMigrator.java`（新規）
- `src/main/java/core/infrastructure/save/PersistenceServices.java`
- `src/test/java/core/infrastructure/save/LegacySaveMigratorTest.java`（新規）
- `src/test/java/core/infrastructure/save/SaveManagerTest.java`

**完了条件**：

- 旧ファイルのSHA-256を移行IDとしてjournalへ保存する。
- Profile保存後、RunCheckpoint保存後、完了マーカー保存後の各地点へ障害を注入し、再起動後に**全体を決定的に再生成する**。
- 返金とSoul写像は旧ファイルから決定的に再計算し、既存Profileへ加算を繰り返さない。
- ProfileとRunCheckpointを再読込して**値**を検証した後だけ完了マーカーを保存する。
- 完了マーカーを保存するまで旧`save.json`を削除せず、完了後は同じ移行IDを再処理しない。
- **既存の`profile.json`がある状態では移行せず、旧ファイルも残す**。ただし同一移行IDの中断再開時は書きかけを上書きしてよい。

> **2026-08-13 改定 (敵対的検証の反映)**
>
> 2 つの条件を実態へ合わせて書き換えた。「実装したことにする」を避けるための訂正である。
>
> - 「**不足ファイルだけ**を復元する」→「**全体を決定的に再生成する**」。journal に進捗フラグを持たせて部分再開する設計より、純関数で全体を作り直す方が壊れにくい。初版はフラグを持っていたが分岐に一度も使われておらず、未使用状態を残すと将来「意味がある」と誤読されるため削除した (KISS / Deletion First)。
> - 「**移行IDと値**を検証」→「**値**を検証」。移行IDはProfileにもRunCheckpointにも保存していないため、再読込による照合は現schemaでは原理的に不可能だった。IDを持たせるほどの利得が無いと判断し、条件側を実態へ落とす。
>
> 既存Profileのクロバー防止は、敵対的検証で見つかった進捗全損の経路に対する追加条件として新設した。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.infrastructure.save.LegacySaveMigratorTest" --tests "core.infrastructure.save.SaveManagerTest"
```

**依存**：SAVE-02A。

### SAVE-03A RunCheckpointとRunSessionを相互変換する

**説明**：RunSessionへ一意なランIDを持たせ、層境界の状態とRunCheckpointを相互変換する。

**対象ファイル**：

- `src/main/java/core/application/RunId.java`（新規）
- `src/main/java/core/application/RunSession.java`
- `src/main/java/core/infrastructure/save/RunCheckpointMapper.java`（新規）
- `src/main/java/core/infrastructure/bootstrap/InitialStateFactory.java`
- `src/test/java/core/infrastructure/save/RunCheckpointMapperTest.java`（新規）

**完了条件**：

- 新規ランへ一意なランIDを割り当てる。
- RunSessionをRunCheckpointへ変換し、同じランID、次層、能力値、デッキ、通貨、RunInventory、保護スナップショットを保持する。
- `toCheckpoint`は保護ID数が保護枠を超える場合に呼出元へ分かる例外を投げる (`RunInventory`は容量を持たないため型では防げない)。

**2026-08-12 移設**: 下記 2 条件は SAVE-03A では満たさない。敵対的検証で未実装と判明し、実装順序に依存関係があるため **SETTLE-01 へ移す**。

- ~~ラン開始時にProfileの保有Soulを0へ移さず、ラン中獲得Soulは0から開始する。~~ → SETTLE-01
- ~~Profileの保有Soulはソウルツリー購入だけに使い、敵とイベントのSoulは精算までRunCheckpoint側へ保持する。~~ → SETTLE-01
- ~~RunCheckpointから復元したRunSessionを再保存しても、ランIDと一時所持品を維持する。~~ → SAVE-03B (RunSession を Checkpoint から復元する経路が SAVE-03B の担当のため)

移設の理由: 現行 `DddGame.startNewRun()` は `progress.playerSoul()` を Player へ注入して `progress` 側を 0 にする。これを SAVE-03A の段階で外すと、精算 (SETTLE-01/02) が未実装なのでラン中獲得 Soul の行き先が無くなる。

**実装順序の警告 (SETTLE-01 着手時に厳守)**: 先に `DddGame.preserveSoulFromRun()` を「上書き」から「Profile 保有はそのまま、ラン中 Soul のみ加算」へ変え、**その後で** `startNewRun()` の注入と 0 化を削る。逆順にするとラン終了時に Profile 保有分が `player.soul()` で上書きされ、ソウルツリー用の貯金が全損する。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.infrastructure.save.RunCheckpointMapperTest" --tests "core.infrastructure.bootstrap.InitialStateFactoryAdvanceLayerTest"
```

**依存**：SAVE-02B。

### SAVE-03B タイトルの続行を新保存形式へ接続する

**説明**：タイトルの「つづき」を有効なRunCheckpointだけに連動させ、精算実装が入るまで進行中ランの暗黙上書きを禁止する。

**対象ファイル**：

- `src/main/java/core/application/RunLifecyclePolicy.java`（新規）
- `src/main/java/core/presentation/screen/DddGame.java`
- `src/main/java/core/presentation/screen/TitleScreen.java`
- `src/main/java/core/presentation/render/Strings.java`
- `src/test/java/core/infrastructure/save/SaveLifecycleContractTest.java`

**完了条件**：

- RunCheckpointを書いた後にProfileのアクティブなランIDを確定し、両者が一致しないCheckpointを再開しない。
- 層境界で同じランIDを持つRunCheckpointを更新する。
- Profileだけが存在する場合、「つづき」を表示しない。
- Checkpoint存在中の新規開始を拒否し、「続行またはランの放棄が必要」と表示する。
- 新規開始、層末保存、再起動の契約試験がLibGDX画面を起動せずに通る。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.infrastructure.save.SaveLifecycleContractTest"
```

**依存**：SAVE-03A。

### EQUIP-01 所有装備と編成の不変条件を追加する

**説明**：Profileへ所有装備IDと保護指定IDを追加し、編成が所有一覧を越えないようにする。

**対象ファイル**：

- `src/main/java/core/domain/meta/PlayerProgress.java`
- `src/main/java/core/domain/equipment/EquipmentOwnership.java`
- `src/test/java/core/domain/meta/PlayerProgressTest.java`
- `src/test/java/core/domain/equipment/EquipmentOwnershipTest.java`

**完了条件**：

- 新規Profileの所有装備は初期短剣だけである。
- `loadout`と保護指定は所有装備の部分集合である。
- 保護指定は装着中装備に限る。
- 保護指定数はソウルツリーから導出した容量以下、かつ2以下である。
- 装備を失った場合、編成と保護指定から同じIDを除く。
- 外部の可変Collectionで内部状態を変更できない。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.domain.meta.PlayerProgressTest" --tests "core.domain.equipment.EquipmentOwnershipTest"
```

**依存**：SAVE-03B。

### EQUIP-02 所有装備だけを装着できる画面へ直す

**説明**：装備画面の全カタログ無料開放を廃止し、所有、装着、保護指定を一つの一覧で操作できるようにする。

**対象ファイル**：

- `src/main/java/core/presentation/screen/EquipmentScreen.java`
- `src/main/java/core/presentation/screen/DddGame.java`
- `src/main/java/core/presentation/render/Strings.java`
- `src/main/java/core/presentation/render/RenderLayout.java`
- `src/test/java/core/presentation/render/RenderLayoutConstraintTest.java`

**完了条件**：

- 一覧に所有装備だけを表示する。
- 未所有IDを装着APIへ渡した場合は状態を変えない。
- 装着行の右側に保護切替領域を表示し、上限を越える選択を拒否して理由を表示する。
- 初期短剣は常時保証と表示し、保護切替を表示しない。
- 装着解除した装備は保護指定からも外れる。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "*EquipmentOwnershipTest" --tests "core.presentation.render.RenderLayoutConstraintTest"
```

**依存**：EQUIP-01、SOUL-03。

### EQUIP-03 ラン中装備をRunInventoryへ分離する

**説明**：持込品、未確定品、現在装備を別々に記録し、同部位の買替えで旧装備を消さない。

**対象ファイル**：

- `src/main/java/core/domain/equipment/RunInventory.java`
- `src/main/java/core/application/RunSession.java`
- `src/main/java/core/presentation/screen/DddGame.java`
- `src/main/java/core/presentation/screen/DungeonScreen.java`
- `src/test/java/core/domain/equipment/RunInventoryTest.java`

**完了条件**：

- ラン開始時に持込品と保護指定をスナップショットする。
- ショップ購入品は未確定品へ加え、Profile所有一覧を変更しない。
- 同部位を買い替えても旧装備IDをRunInventoryに残す。
- 同じ装備IDを同一ランで複数回購入または精算できない。
- RunCheckpoint往復後も三つの集合が一致する。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.domain.equipment.RunInventoryTest" --tests "*ProfileCheckpointRoundTripTest"
```

**依存**：EQUIP-02、SAVE-03B。

### SOUL-01 敵報酬とソウルの祠を目標範囲へ戻す

**説明**：仕様上の平均10から15 Soulと実装上の全撃破43 Soulの差を解消する。

**対象ファイル**：

- `src/main/java/core/domain/entity/EnemyKind.java`
- `src/main/java/core/domain/layer/EventKind.java`
- `src/test/java/core/domain/battle/TurnEngineGoldRewardTest.java`
- `src/test/java/core/domain/layer/LayerEndNodeTest.java`
- `src/test/java/core/infrastructure/bootstrap/InitialStateFactoryEnemyVarietyTest.java`

**完了条件**：

- 敵Soulを0、0、1、2、5へ変更する。
- ソウルの祠を+3 Soul、-5 HPへ変更する。
- 基本3層の全敵合計が12 Soulであることを試験で固定する。
- Gold報酬は変更しない。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.domain.battle.TurnEngineGoldRewardTest" --tests "core.domain.layer.LayerEndNodeTest" --tests "core.infrastructure.bootstrap.InitialStateFactoryEnemyVarietyTest"
```

**依存**：SAVE-03B。

### SOUL-02 無効なスキル枠ノードを装備保護ノードへ移行する

**説明**：プレイヤー側で廃止済みのSlotExpand効果を削除し、意味を変えた新IDで装備保護枠を追加する。

**対象ファイル**：

- `src/main/java/core/domain/tree/NodeEffect.java`
- `src/main/java/core/infrastructure/bootstrap/SoulTreeCatalog.java`
- `src/main/resources/tree.json`
- `src/test/java/core/domain/tree/NodeEffectTest.java`
- `src/test/java/core/infrastructure/bootstrap/SoulTreeCatalogTest.java`

**完了条件**：

- `SlotExpandEffect`を新規Profileへ適用しない。
- `EquipmentRetentionEffect(1)`を読込できる。
- 「魂の刻印 I」は30 Soul、「魂の刻印 II」は60 Soulで、後者は前者を前提とする。
- ツリー内に購入可能な無効果ノードが残らない。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.domain.tree.NodeEffectTest" --tests "core.infrastructure.bootstrap.SoulTreeCatalogTest"
```

**依存**：SAVE-02B、SOUL-01。

### SOUL-03 保護容量をソウルツリーへ接続する

**説明**：解放済み効果から0から2の保護容量を導出し、ツリー画面に新ノードを表示する。

**対象ファイル**：

- `src/main/java/core/domain/tree/SoulTree.java`
- `src/main/java/core/presentation/screen/SoulTreeScreen.java`
- `src/main/java/core/presentation/render/NodeIconPathResolver.java`
- `src/test/java/core/domain/tree/SoulTreeTest.java`
- `src/test/java/core/presentation/render/NodeIconPathResolverTest.java`

**完了条件**：

- 未解放は0、Iのみは1、IとIIは2を返す。
- ツリーリセット後も魂の刻印IとIIを解放済みに残し、その90 Soulを返金額へ含めない。
- アクティブなRunCheckpointを再開してリセットしても、Checkpoint内の保護容量とProfileの容量が食い違わない。
- ツリー画面で両ノードの名称、価格、前提、効果を確認できる。
- 刻印ノードのアイコンが`test.png`フォールバックのまま完了しない。`NodeIconPathResolver`が返す`icons/frame.png`と`icons/center.png`は現在実在せず、SlotExpand 5件・None 1件・LayerExtend 2件の計8ノードが代替素材を表示している。刻印用アイコンを新規作成するか、`assets/icons/stats/`の既存素材を流用するかを選び、選択を記録する。
- `NodeIconPathResolverTest`が、刻印ノードに対して実在するファイルパスを返すことを検証する。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.domain.tree.SoulTreeTest" --tests "core.presentation.render.NodeIconPathResolverTest"
```

**依存**：SOUL-02。

### SOUL-04 ソウルツリーのノード価格を新しい収入へ再スケールする

**説明**：SOUL-01で1ラン収入を73から15へ下げた分、ノード価格側を引き下げて[GAME_DESIGN.md](GAME_DESIGN.md)の「ノードコストと整合させる」条件を満たす。SOUL-01だけを実施すると全解放が約50ランに膨張する。

**対象ファイル**：

- `src/main/resources/tree.json`
- `docs/GAME_DESIGN.md`
- `src/test/java/core/infrastructure/bootstrap/SoulTreeCatalogTest.java`

**完了条件**：

- ツリー全解放の総額が300 ± 30 Soulに収まる（現行834、刻印置換後724）。
- 「魂の刻印 I」30 Soul、「魂の刻印 II」60 Soulを維持する。
- 最安ノードが3 Soul以下であり、[GAME_DESIGN.md](GAME_DESIGN.md) §15-12のデモ「1ラン後にノードを1つライブ解放」が成立する。
- `LayerExtend`の100と200を再設定する。据え置くと2ノードだけで20ラン必要になる。
- 相対的な価格の序列を保つ。StatsBonus小 < StatsBonus大 < CardGrant < LayerExtend の関係を崩さない。
- `SoulTreeCatalogTest`が総額と最安値を固定し、以後の価格ドリフトを検知する。

**参考（現行内訳、2026-08-12実測）**：

| 効果種別 | ノード数 | 現行価格 | 現行小計 |
|---|---|---|---|
| None | 1 | 0 | 0 |
| StatsBonus | 12 | 4,4,5,5,6,10,10,12,12,14,22,30 | 134 |
| CardGrant | 5 | 30,30,40,50,50 | 200 |
| SlotExpand | 5 | 40×5 | 200（SOUL-02で廃止） |
| LayerExtend | 2 | 100,200 | 300 |

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.infrastructure.bootstrap.SoulTreeCatalogTest"
```

**依存**：SOUL-02。

### SETTLE-01 死亡とクリアの精算を純粋関数にする

**説明**：画面から精算規則を分離し、同じ入力から同じProfile更新を返す`RunSettlement`を追加する。

**対象ファイル**：

- `src/main/java/core/application/RunOutcome.java`（新規）
- `src/main/java/core/application/RunSettlement.java`（新規）
- `src/main/java/core/application/RunSettlementResult.java`（新規）
- `src/test/java/core/application/RunSettlementTest.java`（新規）

**完了条件**：

- 死亡時は初期短剣と保護品を残し、未保護の持込品だけを失う。
- 放棄時は死亡と同じ装備規則を適用する。
- 未確定品は死亡時に消え、装備由来Soulを生まない。
- 装備由来Soulは1品2、合計は2×踏破済み層数以下である。
- 敵とイベントで得たラン中Soulは、死亡、放棄、クリアのすべてで100%をProfileへ移す。
- クリア時は持込品を維持し、未確定品を所有化する。
- 同じ`lastSettledRunId`を受け取った再精算はProfileを変更しない。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.application.RunSettlementTest"
```

**依存**：EQUIP-03、SOUL-03。

### SETTLE-02 精算を保存とゲーム終了画面へ接続する

**説明**：死亡またはクリアを一度だけ精算し、Profile保存成功後にRunCheckpointを削除する。

**対象ファイル**：

- `src/main/java/core/presentation/screen/DddGame.java`
- `src/main/java/core/presentation/screen/GameOverScreen.java`
- `src/main/java/core/presentation/screen/TitleScreen.java`
- `src/main/java/core/infrastructure/save/PersistenceServices.java`
- `src/test/java/core/infrastructure/save/SaveLifecycleContractTest.java`

**完了条件**：

- `GameOverScreen.show()`が複数回呼ばれても周回数とSoulを一度だけ加算する。
- 精算対象のランIDがProfileのアクティブなランIDと一致しない場合はProfileを変更しない。
- タイトルで進行中ランを放棄した場合も、死亡と同じ精算を一度だけ行ってから新規ランを許可する。
- Profile保存に失敗した場合、RunCheckpointを削除せず再試行可能にする。
- Profile保存後にクラッシュしてRunCheckpointが残っても、再起動時に報酬を再加算せず削除できる。
- 死亡とクリアの表示に、保持、喪失、所有化、獲得Soulの内訳を出す。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "*SaveLifecycleContractTest" --tests "core.application.RunSettlementTest"
```

**依存**：SETTLE-01。

### REWARD-01 カード報酬抽選をapplication層へ移す

**説明**：強化敵の報酬抽選を画面から分離し、解放状態とレアリティを固定シードで検証できるようにする。

**対象ファイル**：

- `src/main/java/core/application/CardRewardSelector.java`（新規）
- `src/main/java/core/presentation/window/EliteRewardOrchestrator.java`
- `src/main/java/core/presentation/screen/DungeonScreen.java`
- `src/test/java/core/application/CardRewardSelectorTest.java`（新規）
- `src/test/java/core/presentation/screen/DungeonScreenDrawSeForTest.java`

**完了条件**：

- 抽選候補をProfileで利用可能なカードへ限定する。
- COMMON、RARE、UNIQUEの重みを一か所へ定義する。
- 候補不足時も重複表示せず、0から3件を返す。
- 同じシードと入力集合で同じ結果を返す。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.application.CardRewardSelectorTest" --tests "core.presentation.screen.DungeonScreenDrawSeForTest"
```

**依存**：EQUIP-01、CARD-01。

### INPUT-01 SPACEキーの説明を実装へ合わせる

**説明**：SPACEをターン終了と説明している箇所を、現行の待機1回へ統一する。

**対象ファイル**：

- `src/main/java/core/presentation/render/Strings.java`
- `src/main/java/core/presentation/render/HudRenderer.java`
- `src/test/java/core/presentation/render/InputHelpTextTest.java`（新規）

**現状**：誤った記述は3箇所ある。`Strings.java:175`（日本語`TUTORIAL_BODY`）、`Strings.java:391`（英語`TUTORIAL_BODY`）、`HudRenderer.java:168`（コメント「SPACE / ENTER と同等」）。一方`Strings.java:24-25`の`CONTROLS_WAIT`「SPACE 待機 (AP 1)」と`CONTROLS_END`「ENTER ターン終了」は正しい。したがって本件は一律の誤りではなく自己矛盾であり、正しい2箇所を壊さないこと。

**設計上の制約**：`PlayerInputs`は`Gdx.`静的参照を11箇所持ち、LibGDX headless試験基盤はプロジェクトに存在しない。既存のpresentation試験9件はすべて純粋クラスのみを対象とする。したがって`PlayerInputs`を直接駆動する試験は書かず、キー割当を純粋な対応表へ抽出して両者がそこから導出する構造にする。

**完了条件**：

- 日本語と英語のチュートリアル、操作ヒント、HUDが同じ規則を示す。
- `core.presentation.input`にLibGDX非依存の純粋型を置き、「SPACE = 待機 (AP 1)」「ENTER = ターン終了」の対応を単一ソースにする。`Keys`定数はint値で保持し、`com.badlogic`をimportしない。
- `PlayerInputs`がその対応表を参照して分岐する。
- `InputHelpTextTest`は純粋クラスのみを対象とし`Gdx`に触れない。既存の`CardDescriberTest`や`LayerEndNodeLabelsTest`と同じ書き方に揃える。
- 表示文と対応表の不一致が試験で失敗する。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.presentation.render.InputHelpTextTest"
```

**依存**：BASE-01。

### CARD-01 cards.jsonのレアリティを明示する

**説明**：暗黙COMMONを廃止し、59枚すべてのレアリティをゲーム設計として確定する。

**対象ファイル**：

- `src/main/resources/cards.json`
- `src/test/java/core/infrastructure/bootstrap/CardCatalogTest.java`
- `docs/GAME_DESIGN.md`
- `docs/20260812_card_rarity_approval.md`（新規）

**完了条件**：

- 59件すべてに`rarity`がある。
- 値は画像から自動転記せず、59 IDとrarityを列挙した承認文書から転記する。
- 承認文書に版、承認者の公開名、承認日、59行のSHA-256を記録する。
- 欠落、未知値、重複IDで試験が失敗する。
- `cards.json`と承認文書が完全一致し、全件COMMONへの一括置換を検出する。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.infrastructure.bootstrap.CardCatalogTest"
```

**依存**：BASE-01、人間ゲートG3。

### CARD-02 カード資産manifestと1枚の互換スパイクを作る

**説明**：旧完成PNGを維持したまま、`zangeki`だけを新しいフレームとイラストへ移行する。

**対象ファイル**：

- `src/main/resources/card_image_map.json`
- `src/main/java/core/infrastructure/bootstrap/CardImageRegistry.java`
- `src/test/java/core/infrastructure/bootstrap/CardAssetManifestTest.java`
- `assets/cards/frame/card_frame.png`（新規）
- `assets/cards/art/zangeki.png`（新規）

**完了条件**：

- manifestのカードID集合が`cards.json`の59 IDと一致する。
- フレームと新規artが300×420 RGBAである。
- `zangeki`は新経路、残り58枚は旧完成PNG経路で読める。
- 未参照の`card_23.png`を検出する。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.infrastructure.bootstrap.CardAssetManifestTest"
```

**依存**：BASE-01、人間ゲートG0、G1。

### CARD-03 手札を動的合成し、詳細文を省略しない

**説明**：新経路のカードをイラスト、フレーム、動的値の順で描画し、旧カードとの段階移行を可能にする。

**対象ファイル**：

- `src/main/java/core/presentation/render/CardRenderer.java`
- `src/main/java/core/presentation/render/HudRenderer.java`
- `src/main/java/core/presentation/render/CardPresentationText.java`（新規）
- `src/test/java/core/presentation/render/CardPresentationTextTest.java`（新規）

**完了条件**：

- 名称、AP、レアリティ、短縮効果をCardから導出し、画像へ重複保存しない。
- 120×168の手札で最長名称、最大AP、全レアリティが切れない。
- 選択詳細は効果全文を折り返し、省略記号で捨てない。
- 描画後にSpriteBatchとFontの色を復元する。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.presentation.render.CardPresentationTextTest" --tests "core.presentation.render.CardDescriberTest" --tests "core.presentation.render.RenderLayoutConstraintTest"
```

**依存**：CARD-01、CARD-02。

### CARD-04 図鑑とソウルツリーを同じ表示モデルへ接続する

**説明**：値を表示する画面が共通モデルを迂回しないようにする。

**対象ファイル**：

- `src/main/java/core/infrastructure/bootstrap/CardImageRegistry.java`
- `src/main/java/core/presentation/screen/CardCollectionScreen.java`
- `src/main/java/core/presentation/screen/SoulTreeScreen.java`
- `src/test/java/core/infrastructure/bootstrap/CardAssetManifestTest.java`

**完了条件**：

- 図鑑はart、名称、AP、レアリティ、効果全文を表示する。
- ソウルツリーの小表示はartだけを使用する。
- 移行済み、旧形式、欠損の三状態を区別する。
- リリースビルドでは欠損をfallbackで隠さず失敗させる。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.infrastructure.bootstrap.CardAssetManifestTest"
```

**依存**：CARD-03、人間ゲートG2。

### ART-01からART-15 カードイラストを4枚単位で移行する

**説明**：残り58枚を高露出カードから4枚以下のバッチで新形式へ移す。

**対象ファイル**：各バッチで`assets/cards/art/<card-id>.png`を最大4件と`LICENSES/ASSET_PROVENANCE.json`を変更する。

**完了条件**：

- 各画像は300×420 RGBAで、枠、文字、数値、レアリティ記号を含まない。
- 各資産にAIサービス、モデル、生成日、元出力ハッシュ、人間編集者、編集内容、最終ハッシュを記録する。
- AIのみとNPCdotcom加筆済みを区別する。
- 各バッチの接触シートを人間が承認し、承認者、日付、対象ID、接触シートのハッシュをprovenanceへ記録する。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.infrastructure.bootstrap.CardAssetManifestTest" --tests "core.infrastructure.assets.AssetProvenanceTest"
```

**依存**：CARD-04、ASSET-01、人間ゲートG4。

### CARD-05 旧完成カード画像を本番経路から外す

**説明**：59枚の移行完了後にlegacy mappingと旧完成PNGを段階的に削除する。

**対象ファイル**：

- `src/main/resources/card_image_map.json`
- `src/main/java/core/infrastructure/bootstrap/CardImageRegistry.java`
- `src/test/java/core/infrastructure/bootstrap/CardAssetManifestTest.java`
- `docs/AssetProductionSpec.md`

旧PNGの削除は1コミット最大5ファイルに分ける。

**完了条件**：

- `cards.json`、art、provenanceのID集合が59件で一致する。
- legacy fallbackを削除する。
- 値を焼き込んだ旧PNGがfat JARに残らない。
- カード追加時にID対応表の手編集を必要としない。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.infrastructure.bootstrap.CardAssetManifestTest"
.\gradlew.bat --no-daemon check fatJar
```

**依存**：ART-01からART-15、人間ゲートG5。

### CARD-06 挙動が同一のカードを統合または差別化する

**説明**：レビューP1-6で確認された重複7組を解消する。カード名が違っても戦術判断が変わらない状態を残さない。

**対象ファイル**：

- `src/main/resources/cards.json`
- `src/main/java/core/domain/card/CardEffect.java`
- `src/test/java/core/domain/card/CardCatalogUniquenessTest.java`（新規）

**現行の重複群（2026-08-12実測、apCost・tag・element・effect・rarityが完全一致）**：

| 群 | カードID |
|---|---|
| 1 | `strong_strike`、`quick_stab`、`riposte` |
| 2 | `arcane_veil`、`magic_barrier` |
| 3 | `double_strike`、`armor_break` |
| 4 | `acid_splash`、`shadow_bolt` |
| 5 | `dash`、`retreat` |
| 6 | `spike_trap`、`poison_needle` |
| 7 | `thunder_rune`、`acid_pool` |

**完了条件**：

- 各カードが、位置、射程、方向、状態、デッキ操作、次手との連携のいずれかで少なくとも一つ固有差を持つ。
- 差を作れないカードは統合し、`cards.json`から削除する。削除する場合は`card_image_map.json`、ソウルツリーのCardGrant、初期デッキへの影響を確認する。
- `CardCatalogUniquenessTest`が、apCost・tag・element・effect・rarityの組が全カードで一意であることを検証する。
- カード枚数の変更は`CardAssetManifestTest`のID集合一致条件と矛盾しない。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.domain.card.CardCatalogUniquenessTest" --tests "core.infrastructure.bootstrap.CardAssetManifestTest"
```

**依存**：CARD-03。

### PLATFORM-01 Desktop版の表明へ統一する

**説明**：実装していないAndroidと端末間セーブ同期を現行機能として説明しない。

**対象ファイル**：三つの小タスクへ分ける。

- 文書A：`README.md`、`docs/GAME_DESIGN.md`、`docs/ProjectOverview.md`、`docs/SystemSummary.md`、`docs/FeatureCatalog.md`
- 文書B：`src/main/java/core/infrastructure/desktop/DesktopLauncher.java`、`src/main/java/core/presentation/render/Strings.java`、`docs/RolesDivision.md`、`docs/TechSelectionMemo.md`、`.github/ISSUE_TEMPLATE/bug_report.yml`
- 検証C：`src/test/java/core/infrastructure/desktop/PlatformClaimTest.java`（新規）、`docs/Schedule.md`、`tasks/m2_backlog.md`

**完了条件**：

- Windowsは実行検証済み、macOSとLinuxはCI通過後もビルド検証済み、Androidと端末間同期は対象外という記述が一致する。
- タイトルとサブタイトルから「どこでも動く」という未実証表明を外す。
- 過去の判断を記録するレビュー文書と`tasks/ai_log`は改変しない。
- README、画面タイトル、デモ脚本、Issue環境選択が同じ対象範囲を示す。
- **プロジェクト名とJAR名は変更しない**。新名称が未決のため、`settings.gradle`の`rootProject.name`と成果物名`DDD-Jyogi-Kuroto-F-0.1.0-MVP-all.jar`は本タスクの対象外とする。本タスクが直すのはAndroidと端末間同期という未実装機能の表明に限る。名称の統一は新名称の決定後に別タスクで行う。
- リポジトリ横断試験はレビュー文書、`tasks/ai_log`、本計画を履歴資料として許可し、それ以外の現行文書とコードにある「Android必須」「スマホで同じセーブ」「DesktopとAndroid両動作」を失敗させる。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.infrastructure.desktop.PlatformClaimTest"
$matches = @(rg -n 'PC・スマホで動く|Android.*必須|Desktop / Android 両動作|スマホ側で同じセーブ' .)
$allowed = @($matches | Select-String '20260812_|tasks[\\/]ai_log')
if ($matches.Count -ne $allowed.Count) { throw '現行ファイルに未実装表明が残っています' }
```

**依存**：BASE-01。

### ASSET-01 配布資産の来歴manifestを追加する

**説明**：配布バイナリごとに作者、AI関与、取得元、利用条件、改変、ハッシュ、承認状態を記録する。

**対象ファイル**：

- `LICENSES/ASSET_PROVENANCE.json`（新規）
- `LICENSES/asset-provenance.schema.json`（新規）
- `docs/AssetGuidelines.md`
- `src/test/java/core/infrastructure/assets/AssetProvenanceTest.java`（新規）
- `.gitignore`

**完了条件**：

- 追跡中の全PNG、OGG、TTFが一意な台帳行を持つ。
- path、種別、作者、AI関与、生成サービスとモデル、URL、ライセンス、改変、SHA-256、承認状態を検証する。
- 規約の適用版と適用日、保存写しとそのハッシュ、原本ハッシュ、同定方法、配布経路別の再配布条件を検証する。
- 作者と編集者には公開用表記名と公開同意の状態を持たせ、非公開の氏名やアカウント情報を配布manifestへ入れない。
- DotGothic16をGit追跡対象へ変更し、clean cloneとローカルのfat JAR差をなくす。
- 未承認資産がある場合、releaseCheckを失敗させられる。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.infrastructure.assets.AssetProvenanceTest"
```

**依存**：PLATFORM-01。

### DEPENDENCY-01 配布依存のライセンスとNOTICEを棚卸しする

**説明**：fat JARへ展開する直接依存、推移依存、native artifactについて、解決済みバージョンと配布条件を記録する。

**対象ファイル**：

- `LICENSES/DEPENDENCIES.json`（新規）
- `LICENSES/THIRD_PARTY_NOTICES.md`（新規）
- `build.gradle`
- `src/test/java/core/infrastructure/assets/DependencyLicenseTest.java`（新規）

**完了条件**：

- Gradleが解決した全runtime artifactについてgroup、name、version、classifier、ライセンス、公式URL、NOTICE要否を記録する。
- LibGDX、LWJGL、Jacksonと各native artifactのライセンス本文または公式参照を含める。
- `duplicatesStrategy = EXCLUDE`へ依存NOTICEの収集を任せず、プロジェクトの通知へ集約する。
- Javaランタイムは現行fat JARへ同梱しないことを明記し、将来JREを同梱する場合はvendorと再配布条件を別ゲートで確認する。
- 不明なライセンスまたはNOTICE要否が1件でもある場合、公開リリースを失敗させる。

**検証**：

```powershell
.\gradlew.bat --no-daemon dependencies --configuration runtimeClasspath
.\gradlew.bat --no-daemon test --tests "core.infrastructure.assets.DependencyLicenseTest"
```

**依存**：ASSET-01。

### AUDIO-01 OtoLogic音源15件の証跡を確定する

**説明**：埋込タグと公式配布ページを照合し、保持する15音源の帰属と改変を記録する。

**対象ファイル**：

- `LICENSES/INDEX.md`
- `LICENSES/OtoLogic_CC_BY_4.0.txt`（新規）
- `LICENSES/ASSET_PROVENANCE.json`
- `assets/audio/README.md`

**完了条件**：

- 15件すべてに公式素材名、公式URL、取得確認日、CC BY 4.0、クレジット、変換または編集内容を記録する。
- 取得時規約の保存写しと現行規約を分け、適用版、適用日、保存写しのハッシュを記録する。
- 現行OGGと公式原本の復号音声を照合し、原本ハッシュ、判定方法、確信度、判定者を台帳へ記録する。
- 音源ファイル名とSoundManagerの参照が一致する。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.infrastructure.assets.AssetProvenanceTest" --tests "core.infrastructure.audio.SoundManagerTest"
```

**依存**：ASSET-01。

### AUDIO-02 証明できない音源5件を置換する

**説明**：`title.ogg`、`dungeon.ogg`、`floor_advance.ogg`、`card_phisycal.ogg`、`status_up.ogg`を、出典と再配布条件を保存できる音源へ置き換える。

**対象ファイル**：5音源を二つの小タスクへ分け、各タスクで台帳も更新する。

**完了条件**：

- 旧5件のSHA-256が配布物から消える。
- 代替音源は有効なOGGで、作者、URL、利用条件、取得日、改変、ハッシュが承認済みである。
- `card_phisycal`は参照を一度に変更できる段階で`card_physical`へ改名する。
- 公開Gitでの素材単体再配布を許可しない条件の音源を採用しない。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.infrastructure.assets.AssetProvenanceTest" --tests "core.infrastructure.audio.SoundManagerTest"
```

**依存**：AUDIO-01、人間ゲートG6。

### HISTORY-01 過去の資産配布経路を棚卸しする

**説明**：置換前の音源とカード画像が、公開Git履歴や既存配布物から取得できる範囲を記録し、人間判断が必要な対象を分離する。

**対象ファイル**：

- `LICENSES/DISTRIBUTION_HISTORY.md`（新規）
- `LICENSES/ASSET_PROVENANCE.json`
- `tasks/todo.md`

**完了条件**：

- 対象資産ごとに初出コミット、削除コミット、到達可能なタグ、Release、CI成果物、既知forkを記録する。
- 現行条件で過去配布を説明できない資産を、人間または有資格者の判断待ちとして明示する。
- 履歴書換え、タグ削除、Release削除は自動実行せず、対象と影響を提示して個別承認を得る。
- 旧カード画像のバックアップタグを作る場合、そのタグ自体の再配布条件をG5で確認する。

**検証**：

```powershell
git log --all --name-status -- assets LICENSES
git tag --list
```

**依存**：AUDIO-02、人間ゲートG8。

### CREDIT-01 Creditsと配布通知を台帳から生成する

**説明**：ゲーム内表示、README、fat JAR内通知の内容を同じ構造化データから導出する。

**対象ファイル**：

- `src/main/java/core/presentation/screen/CreditsContent.java`（新規）
- `src/main/java/core/presentation/screen/CreditsScreen.java`
- `src/test/java/core/presentation/screen/CreditsContentTest.java`（新規）
- `LICENSES/THIRD_PARTY_NOTICES.md`
- `build.gradle`

**完了条件**：

- 「カード49枚」「BGM / SE未投入」という誤表示を削除する。
- 必須クレジットとゲーム内表示が一致する。
- `LICENSES/INDEX.md`、provenance、third-party noticeをfat JARへ同梱する。
- 直接依存、推移依存、native artifactの通知を`DEPENDENCIES.json`と一致させる。
- プロジェクトコードのライセンスは、寄与者と引用コードの権利連鎖を確認したうえで、所有者が選んだライセンスまたは「ライセンス未付与」をREADMEへ明示する。

**検証**：

```powershell
.\gradlew.bat --no-daemon test --tests "core.presentation.screen.CreditsContentTest" --tests "core.infrastructure.assets.AssetProvenanceTest"
.\gradlew.bat --no-daemon fatJar
```

**依存**：DEPENDENCY-01、HISTORY-01、人間ゲートG7。

### RELEASE-01 releaseCheckとOSマトリクスCIを追加する

**説明**：テスト成功だけでは見つからなかった資産、通知、配布物の欠落を単一コマンドで検出する。

**対象ファイル**：

- `build.gradle`
- `.github/workflows/quality.yml`（新規）
- `src/test/java/core/infrastructure/assets/DistributionContractTest.java`（新規）

**完了条件**：

- `releaseCheck`がspotless、test、fatJar、provenance、未承認資産0、通知同梱、音源20件、Main-Classを検証する。
- Windows、Ubuntu、macOSでJava 25を使用して`releaseCheck`を実行する。
- GUI起動はCI合否へ含めず、Windows実機の手動スモークへ分ける。

**検証**：

```powershell
.\gradlew.bat --no-daemon releaseCheck
```

**依存**：CREDIT-01。

### QA-01 自動試験と敵対シナリオを完走する

**説明**：保存、所有、精算、カード、資産、配布の境界をまとめて検証する。

**対象ファイル**：既存試験の修正に限定し、新しい本番機能は追加しない。

**完了条件**：

- 新規Profile、層末保存、強制終了、再開、死亡、クリア、再起動を通して進捗消失0、二重精算0である。
- 未所有装着、同一装備二重精算、スターターSoul化、Gold購入品Soul化が0件である。
- 59カードのデータ、art、provenanceが一致する。
- clean clone相当の入力だけでreleaseCheckが通る。

**検証**：

```powershell
.\gradlew.bat --no-daemon clean releaseCheck
```

**依存**：SETTLE-02、REWARD-01、INPUT-01、CARD-05、RELEASE-01。

### QA-02 実機確認とSoul経済を計測する

**説明**：自動試験では判断できない可読性、操作理解、Soul毎分、死亡損失を固定シナリオで確認する。

**対象ファイル**：

- `docs/20260812_playtest_results.md`（新規）
- `tasks/todo.md`

**完了条件**：

- Windows実機でタイトル、Credits、新規開始、装備、保護指定、戦闘、層末保存、再開、死亡、クリアを確認する。
- 30固定シードで全踏破と故意早期死亡のSoul毎分を測る。
- 全踏破のSoul毎分が最良の故意早期死亡の1.5倍以上である。
- 中盤死亡の50%以上で未保護装備を1件以上失う。
- 1920×1080と1280×720で手札9枚、最長名称、効果全文を読める。
- ソウルツリー全解放までの想定ラン数を記録し、20 ± 5ランに収まる。上の1.5倍条件は比率検定であり、収入と価格が同時に低すぎても通過してしまうため、絶対ペーシングを別途測る。
- 「魂の刻印 I」到達までの想定ラン数を記録し、3ラン以内に収まる。保護枠0のまま装備ロストが続く期間を短く保つため。
- 作品未見の5人へ説明なしで渡し、4人以上が30秒以内に開始し、SPACEをターン終了と誤解しないことを確認する。INPUT-01の修正効果はこの試験でのみ検証できる。
- 基準を満たさない場合は、原因、再調整値、再試験結果を同じ文書へ記録する。

**検証**：

```powershell
.\gradlew.bat --no-daemon run
```

**依存**：QA-01。

## 5. 人間の承認ゲート

| ゲート | 承認対象 | 通過しない場合の扱い |
|---|---|---|
| G0 | NPCdotcom制作フレームの編集原本、作者宣言、300×420書出し | CARD-02を開始しない |
| G1 | 画像生成サービス、モデル、契約プラン、生成時の規約版と適用日、商用利用、公開Git再配布、入力と参照画像の権限、学習と保持条件、禁止する参照画像と作家名指定 | 不明点を人間または有資格者へ送り、AIカード制作を開始しない |
| G2 | `zangeki`の手札、選択、図鑑、ソウルツリー表示 | 残り58枚を量産しない |
| G3 | 59カードのレアリティ一覧 | CARD-01と報酬重みを確定しない |
| G4 | 各4枚バッチの接触シート、AIのみとNPC加筆済みの区分 | 該当バッチを本番へ入れない |
| G5 | 全59枚の置換完了と旧画像のバックアップタグ | 旧完成PNGを削除しない |
| G6 | 代替音源5件の聞き比べ、取得時規約、公開Gitを含む配布経路別の利用条件 | 不明点を人間または有資格者へ送り、AUDIO-02を本番へ入れない |
| G7 | コード寄与者と引用コードの権利連鎖、依存条件、プロジェクトコードのライセンスまたはライセンス未付与の明示 | 不明点を人間または有資格者へ送り、公開リリースを作らない |
| G8 | 旧資産が残るコミット、タグ、Release、CI成果物、forkの扱い | 履歴変更を自動実行せず、公開リリースを作らない |

## 6. 並列担当

| 担当 | 所有範囲 | 開始可能時点 | 競合回避 |
|---|---|---|---|
| 保存担当 | SAVE-01からSAVE-03B | EQUIP-00後 | `DddGame.java`変更中は装備担当を止める |
| 進行担当 | EQUIP、SOUL、SETTLE | SAVE-03B後 | SOULデータと精算を順番に統合する |
| カード担当 | CARD、ART | BASE-01とG0後 | `CardImageRegistry.java`は同時編集しない |
| 配布担当 | PLATFORM、ASSET、AUDIO、CREDIT、RELEASE | BASE-01後 | `build.gradle`はCREDIT後にRELEASE担当へ渡す |
| テスト担当 | 各タスクの失敗試験、QA | 契約確定後 | 本番コードを編集しない |
| 批判担当 | 各チェックポイントの敵対的レビュー | 2から3タスクごと | 実装担当の説明を前提にせず差分と試験を見る |

## 7. チェックポイント

### チェックポイントA

対象はEQUIP-00とSAVE-01からSAVE-03Bである。

- ProfileとRunCheckpointが別ファイルに保存される。
- 旧v1からv3を一度だけ移行できる。
- 各書込間で停止しても移行journalから復旧できる。
- 終了済みランが「つづき」に出ない。
- 進行中ランの放棄が死亡と同じ精算へ送られる。
- focused testと`gradlew check`が通る。

### チェックポイントB

対象はEQUIP-01、SOUL-01からSOUL-04、SETTLE-01からSETTLE-02である。

- 初期短剣以外は購入またはクリア取得するまで装着できない。
- 死亡とクリアの精算が一度だけ行われる。
- Gold購入品を死亡でSoulへ変換できない。
- 保護枠は最大2である。
- ソウルツリー総額が300 ± 30 Soulに収まり、1ラン収入15 Soulと整合する。
- focused testと`gradlew check`が通る。

### チェックポイントC

対象はCARD-01からCARD-04、CARD-06、1枚スパイクである。

- `zangeki`の表示値を`cards.json`だけで変更できる。
- 手札と詳細で文字が切れない。
- 挙動が完全に同一のカード群が残っていない。
- 4画面をNPCdotcomが承認する。
- 量産可否を人間が判断する。

### チェックポイントD

対象はPLATFORM-01からRELEASE-01である。

- 現行プラットフォーム表明が実装と一致する。
- 配布全資産に来歴行がある。
- Creditsとfat JARの通知が一致する。
- `releaseCheck`がOSマトリクスで通る。

### 完了判定

- `gradlew clean releaseCheck`が成功する。
- Windows実機の保存、装備、死亡、クリア、Creditsのスモーク試験が成功する。
- 30固定シードの経済指標がQA-02の基準を満たす。
- final-architect、multi-perspective-reviewer、devils-advocateの指摘にP0またはP1未解決がない。
- `tasks/todo.md`のレビュー欄へコマンド、結果、残課題、残課題の完了条件を記録する。

## 8. 着手順

最初の開発バッチは**BASE-01 → EQUIP-00 → PLATFORM-01 → INPUT-01**とし、**直列で実行する**。

`SAVE-01`は保存schemaの中核で差分が大きいため、第1バッチから外して第2バッチの先頭に置く。EQUIP-00で型が確定してから着手する。

PLATFORM-01とINPUT-01は`src/main/java/core/presentation/render/Strings.java`を共有するため、並列に動かさない。PLATFORM-01を先に統合してからINPUT-01へ進む。

BASE-01は基準線の記録だけを行い、失敗する試験を残さない。各実装タスクが自分の回帰試験をred → greenで閉じる。

カード担当はG0とG1を待つ間に、manifest試験と動的表示モデルのテスト設計だけを進める。

第1バッチが通った時点で差分を統合レビューし、第2バッチ（SAVE-01以降）へ進む。

第2バッチはSAVE-01 → SAVE-02A → SAVE-02B → SAVE-03A → SAVE-03Bとし、保存形式を共有するため直列で進める。
