# フェーズ 2 報告: 調査レビューの反映と C-1 仕様の確定 (2026-10-30)

Orca Run `run_11ead774bf94` (前回 `run_e05f7624b620` の続き。前回 Run は流用せず、目的欄で関連付けた)。実装・commit・push・有料 API・モデル比較は 0 件。
ラベル: **[実測]** 今回スクリプト・実験で取得 / **[確認済み]** コード・公式文書・git で確認 / **[前回引用]** 前回レポートからの引用 / **[推測]** 未検証 / **[見積り]** 仮定に基づく

## 成果物 (すべて scratchpad 配下)

| ファイル | 内容 |
|---|---|
| `c1_spec_v3.md` | **承認用の C-1 実装仕様** (v2 に Critic v2 の 15 件を反映) |
| `review_reflection_v2.md` | 現状の再確認、前回レポートの採用・修正・撤回、C-3 / C-4a / カード / レアリティ、計測と費用 |
| `model_roles_eval_plan.md` | 役割分担、候補モデル、採用前の評価計画 |
| `hook_experiments/RESULTS.md` | 現行 Hook の実測 (スタブ実験) |
| `metrics_v2.py`、`prices_anthropic_20261030.json`、`metrics_v2_old_session.txt`、`metrics_v2_turn1_and_codex.txt` | 再現可能な計測 |
| `model_candidates_20261030.txt`、`dl_openrouter/models_v2.json` | モデル一覧の取得結果 |
| `workers/critic_review_v2.md` | 独立 Critic v2 の報告 |

## 1. 現行 main と作業ツリーの状態 [確認済み]

| 確認内容 | コマンド | 結果 |
|---|---|---|
| リモート main | `git ls-remote origin refs/heads/main` | `60ac17225507efb6201362f7314dcecfbe3c7db5` (fetch していない) |
| ローカル | `git rev-parse main origin/main HEAD` | すべて 60ac172。6 コミットは push 済み |
| 作業ツリー | `git rev-parse --abbrev-ref HEAD` / `git status --porcelain --untracked-files=all` | `main` / 0 件 |
| 未コミット文書 | `git stash list` / `git stash show --include-untracked --stat stash@{0}` | `stash@{0}` (On docs/consistency-sprint-plan, 21:28)、9 文書。この 9 パスはパス限定の比較で main と同一 (Critic v2 も blob 比較で 9/9 一致)。**一切触れていない** (作業前後とも `2a2c7a6a…`) |
| worktree | `git worktree list` | 2 件 (変更なし) |
| ツール | `claude --version` / `orca --version` | 2.1.314 / 1.4.244 |

## 2. 前回レポートの採用・修正・撤回 (詳細: review_reflection_v2.md §2)

- **撤回**: 「未 push 6 コミット」(解消済み)。「レアリティ 55/59 未定義」を欠陥として扱ったこと。これは意図されたデフォルトで、付与が未完了なだけ (`CardCatalog.java:123` に理由が明記)。本番での用途はドロー時の効果音の選択だけ。
- **修正**: 「$280」は API 換算であり、請求額ではない (請求情報にはアクセスできない)。「94%」は 1 セッションの API 換算額に占める比率。カード不一致数は 8/12 の監査と近いが、同一の再現とは扱わない。クロップ流用は技術的な候補にとどめる (出所不明のため、再配布可能とは判断しない)。C-4a は既存の再試行経路を前提に、表示と案内に限定する。
- **格上げ** (推測 → 実測): Stop Hook の終了コード握りつぶし、失敗時の不正 JSON、ガードの迂回。
- **追加** [実測/確認済み]: 変更のない応答で Gradle が約 19 秒 (単発デーモン、テストは UP-TO-DATE)。標準 Skill が `--no-daemon` を指示していた。罠による Elite 撃破で、敵ターン中にも報酬が開く。同時撃破では 2 体目の報酬が出ない (仕様は未決)。失敗時の結果画面のソウル表示が 0 になる。Gradle の test が .md を入力として宣言していない (文書だけの変更では `PlatformClaimTest` が再実行されない)。worktree はユーザーの stash を共有する。
- **自己批判**: 前回 Worker A の監査は、既存結果と重複していた。また Lead が Worker 報告の全文を読み込んでいた。

## 3. C-1 の実装仕様と検証方法 (詳細: c1_spec_v3.md)

- **二層防御**: 権限ルール (deny は bypass を含む全モードで有効) を一次防御、Hook の全文スキャンを補助にする。PreToolUse は `Bash|PowerShell`・timeout 15 秒・`onFailure: block`・exit 2。
- **Stop ゲート**:
  - リポジトリ全体の内容指紋 + 環境キー。有効な green と一致すれば Gradle を起動しない。
  - 起動した場合は `test --rerun` で実行し、検証の前後で指紋が一致した時だけ green にする。
  - 分類は COMPILE / TEST / BUILD / UNKNOWN (Claude へ exit 2)、ENV / TIMEOUT (ユーザーへ systemMessage)。
  - セッション単位の修正予算 (連続 3 回まで)、意図的 RED の 1 回限りのマーカー、強制再検証 (`CLAUDE_GATE_FORCE=1`) を用意する。
- **PostToolUse は削除**。整形は変更した Java ファイルに限定する。
- **検証は 4 層 + 計測**:
  - Hook 単体 U-S1〜S22 / U-G1〜G10 (Gradle 不要)
  - 権限エンジン E-1〜E-6 (偽の git/gh を使い、リモートに届かない)
  - 故障注入 F-1〜F-6 (`claude -p`、任意)
  - 実 Gradle での意図的失敗 R1〜R14 (コンパイル・テスト・JAVA_HOME 不正・timeout・ビルドスクリプト・依存・文書の禁止表明・clean 後を含む)
  - Daemon 計測 P-1〜P-4 (事前登録した判定規則)
  - 導入後の監視 M1〜M5
