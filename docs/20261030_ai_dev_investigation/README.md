# 2026-10-30 AI 開発体制の調査記録と C-1 品質ゲート仕様

AI (Claude Code / Codex / Orca) による開発を速く・安く・品質を落とさずに回すために、2026-10-30 に行った調査の記録。

- **位置付け**: 調査記録と、承認待ちの実装仕様。ゲーム仕様の Single Source of Truth は引き続き [GAME_DESIGN.md](../GAME_DESIGN.md)。
- **対象コード**: `main` @ `60ac172` (2026-08-15)。
- **実装の状況**: 本フォルダの提案 (C-1 など) は**どれも未実装**。承認事項 A1〜A7 は [report_phase2.md](./report_phase2.md) §7 にある。

## 読む順番

1. [report_phase2.md](./report_phase2.md): 最新のまとめ (現状、前回の採否、C-1 仕様の要点、役割分担、効果と未計測、Critic への対応、承認事項)
2. [c1_spec_v3.md](./c1_spec_v3.md): **C-1 品質ゲート復旧の実装仕様 (承認用)**
3. [review_reflection_v2.md](./review_reflection_v2.md): 前回調査の採用・修正・撤回、C-3 / C-4a / カード表示 / レアリティの検討、計測と費用
4. [model_roles_eval_plan.md](./model_roles_eval_plan.md): AI の役割分担と、モデル採用前の評価計画
5. [report_final.md](./report_final.md): フェーズ 1 の総合調査報告 (一部の数値は report_phase2 / review_reflection_v2 で訂正済み)

## ファイル一覧

| パス | 内容 |
|---|---|
| `report_final.md` | フェーズ 1 総合調査 (Orca Run `run_e05f7624b620`) |
| `report_phase2.md` | フェーズ 2 報告 (Orca Run `run_11ead774bf94`) |
| `c1_spec_v3.md` | C-1 実装仕様 v3 (独立 Critic v2 の指摘を反映) |
| `review_reflection_v2.md` | レビュー反映と計測 |
| `model_roles_eval_plan.md` | 役割分担と評価計画 |
| `critic_inputs_v2.md` | Critic v2 に渡した根拠の所在 |
| `hook_experiments/RESULTS.md` | 現行 Hook スクリプトの実測 (スタブ実験) |
| `workers/uiux_audit.md` | UI/UX 静的監査 (Codex Worker) |
| `workers/card_image_audit.md` | カード画像 60 枚と `cards.json` の照合 (Codex Worker) |
| `workers/critic_review.md` / `workers/critic_review_v2.md` | 独立 Critic のレビュー (フェーズ 1 / 2) |
| `scripts/` | 計測スクリプト (`metrics_v2.py` が最新。`analyze*.py` はフェーズ 1 で使ったもの) と価格表 |
| `data/` | 計測結果のテキスト、モデル候補の抽出結果 |
| `archive/` | 置き換えられた版 (フェーズ 1 のドラフト、C-1 仕様 v1・v2)。Critic の行番号付きの指摘を追跡するために残す |

## 表記

- ラベル: **[実測]** スクリプト・実験で取得 / **[確認済み]** コード・公式文書・git で確認 / **[前回引用]** 前回報告からの引用 / **[推測]** 未検証 / **[見積り]** 仮定に基づく概算 / **[設計]** 提案。
- 本文中のパス:
  - `{S}`: 調査時の作業ディレクトリ (= 本フォルダに相当)。ファイルの配置は、本フォルダでは `scripts/`・`data/`・`archive/` に整理し直している。本文中の参照は調査時の配置のまま。
  - `{S}\main` と `main:`: コミット `60ac172` のリポジトリルート (調査時は `git archive` したコピーを読んだ)。
  - `~`: 調査者のホームディレクトリ。
  - `<repo>`: ローカルのクローン。
- 公開リポジトリに置くため、ローカル環境の絶対パスはこれらの記号に置き換えた。それ以外の本文は、調査時点の内容のまま。

## 数値の扱い

- 「API 換算」は、使用量 × 公開単価 (2026-10-30 時点、`scripts/prices_anthropic_20261030.json`) で計算した値で、**請求額ではない**。
- 標本は主に 1 セッション (2026-08-12〜14) と本調査のセッション。一般化には注意すること。
- 価格・モデル一覧・公式文書の記述は 2026-10-30 時点のもので、変わりうる。

## 再現方法

```bash
python -I scripts/metrics_v2.py claude <session.jsonl> [<subagents_dir>] [--since ISO] [--until ISO]
python -I scripts/metrics_v2.py codex <rollout.jsonl> ...
```

セッション記録 (`~/.claude/projects/...`、`~/.codex/sessions/...`) は各自のローカル環境にあり、本リポジトリには含めない。

## 収録していないもの

- 調査用の main のコピー、カード画像の拡大コピー (素材の出所が未確認のため複製しない)
- OpenRouter のモデル一覧の生データ (約 770 KB。抽出結果は `data/model_candidates_20261030.txt`)
- Worker・Critic の一時ファイル、ローカルのセッション記録、Gradle デーモンのログ
