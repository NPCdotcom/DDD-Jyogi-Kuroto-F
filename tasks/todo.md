# tasks/todo.md

> DDD-Jyogi-Kuroto-F のタスクボード。
> 仕様の単一ソース: [docs/GAME_DESIGN.md](../docs/GAME_DESIGN.md)
> 実装俯瞰: [docs/SystemSummary.md](../docs/SystemSummary.md)
> 進行詳細・レビュー記録: [ai_log/](./ai_log/) (リーダー個人ログ)

---

## 設計原則

実装・PR レビューのチェックリスト。

| 原則 | 本プロジェクトでの解釈 |
|---|---|
| **SOLID** | ドメイン層は単一責務 / 拡張に開き修正に閉じる / 依存は抽象 |
| **YAGNI** | MVP に含まれない機能 ([GAME_DESIGN.md §11-3](../docs/GAME_DESIGN.md)) は実装しない・テストしない |
| **KISS** | マルチモジュールにせずシングルモジュールから始める。段階的に複雑化する |
| **DRY** | ステ計算・AP 計算など複数箇所で使う処理は値オブジェクト or サービスに集約 |
| **驚き最小** | パッケージ構成と命名は [GAME_DESIGN.md §9](../docs/GAME_DESIGN.md) と [CommonSense.md](../docs/CommonSense.md) に従う |
| **不変性** | ドメイン値 (Stats, Position, ActionPoints, Soul) は `record` で不変に。状態変化は新インスタンスを返す |
| **副作用の分離** | I/O / 乱数 / 時間 / LibGDX 呼び出しは `infrastructure/` と `presentation/` に閉じ込める。`domain/` は純関数のみ |
| **明示的な依存関係** | コンストラクタ注入。`new` でドメインオブジェクトを直接組まない (ファクトリ or リポジトリ経由) |

> PR では「どの原則のどの選択をしたか」を一行で記録する習慣を付ける。

---

## 完了済み (Phase 0〜6.5)

MVP コア実装 + バージョン整備 + 日本語化までを 2026-05-12 に完了。詳細は [ai_log/phase_6_5_review.md](./ai_log/phase_6_5_review.md)。

| Phase | 内容 | 状態 |
|---|---|---|
| 0 | 仕様整理・設計原則の言語化 | ✅ |
| 1 | ビルド基盤 (Java 25 + Gradle 9.5.0 + LibGDX 1.14.0 + LWJGL 3.4.1) | ✅ |
| 2 | ドメイン層実装 + 61 件のテスト | ✅ |
| 3 | アプリ層 + インフラ層 (DddGame / GameContext / TurnDirector / DesktopLauncher / InitialStateFactory) | ✅ |
| 4 | プレゼン層 (Screen 群 / Renderer / 入力マッピング) | ✅ |
| 5 | 結合・動作確認 (`gradlew run` で起動、M1 合格チェックリスト全 ✅) | ✅ |
| 6.5-1 | ツール最新化 (JUnit 5.12.2 / Jackson 2.21.3 / Spotless 8.4.0 / GJF 1.35.0) | ✅ |
| 6.5-2 | テスト妥当性レビュー (A 評価、61 件全 PASS) | ✅ |
| 6.5-3 | UI/UX 日本語化 (DotGothic16 + Fonts/Strings の日英自動切替) | ✅ |
| 6.5-4 | 素材収集・取り込み方針 ([docs/AssetGuidelines.md](../docs/AssetGuidelines.md), [LICENSES/](../LICENSES/INDEX.md)) | ✅ |
| 6.5-5 | コード再レビュー (A 評価、層越境を解消) | ✅ |
| 6.5-6 | システムまとめ ([docs/SystemSummary.md](../docs/SystemSummary.md)) | ✅ |

---

## これから (チーム議論のスタート地点)

> **状態更新 (2026-08-12)**: 以下 Phase 6〜8 は 5 月時点の計画。日付は経過済みで、
> Phase 8 の Phase A〜C の大半は E-1〜E-10 として実装済み。
> 現時点で優先すべき残作業は本ファイル末尾の残課題表を参照すること。

