# 素材制作仕様書 (チームメイト向け)

> DDD-Jyogi-Kuroto-F の **スプライト・音声・スキルカード画像** を制作する際の単一仕様書。
> ライセンス・取得元のガイドラインは姉妹文書 [docs/AssetGuidelines.md](AssetGuidelines.md) を参照。

本書がカバーする範囲:
- **§A-1 スプライト** (Player / Enemy / Tile / Trap) — 現状 `ShapeRenderer` 矩形で代替
- **§A-2 音声** (BGM / SE) — `assets/audio/README.md` の補足
- **§A-3 スキルカード画像 (未画像 11 枚)** — 既存 49 枚と整合させる

最終更新: 2026-05-23

---

## §A-1. スプライト (Entity / Tile / Trap)

### 基準寸法

- マップ 1 マス = **80×80 px** (`core.presentation.render.RenderLayout.TILE_SIZE = 80`)
- エンティティスプライトは **64×64 〜 80×80 px**、4〜8 px の透明マージン推奨
- ゲーム解像度は 1920×1080 ベース (固定 viewport)

### 形式

- **PNG / RGBA / 透明背景必須**
- ピクセルアート — ゲーム側で `Nearest` フィルタを適用 (アンチエイリアスなし)
- ドット感を意識: 1 ドットの境界が崩れないようピクセルパーフェクトで描く

### 配置パス + ファイル名 (snake_case)

#### エンティティ (`assets/sprites/`)
| ファイル名 | 用途 | 現在の暫定描画色 |
|---|---|---|
| `player.png` | プレイヤー | 青 `RGB(76,166,255)` |
| `slime.png` | 通常スライム | 緑 `RGB(102,217,102)` |
| `swift_slime.png` | 素早い個体 (高 AP / 低 HP) | 水色 `RGB(102,217,204)` |
| `tough_slime.png` | 頑強な個体 (高 HP / 高物防) | 灰 `RGB(115,140,158)` |
| `elite_slime.png` | 強化個体 (5 層ごと、撃破でカード追加 UI) | 橙 `RGB(217,140,51)` |
| `boss.png` | 最終層ボス | 赤 `RGB(217,64,77)` |

#### タイル (`assets/tiles/`、80×80 px)
| ファイル名 | 用途 |
|---|---|
| `tile_floor.png` | 通路・部屋の床 |
| `tile_wall.png` | 壁 |
| `tile_stairs.png` | 次層への階段 (踏破で CLEARED) |

#### 罠 (`assets/traps/`、`cards.json` の TRAP 系 10 種)
| ファイル名 | cards.json id | 属性 |
|---|---|---|
| `trap_spike.png` | spike_trap | 物理 |
| `trap_bear.png` | bear_trap | 物理 |
| `trap_poison.png` | poison_needle | 物理 |
| `trap_pitfall.png` | pitfall | 物理 |
| `trap_wire.png` | wire_trip | 物理 |
| `trap_frost.png` | frost_rune | 魔法 |
| `trap_thunder.png` | thunder_rune | 魔法 |
| `trap_void.png` | void_rune | 魔法 |
| `trap_acid.png` | acid_pool | 魔法 |
| `trap_dark.png` | dark_snare | 魔法 |

### 取り込み手順 (artist or CTO)

**重要**: 現状の `DungeonRenderer` は矩形描画のため、スプライト投入後に `DungeonRenderer` の
改修 (Texture ロード + draw) が必要。配置するだけでは自動表示されない。

1. PNG を上記パスに配置
2. `LICENSES/INDEX.md` に 1 行追加 (取得日 / 取得元 / ライセンス / クレジット)
3. **CTO に通知** — `DungeonRenderer` の `drawEnemies` / `drawMap` / `drawTraps` を矩形描画から
   Texture 描画に置換する PR を別途作成

---

## §A-2. 音声 (BGM / SE)

### Single Source of Truth

