---
name: m1-5-start
description: DDD-Jyogi-Kuroto-F の M1.5 機能 (E-1〜E-10) の着手ワークフロー。Issue 起票 → feat ブランチ作成 → domain-architect で設計レビュー → 実装雛形 まで自動化
argument-hint: [E-番号 (E-1〜E-10)] [機能名]
allowed-tools: Read, Edit, Write, Grep, Glob, Bash(git*), Bash(gh issue*), Bash(./gradlew*)
---

## あなたのタスク

M1.5 機能 (E-1〜E-10) の着手準備を行う。`$ARGUMENTS`:
- `$0`: E-番号 (例: `E-1`、`E-3` 等)
- `$1`: 機能名 (例: `カードシステム`、`層構造` 等)

E-1〜E-10 の定義は [docs/RolesDivision.md カテゴリ E](../../../docs/RolesDivision.md) と [docs/GAME_DESIGN.md §15](../../../docs/GAME_DESIGN.md) を参照。

## 手順

### 1. 該当 E-X の仕様確認
- `docs/RolesDivision.md` で E-X の概要を Read
- `docs/GAME_DESIGN.md` の該当 § (例: E-1 → §15-3、E-3 → §15-6) を Read
- `docs/SystemSummary.md §8` で拡張ロードマップにおける位置を Read

### 2. Issue 起票

```bash
gh issue create -R NPCdotcom/DDD-Jyogi-Kuroto-F \
  --title "[Task] $0 $1 の実装" \
  --label "- type/task" \
  --body "<本文>"
```

本文テンプレ:
```markdown
## タスク名

$0 $1 の実装

## 作業内容

[docs/GAME_DESIGN.md §15-X](../docs/GAME_DESIGN.md) の仕様に基づき、$1 を実装する。

### 対応パッケージ
- (該当パッケージを列挙)

### 仕様の核
- (§15-X の核となるルールを 3〜5 行で抜粋)

## 優先度

P1 (M1.5)

## 完了条件

- [ ] ドメイン層 (該当 record / sealed) が実装されている
- [ ] テスト (ハッピー + 境界 + 例外) が PASS する
- [ ] §15-X 仕様との整合性を game-design-reviewer (or final-architect) で確認
- [ ] (LibGDX 依存があれば) presentation 層も実装
- [ ] チームの 1 名以上のレビュー approve
- [ ] develop へマージ

## 関連

- §15-X
- 依存する E-X (あれば列挙)
```

### 3. ブランチ作成

Issue 番号 (起票で返ってきた URL の末尾) を取得して:

```bash
git fetch origin develop
git switch -c "feat/#<Issue番号>/$0-skeleton" develop
```

### 4. 設計レビュー (domain-architect サブエージェント呼出)

Agent ツールで `domain-architect` を呼び出し:
- 「$0 $1 の実装にあたり、core/domain/ 配下に必要な新規パッケージ / record / sealed interface を提案してください」
- 設計案を受け取ったら、**ユーザーに確認** してから実装に進む

### 5. 実装雛形 (.gitkeep + package-info.java)

新規パッケージが必要なら、`.gitkeep` または `package-info.java` で先行作成:

```java
/**
 * $1 (§15-X) の格納先。
 *
 * 着手予定: M1.5 / Issue #<番号>
 */
package core.domain.xxx;
```

これで「空パッケージにメモを残す」というプロジェクトルール (CLAUDE.md) を守る。

### 6. 動作確認の準備

- `gradlew test` で既存テストが全 PASS することを確認
- 既存実装に変更を加える場合、影響範囲を Grep で確認

### 7. 出力

- 起票した Issue URL
- 作成したブランチ名
- domain-architect の設計案 (要点)
- 次のステップ (test-writer / libgdx-implementer のどちらに引き継ぐか)

## ルール

- **勝手に実装まで進めない**。domain-architect の設計案をユーザー確認してから実装着手する
- ブランチ命名は [BranchingStrategy.md §2](../../../docs/BranchingStrategy.md) 準拠: `feat/#<issue番号>/<短い説明>`
- E-X の依存関係 ([SystemSummary.md §8](../../../docs/SystemSummary.md) Phase A〜D) を意識: E-1 (カード) は E-3 (層) より先、E-2 (ソウルツリー) は他の基盤後 等