### Phase 6: 提出準備 (当初 5/16-5/17 予定、未完了のまま残存)

- [ ] mac/Linux でも動くか確認 (CI で代用可) — レビュー: OS マトリクス CI は未整備
- [ ] デモシナリオの試走 (3〜5 分) — レビュー: 実時間の計測記録なし
- [ ] スクリーンショット撮影 → README 更新
- [ ] LICENSE ファイル決定 + リポジトリに配置 — レビュー: ルート LICENSE 不在を確認
- [ ] 使用素材のクレジット ([LICENSES/INDEX.md](../LICENSES/INDEX.md) 経由) を CreditsScreen か README で表示 — レビュー: `CreditsScreen` は BGM/SE を「未投入」と表示するが ogg 20 件が同梱済

### Phase 7: チーム議論で決める論点 (MVP 完成後のキックオフ)

[ai_log/phase_6_5_review.md](./ai_log/phase_6_5_review.md) のチームメイト視点レビューから抽出。チーム会議で叩いて決める。

1. **Doko-demo (Android 対応)** — スローガンの核なのに MVP は Desktop のみ。誰がいつ Phase D (Android backend) に取り組むか
2. **キーマッピング** — 現状 WASD/矢印 + 1〜4。hjkl 派が居るか / 8 方向移動 (テンキー対角線) を入れるか
3. **スキル装着数** — 4 枠あるのに MVP は 2 個しか使ってない。Phase A-2 (スキル追加) を本番までに進める
4. **クリア条件** — 階段踏破クリアにしたので、敵を避けて直行が最適戦略になりうる。敵配置 ([InitialStateFactory.firstFloor](../src/main/java/core/infrastructure/bootstrap/InitialStateFactory.java)) を再検討
5. **Java 25 機能のハードル** — `sealed` / `record` / switch pattern matching を初学者がどこまで触る前提か。「拡張する人は `Skill` を 1 個追加するだけ」で済むか

### Phase 8: 拡張ロードマップ (Phase A〜D)

[docs/SystemSummary.md §8](../docs/SystemSummary.md) の Phase A〜D を参照。各人の興味で取り合う。

- **Phase A (コンテンツ拡張)**: 敵種別追加 / スキル追加 / 2 階層目 / HP 低下警告演出
- **Phase B (動的化)**: マップ生成 / 敵配置生成 / アイテム生成
- **Phase C (メタ進行 UI)**: 編成画面 / 永続セーブ / 装備システム
- **Phase D (マルチプラットフォーム)**: Android backend / タッチ入力

優先度は [docs/RolesDivision.md](../docs/RolesDivision.md) のラベル (P0/P1/P2) を流用する。

---

## Phase 9: M1.5 並列実装プラン (E-1〜E-10) — ✅ 実装完了

> **状態更新 (2026-08-12)**: 本セクションは 2026-05-14 起算の計画として書かれたが、E-1〜E-10 は全て実装済み。
> 以下の「未起票」「P2 (捨てる候補)」等の表記は**計画当時の記録**であり現状ではない。
> 現在の残作業は本ファイル末尾の「2026-08-12 総合・敵対的作品レビュー」節を参照すること。

[docs/GAME_DESIGN.md §15](../docs/GAME_DESIGN.md) の MVP 後仕様を、AI 駆動 + ファイル衝突回避のパッケージ分離で並列実装した。

### 実装結果 (2026-08-12 実測)

全 10 機能とも対応パッケージが実在する。E-7 は計画時「捨てる候補」だったが実装された。