配置パス・ファイル名は [`assets/audio/README.md`](../assets/audio/README.md) を**正**とする。
本書では制作時の補足のみ記載する。

### 期待されるファイル (audio/README.md より抜粋)

```
assets/audio/
  bgm/
    title.ogg          … タイトル / メニュー系画面 BGM (ループ)
    dungeon.ogg        … ダンジョン戦闘 BGM (ループ)
  se/
    enemy_defeated.ogg … 敵撃破
    player_damaged.ogg … プレイヤー被ダメージ
    deal_damage.ogg    … プレイヤーが敵に与ダメージ
    card_used.ogg      … カード使用
    button.ogg         … ボタン操作 / 画面遷移 / 選択確定
    floor_advance.ogg  … 層遷移
```

### 形式

- **`.ogg` (Vorbis)** 推奨。`.wav` も可だがファイルサイズに注意
- **SE**: 500 ms 以下、モノラルでも可、軽量重視
- **BGM**: 30〜90 秒の**シームレスループ**、ステレオ
- サンプリングレート: 44.1 kHz / 16-bit が無難

### 音量目安

- 素材は **-6 dB ピーク**程度に揃える (クリッピング回避 + 余裕)
- ゲーム側 (`Settings`) で BGM / SE 個別音量を線形適用するため、素材側で正規化は不要
- 静かな素材ほど良い (`Settings` で 0.0〜1.0 のスライダー調整される)

### 欠損耐性

ファイルが無い種別は自動的に**無音 (no-op)** になる。揃っていなくてもゲームは正常に動く。
素材ができた種別から順に投入可。

### 将来追加候補

`SeKind` / `BgmKind` enum (`core.infrastructure.audio`) に値を追加してから配置:
- `level_up.ogg` (ソウルツリーノード解放時)
- `soul_gain.ogg` (雑魚撃破でソウル獲得時の触感補完)
- `card_draw.ogg` (毎ターン頭のドロー時)

---

## §A-3. スキルカード画像 (未画像 11 枚)

### 既存カードとの整合性

`assets/cards/` に **49 枚の既存カード画像** (`card_01.png` 〜 `card_50.png` の 27 番欠番) が
配置されている。未画像 11 枚はこれらと**視覚的に整合**させる必要がある。

### 既存スタイル (`card_01.png` を参考に)

- **寸法**: **150×188 px** (実測値)
- **形式**: PNG / RGBA / 透明背景 (枠の外側)
- **レイアウト**:
  - 左上: **AP 数字 (1〜6)** を円形バッジで配置
  - 右上: 閉じる風の X アイコン (装飾、機能なし)
  - 中央: 効果を表すアイコン (剣・炎・矢印など)
  - 底部: **displayName 帯** (茶色背景に白文字、中央寄せ)
  - 右下: **レアリティバッジ** (`C` Common / `U` Uncommon / `R` Rare)
- **色味**: 茶色の枠 + ベージュ系の内側、アイコンはタグ/属性別 (物理=銀/赤、魔法=青/紫、罠=緑/黄、
  バフ=金色/緑色、移動=水色)

### 制作対象 11 枚

| cards.json id | displayName (画像内表記) | AP | tag | element | effect | レアリティ目安 |
|---|---|---|---|---|---|---|
| `zangeki` | 斬撃 | 1 | ATTACK | PHYSICAL | Damage 3 (**初期カード**) | C |
| `strong_strike` | 強打 | 1 | ATTACK | PHYSICAL | Damage 4 | C |
| `magic_bolt` | 魔法弾 | 2 | ATTACK | MAGICAL | Damage 5 | C |
| `fireball` | 火球 | 2 | ATTACK | MAGICAL | Damage 6 | U |
| `ember_shot` | 炎弾 | 1 | ATTACK | MAGICAL | Damage 3 | C |
| `blaze_nova` | 爆炎 | 3 | ATTACK | MAGICAL | Damage 9 | R |
| `blink_step` | 瞬歩 | 1 | MOVEMENT | MAGICAL | Move distance 3 | U |
| `flame_circle` | 炎の魔法陣 | 2 | TRAP | MAGICAL | Trap 5 ダメ / 4 ターン持続 | U |
| `arcane_veil` | 魔力の帳 | 2 | BUFF | MAGICAL | 魔防 +3 / 2 ターン | C |
| `stone_wall` | 岩壁の構え | 2 | BUFF | PHYSICAL | 物防 +4 / 2 ターン | U |
| `haste` | ヘイスト | 2 | BUFF | MAGICAL | 速度 +3 / 3 ターン | U |

