# 開発者ガイド

DDD-Jyogi-Kuroto-F に貢献するための運用ガイド。

---

## 1. 開発環境セットアップ

### 1-1. 必要なツール

| ツール | バージョン | 備考 |
|---|---|---|
| JDK | **Java 25 LTS (Oracle JDK 25)** | NFTC で無料利用可。教員資料の手順に準拠 |
| Git | 最新安定版 | — |
| IDE | IntelliJ IDEA Community 推奨 | Eclipse / VSCode でも可 |
| GitHub CLI (`gh`) | 任意 | ラベル一括適用などで使用 |

### 1-2. 初回 clone 手順

```bash
git clone https://github.com/NPCdotcom/DDD-Jyogi-Kuroto-F.git
cd DDD-Jyogi-Kuroto-F
java --version  # 25.x.x が出ること
```

`build.gradle` は MVP 着手時に追加される。それまではドキュメントとディレクトリ構造の確認のみ。

---

## 2. 作業フロー

### 2-1. Issue → ブランチ → PR

1. **Issue を立てる**: GitHub の Issue Template から `bug_report` / `feature_request` / `task` のいずれかを選ぶ
2. **ブランチを切る**: `feat/#<issue番号>` または `fix/#<issue番号>` (詳細は [BranchingStrategy.md](./BranchingStrategy.md))
3. **作業 → コミット → push**
4. **PR を作成**: PR Template に従って記入。動作確認チェックリストを埋めること
5. **CI 全緑 + 人間レビュー1名以上 approve でマージ**

### 2-2. コミットメッセージ

- フォーマット: `<type>: <summary> (#<issue番号>)`
- type: `feat` / `fix` / `docs` / `chore` / `refactor` / `test`
- 例: `feat: AP制戦闘の基盤を追加 (#12)`

---

## 3. レビュー

- AI レビュー (Claude / Rabbit) の使用可。むしろ推奨
- **ただし人間レビュー1名以上が必須**
- AI レビュー結果を PR コメントに貼ると他メンバーの理解が早い

---

## 4. CI 失敗時の見方

CI は以下のジョブで構成 (詳細は `.github/workflows/ci.yml`)。

| ジョブ | 失敗時の対処 |
|---|---|
| `check-branch-name` | ブランチ名が `feat/#\d+` / `fix/#\d+` / `docs/#\d+` / `chore/#\d+` / `develop` に合致していない。リネームして再 push |

`build.gradle` が追加されたら Gradle build / Spotless / JUnit ジョブも有効化される（コメント済み）。

---

## 5. ラベル運用

`.github/labels.yml` で定義されたラベルを `gh` で一括適用する想定。

### 一括適用コマンド (リポジトリオーナーが初回のみ実行)

```bash
# yq + gh が必要
yq -r '.[] | "\(.name)|\(.color)|\(.description // "")"' .github/labels.yml | \
  while IFS='|' read name color desc; do
    gh label create "$name" --color "$color" --description "$desc" --force
  done
```

### ラベル種別（最小構成）

- `type/*`: `type/bug` / `type/task`（機能追加もここに含む）
- `priority/*`: `P0`（MVP必須）/ `P1`（MVP後すぐ）/ `P2`（余力次第）

機能カテゴリ（A: コア / B: 動的生成 / C: UI演出 / D: 配布デモ）は [RolesDivision.md](./RolesDivision.md) の番号（A-1, B-2 等）でタイトルや本文に明示する運用。

---

## 6. LICENSE の方針

- 現時点では LICENSE ファイルを置いていない
- 素材権利関係 (AI 生成画像、効果音など) が未確定のため
- **ハッカソン提出直前にチームで合意のうえ追加する**
- 候補: Apache-2.0 / MIT (コードのみ) + 素材は別ライセンス明記

---

## 7. HackMD と docs/ の使い分け

| 用途 | 配置先 |
|---|---|
| 議論ドラフト・話し合い経緯 | HackMD |
| 確定事項・仕様・規約 | `docs/` (本リポジトリ) |

HackMD で固まったものを PR で `docs/` に反映するのが基本フロー。`docs/GAME_DESIGN.md` がゲーム仕様の Single Source of Truth。

---

## 8. AI 活用ガイド

[GAME_DESIGN.md §8](./GAME_DESIGN.md) を参照。

- コード生成: 積極使用
- アセット生成: GPT / VOICEVOX
- 設計: AI 主導
- レビュー: AI 可、ただし人間チェック必須

将来は `.claude/skills/` に共通スキル定義を置き、全メンバーが同一の AI 出力傾向で開発できるようにする。