| # | 機能 | パッケージ | 状態 |
|---|---|---|---|
| E-1 | カードシステム ([§15-3](../docs/GAME_DESIGN.md)) | `core.domain.card/` + `core.domain.meta/Gold.java` | ✅ カード 59 種 |
| A 依存 | ActionPoints 使い切り型 (§15-3) | `core.domain.battle/` | ✅ |
| B 依存 | Stats 6 ステ化 (§15-4) | `core.domain.entity/` | ✅ |
| E-3 | 層構造 + ノード分岐 ([§15-6](../docs/GAME_DESIGN.md)) | `core.domain.layer/` | ✅ |
| E-4 | 通貨二層 ([§15-2](../docs/GAME_DESIGN.md)) | `core.domain.meta/` | ✅ |
| E-6 | ポップアップ UI 基盤 ([§15-1, §15-8](../docs/GAME_DESIGN.md)) | `core.presentation.window/` | ✅ 5 クラス |
| E-2 | ソウルツリー ([§15-7](../docs/GAME_DESIGN.md)) | `core.domain.tree/` + `SoulTreeScreen` | ✅ 25 ノード |
| E-5 | 装備システム ([§15-9](../docs/GAME_DESIGN.md)) | `core.domain.equipment/` | ✅ 装備 20 件 |
| E-9 | セーブ最小 ([§15-11](../docs/GAME_DESIGN.md)) | `core.infrastructure.save/` | ✅ 8 クラス |
| E-8 | シームレス戦闘演出 ([§15-5](../docs/GAME_DESIGN.md)) | `core.presentation.effect/` | ✅ `DamagePopup` / `LowHpWarning` |
| E-10 | チュートリアル ([§15-10](../docs/GAME_DESIGN.md)) | `core.presentation.window/TutorialOverlay` | ✅ |
| E-7 | Bestiary ([§15-5](../docs/GAME_DESIGN.md) 連動) | `core.domain.meta/Bestiary.java` | ✅ 計画変更して実装 |

> 実装済みであることと**正しく動くこと**は別問題。E-9 (セーブ) と E-5 (装備) には
> レビューで P0 の欠陥が見つかっている (残課題表を参照)。

### 各 E-X の進め方 (`.claude/skills/m1-5-start/SKILL.md` 準拠)

1. `/m1-5-start E-X 機能名` で Issue 起票 + `feat/#<N>/E-X-skeleton` ブランチ作成
2. `domain-architect` Agent で設計レビュー → ユーザー確認
3. 実装 (Edit/Write) → hook が自動で `spotlessApply` + `FooTest` 実行
4. `test-writer` Agent でテスト補強
5. (LibGDX 依存がある場合) `libgdx-implementer` Agent で `presentation/` 実装
6. `/architect-review` Skill で `final-architect` の最終チェック
7. `/japanese-pr-create draft` Skill で Draft PR 作成
8. チームレビュー → マージ

### MVP コードとの breaking change リスト (§15 実装で書き換え必須) — ✅ 適用済 (履歴)

- `core.domain.battle.ActionPoints`: 蓄積型 → 使い切り型 (毎ターン速度ぶん全リセット)
- `core.domain.entity.Stats`: 3 ステ → 6 ステ (物攻/魔攻/物防/魔防 追加)
- `core.domain.skill.SkillSlot`: スキル枠単独 → スキル枠 + カードデッキ併存
- `core.domain.battle.TurnEngine`: 単敵想定 → 多敵 + AP 使い切り型
- `core.presentation.render.RenderLayout`: 800×600 → 1920×1080 (座標を比率化)
- `core.presentation.screen.*`: 画面遷移型 → ポップアップ式 UI

### 提出チェックリスト (M2)

- [ ] [Schedule.md M1.5 チェックリスト](../docs/Schedule.md) 全項目 ✅
- [ ] mac/Linux でも `gradlew run` が動作
- [ ] `fatJar` で配布 JAR 生成可
- [ ] §15-12 のデモシナリオで 5 分プレイ実演が成立
- [ ] スクリーンショット撮影 → README 更新
- [ ] LICENSE ファイル決定 + 配置
- [ ] 使用素材のクレジット (CreditsScreen or README)

---

## Issue 化のタイミング

- 上の Phase 6/7/8 を実際に取り掛かるときに GitHub Issue を立てる
- ブランチ命名は [BranchingStrategy.md §2](../docs/BranchingStrategy.md) の `feat/#<issue番号>` 形式
- 完了したら本ファイルの該当行に `✅ #N` を付ける

