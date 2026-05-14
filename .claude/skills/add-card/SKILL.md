---
name: add-card
description: DDD-Jyogi-Kuroto-F のカード (§15-3) を 1 種追加するワークフロー。JSON 定義 + Card record + SkillEffect 拡張 + テスト + §15-3 ドキュメント更新 までを一気通貫
argument-hint: [card-id] [name] [tag] [element] [ap-cost] [base-value] [effect-type]
allowed-tools: Read, Edit, Write, Grep, Glob, Bash(./gradlew test*), Bash(./gradlew spotlessApply*)
---

## あなたのタスク

カード 1 種を追加する。`$ARGUMENTS` の各位置:
- `$0`: カード ID (例: `light_slash`)
- `$1`: 表示名 (例: `斬撃`)
- `$2`: タグ (`attack` / `move` / `buff` / `trap`)
- `$3`: 属性 (`physical` / `magical`)
- `$4`: AP コスト (整数、最低 1)
- `$5`: カード基礎値 (ダメージ量 / 移動マス数 等)
- `$6`: 効果種別 (`damage` / `move` / `buff` / `trap`)

引数が不足していれば、ユーザーに不足分を尋ねる。

## 手順

### 1. 仕様確認
- [docs/GAME_DESIGN.md §15-3](../../../docs/GAME_DESIGN.md) を Read で確認
- 既存カードを Grep で把握: `grep -r "SkillId.of" src/main/java/core/`

### 2. JSON 定義
- `assets/data/cards/$0.json` を新規作成 (`assets/data/cards/` ディレクトリが無ければ作成):
```json
{
  "id": "$0",
  "name": "$1",
  "tag": "$2",
  "element": "$3",
  "apCost": $4,
  "baseValue": $5,
  "effect": "$6",
  "targetRange": "adjacent_4dir"
}
```

### 3. ドメインクラス追加
- `core/domain/card/Card.java` の sealed interface に case 追加 (該当タグの実装クラス追加)
- `core/domain/skill/SkillEffect.java` に対応する effect record 追加 (sealed 網羅性)
- 既存カードと同様のパターンを踏襲

> 重要: ドメイン層の変更は **domain-architect サブエージェント** に依頼する方が安全。複雑な場合は `architect-review` で先に設計検証。

### 4. テスト追加
- `src/test/java/core/domain/card/$0Test.java` を新規:
  - ハッピーパス: カードが正しい AP / 効果値を返す
  - 境界値: AP 不足時の挙動、効果値 0 / max
  - 例外パス: 不正値での例外
- `src/test/java/core/domain/support/DomainFixtures.java` に `$0Card()` ファクトリ追加 (必要なら)

### 5. ドキュメント更新
- `docs/GAME_DESIGN.md §15-3` の **カード分類表** に該当カードを追記 (例: 「物理 × 攻撃: 斬撃 / 突き / 弓 / **新カード名**」)
- 必要なら改訂履歴に 1 行追記

### 6. ビルド確認

```bash
./gradlew spotlessApply
./gradlew test --tests "*${0}Test"  # 該当テストだけ
./gradlew test                       # 全体回帰
```

すべて PASS したら完了。

### 7. 出力

- 追加したファイル一覧
- 修正したファイル一覧
- テスト結果 (PASS 数 / 失敗あれば原因)
- §15-3 への追記内容
- 次に必要なアクション (報酬プールへの組み込み等)

## ルール

- `add-card` は **1 種ずつ**。複数追加は複数回呼ぶ
- カード ID は重複不可、`SkillId.of("$0")` で他に存在しないことを Grep で確認
- §15-3 の数値ルール (AP 最低 1、報酬重み付け 60/20/20) を守る
- 設計変更が必要なら勝手にやらず、ユーザーに「§15-3 の仕様変更が必要、進めて良いか」を確認
