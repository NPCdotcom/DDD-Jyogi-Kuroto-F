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

### Phase 6: 提出準備 (5/16 土 - 5/17 日)

- [ ] mac/Linux でも動くか確認 (CI で代用可)
- [ ] デモシナリオの試走 (3〜5 分)
- [ ] スクリーンショット撮影 → README 更新
- [ ] LICENSE ファイル決定 + リポジトリに配置
- [ ] 使用素材のクレジット ([LICENSES/INDEX.md](../LICENSES/INDEX.md) 経由) を CreditsScreen か README で表示

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

## Phase 9: M1.5 並列実装プラン (E-1〜E-10) — 2026-05-14 起算 約 10 日

[docs/GAME_DESIGN.md §15](../docs/GAME_DESIGN.md) の MVP 後仕様を、AI 駆動 + ファイル衝突回避のパッケージ分離で並列実装する。

### 着手前提

- [x] MVP コードと §15 仕様 docs が develop に統合済 (commit `4a9382f`)
- [ ] AI 駆動体制整備 (`.claude/agents/`, `.claude/skills/`, `.claude/settings.json`, ルート `CLAUDE.md`) — Issue #10 / PR 作成中
- [ ] チームメイトへの共有メッセージ送信 (mvp ブランチ + PR #9 マージ済 + AI 体制 PR の 3 点セット)

### スコープ (再ラベル: チームメイト視点レビューを反映)

P0' = デモ必須、P1 = タイムボックス各 1〜2 日、P2 = 余力次第。

| # | 機能 | パッケージ | 優先度 | 担当目安 |
|---|---|---|---|---|
| **E-1** | カードシステム ([§15-3](../docs/GAME_DESIGN.md)) | `core.domain.card/` + `core.domain.meta/Gold.java` | **P0'** | domain-architect → libgdx-implementer |
| **E-3** | 層構造 + ノード分岐 ([§15-6](../docs/GAME_DESIGN.md)) | `core.domain.layer/` | **P0'** | domain-architect |
| **E-4** | 通貨二層 ([§15-2](../docs/GAME_DESIGN.md)) | `core.domain.meta/Gold.java` (E-1 と相乗り) | **P0'** | domain-architect |
| **E-6** | ポップアップ UI 基盤 ([§15-1, §15-8](../docs/GAME_DESIGN.md)) | `core.presentation.window/` | **P0'** | libgdx-implementer |
| **E-2** | ソウルツリー ([§15-7](../docs/GAME_DESIGN.md)) | `core.domain.tree/` + `core.presentation.screen/SoulTreeScreen` | **P0'** | domain-architect → libgdx-implementer |
| E-5 | 装備システム ([§15-9](../docs/GAME_DESIGN.md)) | `core.domain.equipment/` | P1 | domain-architect |
| E-9 | セーブ最小 ([§15-11](../docs/GAME_DESIGN.md)) | `core.infrastructure.save/` | P1 | libgdx-implementer |
| E-8 | シームレス戦闘演出 ([§15-5](../docs/GAME_DESIGN.md)) | `core.presentation.effect/` | P1 | libgdx-implementer |
| E-10 | チュートリアル ([§15-10](../docs/GAME_DESIGN.md)) | `core.presentation.screen/TutorialPopup` | P1 | libgdx-implementer |
| E-7 | Bestiary ([§15-5](../docs/GAME_DESIGN.md) 連動) | `core.domain.meta/Bestiary.java` | **P2 (捨てる候補)** | — |

### タイムライン

| 日付 | 着手 | 並列の組み合わせ |
|---|---|---|
| **5/14-15** | E-1 / E-3 / E-4 / E-6 (基盤) | パッケージが分離されているため並列着手可。AP 使い切り化 / ステ 6 種化が `core.domain.battle` と `core.domain.entity` に影響するため、まずそこを domain-architect が更新 |
| **5/16-18** | E-2 / E-5 / E-9 (派生) | E-1 (カード) と E-2 (ソウルツリーのカード解放) が依存。E-9 は最小実装 (1 セーブスロット、層単位) |
| **5/19-21** | E-8 / E-10 / 余力で E-7 | E-1〜E-6 の動作が見えてから演出層を作る方が手戻り少 |
| **5/22** | 統合テスト + デモ録画 | §15-12 のデモシナリオを実機で 1 本撮る |
| **5/23-24** | ハッカソン本番 | 提出 + ライブデモ |

### 各 E-X の進め方 (`.claude/skills/m1-5-start/SKILL.md` 準拠)

1. `/m1-5-start E-X 機能名` で Issue 起票 + `feat/#<N>/E-X-skeleton` ブランチ作成
2. `domain-architect` Agent で設計レビュー → ユーザー確認
3. 実装 (Edit/Write) → hook が自動で `spotlessApply` + `FooTest` 実行
4. `test-writer` Agent でテスト補強
5. (LibGDX 依存がある場合) `libgdx-implementer` Agent で `presentation/` 実装
6. `/architect-review` Skill で `final-architect` の最終チェック
7. `/japanese-pr-create draft` Skill で Draft PR 作成
8. チームレビュー → マージ

### MVP コードとの breaking change リスト (§15 実装で書き換え必須)

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