---

## 2026-08-12 総合・敵対的作品レビュー

- [x] 企画、実装、資産、テスト、配布構成を横断確認した。
- [x] プロダクト、ビジュアル、敵対的検証、コンプライアンスの独立レビューを統合した。
- [x] `gradlew check`と`fatJar`で基準線を検証した。
- [x] 敵対的レビューの画像誤読を再検証し、不一致件数を43／59枚へ訂正した。
- [x] 優先度、完了条件、依存関係、三つの改善方針を文書化した。

### 残課題

#### 意思決定（2026-08-12 採択済み）

| 項目 | 完了条件 | 優先度 | 依存 |
|---|---|---|---|
| Desktop向け戦術ローグライトを現行版の主役にする | Androidと端末間同期を現行訴求から外し、Java 25、DDD、反復プレイへ説明を揃える | 採択済み | PLATFORM-01 |
| 未保護の持込装備を死亡時に失い、深度上限内でSoul化する | 初期短剣、最大2保護枠、未確定品の死亡消失を保存と画面へ一致させる | 採択済み | EQUIP、SOUL、SETTLE |
| NPCdotcom制作フレーム、art-only画像、動的テキストを合成する | 300×420の1枚スパイクを承認後、59枚を段階移行する | 採択済み | CARD、ART、人間ゲートG0〜G5 |

#### Gate 0: 表示・保存の整合性 (実装タスク)

| 項目 | 完了条件 | 優先度 | 依存 |
|---|---|---|---|
| Profile と RunCheckpoint を分離する | 新規開始 → 層末保存 → 死亡/クリア → 再起動の一連で進捗が残り、終了済みランが「つづき」に出ず、旧形式が移行できる | P0 | SAVE-01〜03 |
| 装備所有モデルを追加する | 新規Profileは初期短剣のみ装着可、ラン中購入品はクリア後だけ所有化され、再起動後も保持 | P0 | EQUIP-01〜03 |
| カード表示を `cards.json` から実行時描画する | 全59枚の描画値と実データが一致し、自動照合テストがドリフトを検知する | P0 | CARD-01〜05 |
| 素材台帳とCreditsを完成する | 配布全資産の作者、URL、ライセンス、AI関与、加工、ハッシュが台帳化され、配布物から辿れる | P0 | ASSET、AUDIO、CREDIT |
| 主要訴求を実装へ合わせる | README、ウィンドウ名、JAR名、設計文書、デモ脚本がDesktop版の実装と一致する | P0 | PLATFORM-01 |
| SPACE の説明を 3 箇所直す | `Strings.java:175` / `:391` / `HudRenderer.java:168` が実装と一致し、入力契約テストが退行を検知する | P1 | なし (即着手可) |
| 無効な SlotExpand ノード 5 件を処置する | 全購入ノードが見える効果を持つか、返金される | P1 | Profile 移行 |

#### Gate 1: ゲーム性の計測と是正

| 項目 | 完了条件 | 優先度 | 依存 |
|---|---|---|---|
| 報酬抽選を `application` 層へ移設し解放・レアリティを反映する | 固定シードで未解放カードが出ず、採択した重みが `src/test/java/core/application/` から LibGDX なしで検証できる | P1 | Profile 修正 |
| 30 ラン + 初見 5 人の検証を実施する | 3〜5 分、非操作時間、階段ルート、初手選択の実測値がレビューへ追記される | P1 | 計測可能なビルド |
| 重複カード 7 組を統合または差別化する | 各カードに少なくとも一つ、プレイ判断を変える固有差がある | P1 | カード動的表示 |

#### Gate 2: 選んだ訴求の証明

