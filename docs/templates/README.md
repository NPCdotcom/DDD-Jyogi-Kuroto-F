# カード / 装備 設計テンプレート

チームメイトがカード・装備を設計する際の JSON テンプレート集。
ChatGPT 等の AI に「このテンプレで火属性カードを 5 種類書いて」と投げる用途も想定。

## ファイル

- [cards.json](cards.json): カード 7 種のサンプル (実装済 3 + 想定 4)
- [equipments.json](equipments.json): 装備 2 種のサンプル

## 書き方

1 つのカード / 装備は 2 ブロック構造で書く:

```json
{
  "gameplay": { 数値・enum (ゲームの動作に使う) },
  "metadata": { 文章・素材パス (画像生成・UI 表示用) }
}
```

- **`gameplay` ブロック**: 内部の `Card` / `Equipment` record に直接マップされる (実装と完全一致)
- **`metadata` ブロック**: UI 表示 / AI 画像生成用 (将来 `CardMetadata` record にマップ)

## 選べる固定値 (これ以外は弾かれる)

| 項目 | 選択肢 |
|---|---|
| `gameplay.tag` | `ATTACK` / `MOVEMENT` / `BUFF` / `TRAP` |
| `gameplay.element` | `PHYSICAL` / `MAGICAL` |
| `gameplay.effect.type` | `Damage` / `Move` / `Buff` / `Trap` |
| `effect.kind` (Buff のとき) | `PHYSICAL_ATTACK_UP` / `MAGICAL_ATTACK_UP` / `PHYSICAL_DEFENSE_UP` / `MAGICAL_DEFENSE_UP` / `SPEED_UP` |
| `effect.lifetime.type` (Trap のとき) | `UntilStepped` (踏むまで残る、物理罠) / `Turns` (N ターン後消滅、魔法罠、`remaining` 必須) |
| 装備の `slot` | `FEET` / `HAND` / `BODY` / `HEAD` / `ACCESSORY` |

## 数値の制約

- `id`: snake_case 半角英数のみ (例: `fireball`, `iron_skin`)
- `apCost` / `baseValue` / `distance` / `durationTurns`: **1 以上必須**
- `amount` のみ 0 不可 (負値はデバフ表現可)
- 装備の `statsBonus` は **6 項目** (`currentHp` は含めない、`maxHp` / `speed` / 4 攻防ステ)
- 装備の `grantedCardIds` は cards.json で定義したカードの `id` を文字列で参照、空配列 `[]` も OK

## 現状の実装状況 (2026-05-14 時点)

| カードタイプ | 状態 |
|---|---|
| **Damage 系** | ✅ ゲーム内で動作 (zangeki / magic_bolt / strong_strike / fireball) |
| Move 系 | 🚧 TurnEngine 未実装、5/15 以降にゲーム内動作開始予定 |
| Buff 系 | 🚧 同上 |
| Trap 系 | 🚧 同上 |
| 装備全般 | 🚧 Equipment record 未実装、5/16 以降に動作開始予定 |

上のテンプレで書いた JSON は、実装が追いつき次第そのままゲーム内で動くようになります。
**今は安心して書いてください、ロード機構が動いた瞬間に反映されます。**

## チーム作業フロー

1. このディレクトリの JSON を Google Drive / Discord に上げる
2. ChatGPT 等で「テンプレに沿って 5 種類カード考えて」と AI に投げて生成 (採否はリーダーが判断)
3. Discord で議論 → 採用案を `cards.json` / `equipments.json` に追記
4. リーダーが `InitialStateFactory` に取り込み (or 将来は JSON ローダで自動取り込み)
