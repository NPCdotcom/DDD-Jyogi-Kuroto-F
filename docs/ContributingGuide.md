# 開発者ガイド

DDD-Jyogi-Kuroto-F に貢献するための運用ガイド。

---

## 1. 開発環境セットアップ

### 1-1. 必要なツール

| ツール | バージョン | 備考 |
|---|---|---|
| JDK | **Java 25 LTS (Oracle JDK 25)** | — |
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

## 4. CI

現状 CI ジョブは配置していない（沼にハマりやすいため軽量運用）。`build.gradle` 追加時に以下を最小構成で導入予定:

- Gradle build
- Spotless check (`./gradlew spotlessCheck`)
- JUnit 5 テスト実行

ブランチ命名規則 (`feat/#\d+` / `fix/#\d+` / `docs/#\d+` / `chore/#\d+` / `develop`) は手動で守る方針。PR レビュー時に確認する。

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

### MVP 後の実装時 (§15 対応)

- ポップアップ式 UI ([§15-1](./GAME_DESIGN.md)) 実装時は FancyMenu Mod 等の参照実装を AI に提示する
- カードシステム ([§15-3](./GAME_DESIGN.md)) のコスト・効果調整は AI 大量生成 → 人間選別の運用を推奨
- ソウルツリー ([§15-7](./GAME_DESIGN.md)) のノード配置は AI 生成 → 人間がバランス調整に回す

> **運用フロー詳細は実装段階で詰める**。当面は「実装着手者が参照 URL・要件を Issue or PR に貼り、AI に渡す → 出力を人間が選別」の素朴な運用で開始。`.claude/skills/` の整備は M1.5 以降で。

将来は `.claude/skills/` に共通スキル定義を置き、全メンバーが同一の AI 出力傾向で開発できるようにする。