| 項目 | 完了条件 | 優先度 | 依存 |
|---|---|---|---|
| OS マトリクス CI を追加する | 掲載する全 Desktop OS で build / test / packaging が通る | P1 | 対応 OS の明記 |
| clean clone からの再現性を確保する | フォント等の手動追加なしで build → test → package → launch が成功する | P1 | 素材台帳 |
| Android構想を将来候補として分離する | 現行ロードマップと受入条件から外し、再採択時だけ独立ADRと実機計画を作る | 対象外 | 現スプリントでは実装しない |

### レビュー

- 成果物: [総合・敵対的作品レビュー](../docs/20260812_comprehensive_adversarial_review.md)
- 機械検証: `gradlew clean check fatJar` 成功、73 スイート、761 テスト、失敗 0。
- 実装変更: なし。

### 検証パス (2026-08-12)

レビュー成果物を一次情報として扱わず、主張を実コード・データ・ビルド成果物・画像へ独立照合した。詳細は成果物の §18。

- [x] 機械的に検証可能な数値を全件照合した (テスト数、行数、資産数、画像寸法、重複カード数など) — **全件一致**。
- [x] P0-1〜P0-5 / P1-1〜P1-6 / 7.1〜7.9 の根拠行を実ファイルで確認した — **全件実在**。
- [x] カード画像 59 枚を独立に再目視した — 名称 7 / AP 36 / レアリティ 27 / 不一致 43 / 完全一致 16 が**全て再現**。
- [x] レビューの弱点 3 点を訂正した (7.7 は意図的決定、P1-1 は 3 箇所の自己矛盾、7.4 の todo.md 論拠)。
- [x] レビューが見落とした指摘 2 件を追加した (P1-7 層配置、`card_42`/`card_57` の画像内レアリティ矛盾)。
- [x] 本ファイルの陳腐化 (Phase 6 の 5 月日程、Phase 9 の「未起票」表記) を解消した。

**結論**: レビューの品質は高く、結論 (Request Changes、整合性スプリント優先) を変更する誤りは無かった。訂正は論拠の精度と重大度の位置付けに限られる。

---

## 2026-08-12 整合性スプリント開発計画

詳細な規則、対象ファイル、検証コマンド、承認ゲートは[実装計画](../docs/20260812_development_task_plan.md)を正とする。

### 採択済みの規則

- 現行配布対象はJava 25のDesktop版とし、Androidと端末間セーブ同期は対象外と明記する。
- ProfileとRunCheckpointを分離し、死亡またはクリア後の古いランを再開させない。
- 新規Profileはぼろい短剣だけを所有し、未所有装備を装着させない。
- ラン中購入品はクリア時に所有化し、死亡時はSoulへ変換せず失う。
- 持込装備は死亡時に未保護品だけ失い、踏破深度の上限内でSoulへ変換する。
- 装備保護枠は初期0、ソウルツリーで最大2とする。解放は「魂の刻印 I」30 Soul、「魂の刻印 II」60 Soul、ツリーリセット時も維持し90 Soulを返金しない。
- 1ラン収入とノード価格を同時に調整する。敵由来Soulは基本3層全撃破で12、祠は+3、ツリー全解放は300 ± 30 Soulとする ([GAME_DESIGN.md](../docs/GAME_DESIGN.md) の「ノードコストと整合させる」条件)。
- カードはNPCdotcom制作の固定フレーム、art-only画像、動的テキストを合成する。
- OtoLogic由来15音源は証跡を整えて保持し、証明できない5音源は置換する。

### 実装バックログ