**特記事項**: `zangeki` (斬撃) は**ぼろい短剣の初期付与カード**で、新規ラン開始時に必ず手札に
入る。デモの第一印象に直結するためアートクオリティ優先推奨。

### 配置パス + ファイル名

- **パス**: `assets/cards/` (既存と同じ)
- **ファイル名**: artist 任意。例:
  - 既存と同じ命名規則を継続するなら `card_27.png` (欠番) + `card_51.png` 〜 `card_60.png`
  - id 名を直接使うなら `zangeki.png` / `fireball.png` …
  - 紐付けは `card_image_map.json` で吸収するため、どちらでも OK

### 投入後の取り込み手順

1. PNG を `assets/cards/` に配置
2. `src/main/resources/card_image_map.json` を編集:
   - `mappings` に 1 行追加 (例: `"zangeki": "card_27"`)
   - `unmappedCards` 配列から該当 id を削除
3. `gradlew run` で起動し `CardCollectionScreen` (タイトル画面で **C キー**) で表示確認
4. 暗色シルエットでなく通常色で表示されれば成功

未画像 11 枚をすべて投入すれば、cards.json の全 60 カードが画像化される。

---

## §A-4. ライセンス / クレジット

- 素材は **CC0 / SIL OFL / オリジナル制作** を優先 ([AssetGuidelines.md §3](AssetGuidelines.md))
- 制作した素材は [`LICENSES/INDEX.md`](../LICENSES/INDEX.md) に 1 行追加 (取得日 / 取得元 /
  ライセンス / クレジット / 改変点)
- **本プロジェクトのコード本体はライセンス指定なし (オーナー保有)** だが、同梱素材は素材ごとの
  ライセンスを尊重する
- 第三者素材の改変版を配布する場合、元素材のライセンス条項に従う

---

## §A-5. 完成チェックリスト (PR 提出前)

- [ ] 寸法は仕様どおり (カード: 150×188、エンティティ: 64〜80 px、タイル: 80×80)
- [ ] 透明背景 (PNG/RGBA、αチャネルあり)
- [ ] 配置パスと命名が本仕様書どおり (snake_case + 拡張子)
- [ ] `LICENSES/INDEX.md` に行追加
- [ ] カード画像の場合 `card_image_map.json` の更新内容を PR 説明に明記
- [ ] スプライト・タイル・罠の場合 CTO に通知 (`DungeonRenderer` 改修が必要)
- [ ] 音声の場合、欠損耐性があるためそのまま配置で OK (`SoundManager` が自動ロード)

---

## 関連ドキュメント

- [docs/AssetGuidelines.md](AssetGuidelines.md) — 素材取得・ライセンス管理ルール
- [assets/audio/README.md](../assets/audio/README.md) — 音声配置規約 (Single Source of Truth)
- [src/main/resources/card_image_map.json](../src/main/resources/card_image_map.json) — カード id ↔ 画像紐付け
- [LICENSES/INDEX.md](../LICENSES/INDEX.md) — 全素材のライセンス一覧
- [docs/GAME_DESIGN.md §15](GAME_DESIGN.md) — ゲーム仕様 (カード・敵・装備のドメイン定義)