- **完了条件**: 高速化だけでは完了にしない。意図的失敗の検出を含め、8 項目すべてを満たすこと。

## 4. 役割分担 (詳細: model_roles_eval_plan.md)

- 決定的処理 (スクリプト): 計測・費用・指紋・Gradle 失敗の分類・破壊コマンドの判定。モデルの暗算を計測値として扱わない。
- Lead: 推論力の高い通常 LLM。サイクルごとに新しいセッションにし、Worker 報告は要約と該当箇所だけを読む。
- Implementer: 既定は Codex。独立クローンで作業し、サンドボックス付きで起動する (A5)。
- Critic: 実装担当と別系統。価格を理由に格下げしない。
- 軽量 Worker: 合否ゲートには使わない。候補は「レビュー指摘の分類」と「計測結果の文章化」だけで、評価 (E1〜E3、事前登録した 4 条件) に合格した場合に限る。
- 構造化出力特化モデル (`inference-net/schematron-v2-turbo`: 3B の HTML→JSON 抽出) と、通常 LLM の JSON Schema 出力 (構文は保証するが、意味は保証しない) は区別する。Jev 系 (`typesafe/jev-router`) は除外した。Clef 系は一覧に存在しない。

## 5. 期待できる効果と未計測の事項

| 項目 | 現状 (根拠) | 期待 (区分) |
|---|---|---|
| 変更のない応答の Stop | 19.0 秒 (n=1、2.1.314) / 中央値 18.5 秒 (n=31、8 月) [実測] | Gradle を起動しない [設計]。所要時間は導入後に実測 (M2) |
| 失敗の通知 | 起動失敗は成功扱い、要約 2 行以上は JSON 不正 [実測] | 全分類を通知 [設計]。U-S と R で検証 |
| ガード | 7 入力中 5 件が素通り [実測]。中央値 821 ms (n=10) [実測] | 権限ルール + 全文スキャン [設計]。E と U-G で検証 |
| Daemon の効果 | **未計測** | P-1〜P-4 で判定 |
| 変更のある応答の検証時間 (`test --rerun`) | **未計測** (761 テストの実行本体は 8 月に約 5 秒 [前回引用]) | P-3 で計測 |
| 整形差分の注入 | 8 月: 71 件・359,680 文字 [実測] | 変更ファイルに限定 [設計]。M4 で計測 |
| 費用 | 8 月 1 セッション API 換算 $256.24 + $23.99、失敗ループ窓 81 呼出・$21.01 [実測・定義付き]。本セッション 1 ターン目 $13.65、2 ターン目 (13:43Z 時点) $11.92 [実測]。Codex 4 本は単価非公開で換算不可、画面上の 5 時間枠の残りは 47% (22:30) [観測] | 削減額は**見積もらない** (導入後に同じスクリプトで比較) |

## 6. 独立 Critic の指摘と対応

- **Critic v1** (前回、Blocker 1・Major 7・Minor 4): 全件を採用済み [前回引用]。
- **Critic v2** (今回、Codex、25.1 分): Blocker 0・Major 10・Minor 5。**全件採用**。
  - 却下はなし。ただし判定表の「再読込指標を実課金と主張した」という反証 1 行は、もともと指標と明記していたため変更不要とした。
  - 主要な Major は、Lead が自分でも再現した: M01 の文書入力を `PlatformClaimTest.java` で確認、M04 の上限リセットを公式本文で確認、M08 の再計算で 81 呼出・$21.01 を再現。
- 対応表は `c1_spec_v3.md` §0。

## 7. 承認していただきたい変更範囲

| ID | 内容 | 推奨 |
|---|---|---|
| A1 | C-1 の実装 (`c1_spec_v3.md` §7 の C-1 列。専用ブランチと PR の下書きまで。push とマージは別途) | 承認を推奨 |
| A2 | 権限ルール (deny 8 行・ask 14 行) の各行 | 行ごとに判断。非破壊操作にも確認が出る行 (`git restore *`、`PowerShell(Remove-Item *)`) は狭めることも可 |
| A3 | 使い捨てクローンでの実 Gradle (約 40 回、推定 20〜30 分、外部送信なし) | C-1 の完了条件に必須 |
| A4 | `claude -p` による故障注入と実機の契約確認 (少量の API 使用) | 任意。しない場合は「未検証」として残す |
| A5 | 実装 Worker の Codex をサンドボックス付きで起動する設定 | 推奨 (YOLO では stash を守れない) |
| A6 | stash@{0} のバックアップ ref の作成 | 任意 (内容は main と同一なので、必要性は低い) |
| A7 | C-1b (`build.gradle`: spotless の `ratchetFrom`、test の文書入力宣言) | C-1 とは別に判断 |
| — | C-3 以降の実装、モデル比較の実行 (評価計画 §4.6) | 今回は対象外 |

## 付記: Orca 運用で実測した問題

- `worker-start` では 4 回中 3 回が「30 秒以内にターン開始を観測できず」と返った。Critic を起動した 2 回は、どちらも指示文 (約 8,000 文字の貼り付け) が Codex の入力欄に残り、手動で Enter を送るまで止まっていた。
- 対策: spec はファイルに置き、短い指示でそのパスを渡す。`worker-start` の後に `terminal read` で `draft:` が残っていないかを確認する。