| ID | 項目 | 完了条件 | 優先度 | 依存 | 状態 |
|---|---|---|---|---|---|
| BASE-01 | 退行試験と基準線 | 保存、所有、カード資産の失敗試験と現行check結果を記録 | P0 | なし | 未着手 |
| EQUIP-00 | 装備ドメイン契約 | 所有、持込、未確定、保護容量0〜2の不変条件を固定 | P0 | BASE-01 | 未着手 |
| SAVE-01 | Profile／Checkpoint保存器 | 分離保存、原子的置換、失敗通知、未来schema拒否 | P0 | EQUIP-00 | 未着手 |
| SAVE-02A | 旧save写像 | v1〜v3のSoulと装備を決定的に写像し、旧SlotExpandを返金 | P0 | SAVE-01 | 未着手 |
| SAVE-02B | 移行journal | 各書込間の障害後に不足ファイルだけを復旧 | P0 | SAVE-02A | 未着手 |
| SAVE-03A | Checkpoint変換 | RunSessionとCheckpointのrunId・一時所持品を往復 | P0 | SAVE-02B | 未着手 |
| SAVE-03B | タイトル続行 | 有効checkpointだけを続行し、暗黙上書きを拒否 | P0 | SAVE-03A | 未着手 |
| EQUIP-01 | 所有モデル | 初期短剣のみ所有、loadoutと保護指定が所有集合内 | P0 | SAVE-03B | 未着手 |
| EQUIP-02 | 装備画面 | 所有装備だけ表示・装着、最大2枠の保護指定 | P0 | EQUIP-01, SOUL-03 | 未着手 |
| EQUIP-03 | RunInventory | 持込、未確定、現在装備を分離し買替えで消失しない | P0 | EQUIP-02 | 未着手 |
| SOUL-01 | Soul報酬是正 | 基本3層全撃破12 Soul、祠+3 | P1 | SAVE-03B | 未着手 |
| SOUL-02 | 保護ノード | 旧5ノード廃止、新30／60 Soulノードを追加 | P1 | SAVE-02B, SOUL-01 | 未着手 |
| SOUL-03 | 容量とツリーUI | 解放効果0〜2、リセット後も刻印維持 (非返金90)、刻印アイコンが実在パスを返す | P1 | SOUL-02 | 未着手 |
| SOUL-04 | ノード価格再スケール | ツリー総額300±30 Soul、最安≦3、LayerExtend再設定 | **P0** | SOUL-02 | 未着手 |
| SETTLE-01 | 純粋精算 | 死亡、クリア、再精算の規則を単体試験 | P0 | EQUIP-03, SOUL-03 | 未着手 |
| SETTLE-02 | 終了時永続化 | Profile保存後だけcheckpoint削除、二重精算0 | P0 | SETTLE-01 | 未着手 |
| REWARD-01 | カード報酬抽選 | application層で解放・レアリティ・固定seedを検証 | P1 | EQUIP-01, CARD-01 | 未着手 |
| INPUT-01 | SPACE説明 | 日本語／英語の3表示と入力契約が一致 | P1 | BASE-01 | 未着手 |
| CARD-01 | レアリティ正規化 | 59カードすべてに承認済みrarityを明示 | P0 | BASE-01, G3 | 待機 |
| CARD-02 | 1枚表示スパイク | 300×420フレーム＋zangeki artを新経路で表示 | P0 | BASE-01, G0, G1 | 待機 |
| CARD-03 | 動的手札表示 | AP、名称、rarity、要約を動的描画し全文を詳細表示 | P0 | CARD-01, CARD-02 | 未着手 |
| CARD-04 | 他画面統合 | 図鑑とSoulTreeが同じ表示モデルを利用 | P1 | CARD-03, G2 | 未着手 |
| ART-01〜15 | 58枚移行 | 各4枚以下、来歴・接触シート承認済み | P1 | CARD-04, ASSET-01, G4 | 待機 |
| CARD-05 | 旧画像撤去 | art＝data＝provenance 59件、legacy参照0 | P1 | ART-01〜15, G5 | 待機 |
| CARD-06 | 重複カード解消 | 重複7組が固有差を持つか統合され、一意性テストが通る | P1 | CARD-03 | 未着手 |
| PLATFORM-01 | Desktop表明 | Android／同期の未実装表明を除去 | P0 | BASE-01 | 未着手 |
| ASSET-01 | 来歴manifest | 全PNG／OGG／TTFに必須項目とhashがある | P0 | PLATFORM-01 | 未着手 |
| DEPENDENCY-01 | 依存ライセンス | runtime全artifactの版・ライセンス・NOTICEを記録 | P0 | ASSET-01 | 未着手 |
| AUDIO-01 | OtoLogic確定 | 15音源を公式名・URL・CC BY・改変と対応 | P0 | ASSET-01 | 未着手 |
| AUDIO-02 | 5音源置換 | 旧hash消滅、代替の再配布条件と台帳を承認 | P0 | AUDIO-01, G6 | 待機 |
| HISTORY-01 | 過去配布経路 | 履歴・タグ・Release・CI成果物の要判断対象を記録 | P0 | AUDIO-02, G8 | 待機 |
| CREDIT-01 | Credits／通知 | ゲーム表示、README、JAR通知が台帳と一致 | P0 | DEPENDENCY-01, HISTORY-01, G7 | 待機 |
| RELEASE-01 | releaseCheck／CI | Win・Ubuntu・macOSで配布契約を自動検証 | P1 | CREDIT-01 | 未着手 |
| QA-01 | 自動総合検証 | clean releaseCheck成功、敵対シナリオ全通過 | P0 | 全実装 | 未着手 |
| QA-02 | 実機・経済計測 | 30seed、Soul/分、死亡損失、2解像度可読性を記録 | P1 | QA-01 | 未着手 |

### 最初の開発バッチ (直列実行)

| 順 | 項目 | 完了条件 | 優先度 | 依存 |
|---|---|---|---|---|
| 1 | BASE-01 | `check`と`fatJar`が成功し基準線を記録。**失敗する試験を残さない** | P0 | なし |
| 2 | EQUIP-00 | 保存schemaが参照する装備所有とRunInventoryの型を固定する | P0 | BASE-01 |
| 3 | PLATFORM-01 | Android／端末間同期の未実装表明を除去する。**プロジェクト名とJAR名は変更しない** | P0 | BASE-01 |
| 4 | INPUT-01 | キー割当を純粋な対応表へ抽出し、表示3箇所を実装へ揃える | P1 | BASE-01 |

`SAVE-01`は保存schemaの中核で差分が大きいため第2バッチの先頭へ移した。

PLATFORM-01とINPUT-01は`Strings.java`を共有するため並列にしない。

### 第1バッチの実行契約

- **BASE-01は緑の確認のみ**。回帰試験は各実装タスク内でred → greenとして書く。基準線タスクが恒常的な赤を残すと、以後「既知の赤」と「新しい退行」を区別できなくなる。
- **ドメイン型は`java.*`のみに依存**し、Jacksonは保存層に限定する。現状`core/domain/`のJackson importは0件で、`SaveDataConverter`が変換を担う既存パターンがある。
- **新名称が決まるまでプロジェクト名・JAR名は変更しない**。
- **Soul価格は30／60で全文書統一**。リセット非返金は90。計画書・本ファイル・`docs/GAME_DESIGN.md`を同値に保つ。

### 第1バッチ実行ログ

作業領域: `feat/consistency-sprint-batch1` (`docs/consistency-sprint-plan` の `cec61b4` から分岐)

#### BASE-01 ✅ 完了 (2026-08-12)

```
gradlew --no-daemon check fatJar  →  BUILD SUCCESSFUL (exit 0)
```

| 指標 | 実測値 |
|---|---|
| テストスイート | 73 |
| テスト件数 | 761 |
| 失敗 / エラー / スキップ | 0 / 0 / 0 |
| fat JAR | 33,459,735 bytes (31.91 MiB) |
| 追跡バイナリ資産 | 99 件 |

失敗する試験は残していない。以後の退行はこの基準線との差分で判定する。

**基準線に関する注意 (P0-5 の実証)**: 本 worktree の fat JAR は 31.91 MiB で、メインリポジトリの作業コピーでビルドした JAR (33 MiB) より約 1.1 MiB 小さい。原因は `assets/fonts/DotGothic16-Regular.ttf` (2,069,236 bytes) の有無で、`.gitignore:130` が `assets/fonts/*.ttf` を除外するため clean checkout には含まれない。

- レビュー P0-5 の「DotGothic16 はローカルに存在すると JAR へ入る一方、clean clone の成果物が再現しない」が実測で確認された。
- **本バッチ以降の JAR 基準は 31.91 MiB (フォント無し)** とする。フォント同梱時と混同しない。
- 日本語表示を伴う実機確認を行う場合は `assets/fonts/README.md` の手順でフォントを取得してから実施する。
- 恒久対応は ASSET-01 と RELEASE-01 (clean clone からの再現性) の担当範囲であり、BASE-01 では記録に留める。

#### EQUIP-00 ✅ 完了 (2026-08-12)

`core.domain.equipment` に 3 record を追加。`java.*` のみ依存、Jackson なし。

- `RetentionCapacity`: 保護枠 0〜2 を compact constructor で強制
- `EquipmentOwnership`: 恒久側。初期短剣を常に所有へ補い保護対象から除外。保護 ID ⊆ 所有 ID かつ容量以下
- `RunInventory`: ラン側。持込品と未確定品を分離し重複取得を禁止。保護は持込品限定、現在装備 ∈ 持込 ∪ 未確定。`unprotectedCarriedIn()` が死亡時の喪失対象を返す

未確定品を Soul へ変換しない規則を型の段階で表現し、金貨 → Soul の交換経路を塞いだ。テストは red → green。

#### PLATFORM-01 ✅ 完了 (2026-08-12)

Android / 端末間同期の未実装表明を現行文書とコードから除去。README・GAME_DESIGN (§1-2 / §1-3 / §2-1 / §15-12)・ProjectOverview・SystemSummary・FeatureCatalog、`DesktopLauncher` のウィンドウ名、`Strings` の日英 SUBTITLE と英語 TITLE。

- **プロジェクト名 (`rootProject.name`) と JAR 名は据え置き** (新名称の決定待ち)。
- `PlatformClaimTest` で再混入を検知する。履歴資料 (`docs/20260812_*`, `tasks/ai_log/`) と本テスト自身は走査対象外。
- §15-12 のデモ脚本を Desktop 単画面へ差し替え、装備喪失の見せ場を追加。「1 ラン収入でノードを 1 つ解放」が成立する前提を SOUL-04 の受入条件 (最安 ≦ 3 Soul) と結び付けた。

#### INPUT-01 ✅ 完了 (2026-08-12)

`core.presentation.input.InputAction` (LibGDX 非依存の enum) を新設し、SPACE = 待機 / ENTER = ターン終了 の割当を単一ソース化。

- 誤記 3 箇所を修正: `Strings.Ja.TUTORIAL_BODY`、`Strings.En.TUTORIAL_BODY`、`HudRenderer` のコメント。
- 元から正しかった `CONTROLS_WAIT` / `CONTROLS_END` は壊していない (テストで固定)。
- チュートリアルから SPACE を消すのではなく【待機】節を新設し、操作の説明自体は残した。
- `PlayerInputs` は `InputAction.keyCode()` を参照する。`Gdx` 静的参照を持つため直接駆動せず、`InputHelpTextTest` が純粋クラスのみで表示文と割当を突き合わせる (A6 の設計)。

#### 第1バッチ完了時の実測

| 指標 | 基準線 | 完了時 |
|---|---|---|
| スイート | 73 | 77 |
| テスト | 761 | **803** (+42) |
| 失敗 / エラー | 0 / 0 | **0 / 0** |
| `gradlew check` | 成功 | **成功** |

回帰ゼロ。次バッチは SAVE-01 (Profile / RunCheckpoint の保存器) から。

### レビュー

- 独立レビューと敵対的レビューのP0/P1指摘を計画へ反映済み。
- 計画に対する敵対的検証でA1〜A6を検出し、SOUL-04とCARD-06の新設、QA-02の基準追加、INPUT-01の再設計、BASE-01の再定義として反映済み。
- 計画は `docs/consistency-sprint-plan` ブランチの `cec61b4` へコミット済み。`main`へは直接コミットしていない。
- 実装はTDDで進め、2〜3タスクごとに構造レビューと敵対的レビューを行う。
