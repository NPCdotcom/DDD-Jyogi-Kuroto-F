# 画像生成ブリーフ (AI 生成用プロンプト + 必要情報の整理)

- 作成日: 2026-08-13
- 対象: カードイラスト 59 枚、ソウルツリー用アイコン、ステータスアイコン正規化
- 前提となる決定: 「NPCdotcom 制作フレーム + art-only 画像 + 動的テキスト」を実行時に合成する (2026-08-12 採択)

---

## 0. 最重要の前提 — 画像に文字と数値を焼き込まない

現行 59 枚の最大の問題は、**カード名・AP・レアリティ・効果文が画像に焼き込まれている**ことである。実データと 43 枚が食い違い、プレイヤーは表示と異なるコストで行動していた。

したがって新しいイラストは以下を**一切含んではならない**。

| 含めないもの | 理由 |
|---|---|
| 文字・数字・記号 (日本語/英語/ローマ数字を問わず) | `cards.json` から実行時描画する。焼き込むと再び食い違う |
| カード枠・縁取り・角丸のフレーム | フレームは NPCdotcom 制作の共通素材を重ねる |
| AP バッジ・レアリティ記号 (C/U/R) | 実行時にデータから描画する |
| 効果説明のテキスト帯 | 同上 |
| 背景の市松模様 (透過を表す市松) | 現行画像はこれを絵柄として焼き込んでおり、暗い手札パネル上で矩形として露出する |
| 白背景・単色べた塗り背景 | 同上。**背景は完全透過**にする |

**これを守れているかの確認方法**: 生成物を暗い背景 (#1A1A22 程度) の上に置き、四隅と縁に矩形の境界が見えないこと。文字が 1 つも写っていないこと。

### 現実的な前提 — ネガティブ指定は守られない

画像生成 AI は「文字を入れるな」「枠を描くな」を**しばしば無視する**。特に「カード」という語がプロンプトに入ると、枠・文字・レイアウトを勝手に付ける傾向が強い。したがって:

- プロンプトに **`card` / `trading card` / `icon frame` の語を使わない**。本ブリーフの画風ブロックが `item icon style` に留めているのはこのため
- **歩留まりを見込む**。1 枚あたり 3〜5 回の生成を想定し、条件を満たす 1 枚を選ぶ運用にする
- 文字や枠が入った生成物は**加筆で消さず破棄する**。消し跡が残ると縮小時に目立つ
- 透過が出ないサービスの場合は、単色背景で生成 → 背景除去 → **端に接続した領域だけ**を除去する。現行画像は白背景と市松模様を絵柄ごと焼き込んでおり、一括透明化では直らなかった

### ピクセルアートと縮小

生成 AI は「ピクセル風に見える高解像度画像」を出すのが実態で、真のドット絵にはならない。ただし 240×336 は手札 120×168 の**ちょうど 2 倍**なので、Nearest 縮小でも 2 ピクセルが 1 ピクセルへ規則的に落ち、線幅は崩れない。

- **揃えたい場合**: 60×84 で生成して 4 倍の最近傍拡大で 240×336 にする。ただし生成 AI は低解像度指定を苦手とするため、1 枚スパイクで品質を比較してから決める
- **画風を変える選択肢**: ピクセルアートをやめて「16-bit 風の描き込みイラスト」に振るなら、`CardImageRegistry` のカード用フィルタを Linear へ変える手もある (1 行)。その場合は寸法制約が緩む。**1 枚スパイクで両案を比較すること**

### 検収基準 (1 枚ごとにこれで合否を出す)

| # | 基準 | 判定方法 |
|---|---|---|
| 1 | 文字・数字が 1 つも無い | 目視 |
| 2 | 枠・縁取りが無い | 目視 |
| 3 | 背景が完全透過 | 暗色背景に重ねて矩形が出ないこと |
| 4 | 240×336 ちょうど | ファイル情報 |
| 5 | 上下左右 20px のセーフエリアに主題が掛かっていない | ガイド重ね |
| 6 | 120×168 へ縮小してもモチーフが判別できる | 縮小表示で確認 |
| 7 | 同バッチ 4 枚の色調・線の太さが揃っている | 接触シートで比較 |

---

## 1. 出力仕様

| 項目 | 値 |
|---|---|
| キャンバス | **240 × 336 px** (縦長、実行時に手札で 120×168 へ等比縮小) |
| 背景 | **完全透過 (アルファ 0)**。PNG-32 |
| セーフエリア | 上下左右 **各 20 px** は空ける (**暫定値**、下記参照) |
| 主題の配置 | 中央やや上寄り。下部 **96 px** はテキスト帯が重なるので重要な要素を置かない (**暫定値**) |
| 形式 | PNG (可逆)。JPEG 不可 (透過が失われる) |
| ファイル名 | `assets/cards/art/<cardId>.png` (例: `assets/cards/art/fireball.png`)。連番 `card_NN` ではなく **カード ID** を使う |

> **セーフエリアと下部帯は暫定値である。** フレームはまだ存在せず、AP バッジの実寸も名称帯の高さも決まっていない。段階 1 (G0) でフレームと**矩形の px 座標表**が確定した時点で実測値へ差し替えること。暫定値のまま生成を始めると、主題がバッジ下に隠れて作り直しになる。

> 現行画像は 133×171 〜 807×1186 まで世代ごとにバラバラで、縦横比も線の太さも揃っていない。今回は全 59 枚を同一キャンバスで作る。

### 既存仕様書との関係 (重要)

[docs/AssetProductionSpec.md](AssetProductionSpec.md) §A-3 は **150×188 px** を「実測値」として定めている。本ブリーフの 240×336 は**それを置き換える**。

- 150×188 (縦横比 0.798) と手札描画 120×168 (0.714) は**比率が違う**ため、現行画像は実行時に必ず歪んでいた
- 240×336 は 120×168 の**ちょうど 2 倍**で比率も一致する (240/336 = 120/168 = 0.714)

### なぜ 240×336 なのか (整数倍で揃える)

`CardImageRegistry.java:102` はカードテクスチャに **min/mag とも Nearest フィルタ**を適用する。Nearest は点サンプリングなので、非整数倍の縮小では線幅が不均一になる。レビュー §7.9 はこれを「2.5 倍拡大でドット幅が不均一」として既に欠陥認定している。

240×336 なら実行時の主要 3 箇所すべてが整数比に収まる。

| 表示箇所 | 実寸 | 240×336 との比 |
|---|---|---|
| 手札 (`RenderLayout.HAND_CARD_*`) | 120×168 | **1/2** (整数) |
| 解放ダイアログ (`SoulNodeUnlockDialog:135-136`) | 240×336 | **等倍** |
| 図鑑サムネ (`CardCollectionScreen:40-41`) | 32×42 | 非整数 (比率 0.762 も不一致) |

図鑑サムネだけが揃わない。CARD-04 でコード側を **40×56** (1/6、比率 0.714) へ直すことを推奨する。

> 当初 300×420 で書いていたが、手札への 2.5 倍縮小がレビュー §7.9 の欠陥をカード全面へ広げるため 240×336 へ改めた。**この寸法は G0 のフレーム制作前に確定させる必要がある** (承認後に変えるとフレーム作り直し)。
- CARD-05 完了後に `AssetProductionSpec.md` §A-3 を 240×336 へ改訂し、150×188 の記述を削除すること。両方が生きている状態を残さない

### ソウルツリー上での表示 (縦長のままでは歪む)

`SoulTreeScreen.NODE_TEX_SIZE = 64f` により、CardGrant ノードは **64×64 の正方形**でカード画像を描く。縦長 240×336 をそのまま入れると横に潰れる。

対応は次のいずれかを CARD-04 で決める。本ブリーフでは**イラスト側は 240×336 のみを作る**前提とし、正方形版を別途生成させない。

1. `SoulTreeScreen` 側で縦横比を保って収める (contain 描画、上下に余白)
2. ノード枠を 64×90 の縦長にする
3. カード解放ノードは専用の小アイコンを使い、カード画像を出さない

> 現行は縦長の完成カードを 64×64 へ変形して表示しており、カード画面と異なる比率になっている (レビュー §7.6)。1 を採るのが最小変更。

### 画風 (全 59 枚で統一)

```
2D pixel art, 16-bit JRPG item icon style, chunky readable pixels,
limited palette (max 24 colors), strong silhouette, high contrast,
dark fantasy dungeon theme, centered single subject,
flat lighting with one clear light source from upper left,
fully transparent background, no text, no frame, no border
```

**ネガティブ (対応サービスのみ)**

```
text, letters, numbers, watermark, signature, frame, border, card layout,
UI elements, checkerboard background, white background, solid background,
photorealistic, 3D render, blurry, multiple subjects
```

---

## 2. プロンプトの組み立て方

全カード共通で次の 3 段を連結する。

```
[画風ブロック] + [属性ブロック] + [個別モチーフ]
```

### 属性ブロック (element × tag で決める)

| 分類 | 追加する語 |
|---|---|
| PHYSICAL / ATTACK | `steel weapon, metallic sheen, cold gray-blue steel with warm leather accents, impact motion` |
| MAGICAL / ATTACK | `arcane energy, glowing runes, luminous core, emissive highlights` |
| PHYSICAL / MOVEMENT | `motion streaks, dust puff, boots or footwork emphasis, sense of momentum` |
| MAGICAL / MOVEMENT | `teleport shimmer, fading afterimage, violet arcane particles` |
| PHYSICAL / BUFF | `armor plating or banner, sturdy grounded shape, warm amber glow` |
| MAGICAL / BUFF | `protective sigil, translucent barrier, soft cyan-violet aura` |
| PHYSICAL / TRAP | `mechanical contraption, iron teeth or spikes, seen from a low three-quarter angle on the ground` |
| MAGICAL / TRAP | `glowing rune circle inscribed on stone floor, seen from a low three-quarter angle` |

内訳: ATTACK 26 / BUFF 13 / MOVEMENT 9 / TRAP 11。

### 強度の目安 (2 軸で決める)

数値そのものは描かないが、視覚的な序列を作る。**効果値とレアリティは別軸**なので分けて扱う。

**軸 1: 効果値 → 画面占有**

| 効果値 | 表現 |
|---|---|
| 1〜3 | 小さく静かな表現。単一の要素 |
| 4〜6 | 標準。中程度の軌跡 |
| 7〜10 | 大きく激しい表現。破片や衝撃波 |

**軸 2: レアリティ → 発光量** (G3 承認後に埋める)

| レアリティ | 表現 |
|---|---|
| COMMON | 発光は控えめ |
| UNCOMMON | 中程度の縁発光 |
| RARE | 強い発光とオーラ |

> **G3 待ちの理由**: 実行時に描かれる記号は C/U/R であって効果値ではない。現行 `cards.json` に明示 rarity があるのは **4 枚だけ** (`blaze_nova` / `meteor_drop` = RARE、`overhead_smash` / `teleport` = UNCOMMON)、残り 55 枚は暗黙 COMMON で、CARD-01 と G3 で確定する。
>
> 効果値だけで派手さを決めると、G3 の結果次第で「派手なのに C」「地味なのに R」が生まれる。**軸 2 は G3 承認後に確定させ、それまで ART バッチを始めない。**

---

## 3. カード別モチーフ一覧 (全 59 枚)

`[画風] + [属性] + 下記モチーフ` で生成する。`—` 区切りの後半は避けたい誤解釈。

### ATTACK / PHYSICAL (12)

| ファイル名 | 表示名 | 効果値 | モチーフ |
|---|---|---|---|
| `zangeki` | 斬撃 | 3 | a single diagonal sword slash arc, worn iron shortsword — 剣士の人物は描かない |
| `strong_strike` | 強打 | 4 | an armored fist mid-impact with a burst of force |
| `stone_throw` | 礫投げ | 3 (射程3) | a jagged stone hurled with a motion trail |
| `heavy_slash` | 重斬り | 6 | a heavy two-handed greatsword cleaving downward |
| `quick_stab` | 速突き | 4 | a slim dagger thrusting forward, sharp speed lines |
| `double_strike` | 二連撃 | 5 | two crossed blades forming an X of slash arcs |
| `armor_break` | 鎧砕き | 5 | a war hammer shattering a steel plate, fragments flying |
| `overhead_smash` | 振り下ろし | 9 | a massive maul slammed down, ground cracking, shockwave ring |
| `riposte` | 受け流し | 4 | a parrying blade deflecting an incoming strike, spark burst |
| `leg_sweep` | 足払い | 2 | a low sweeping kick arc close to the ground, dust |
| `piercing_arrow` | 貫通矢 | 6 (射程4) | an arrow piercing through a shield, straight motion line |
| `skull_crack` | 頭蓋割り | 8 | a spiked mace mid-swing with a violent impact star |

### ATTACK / MAGICAL (14)

| ファイル名 | 表示名 | 効果値 | モチーフ |
|---|---|---|---|
| `magic_bolt` | 魔法弾 | 5 (射程4) | a compact blue arcane orb with a trailing tail |
| `fireball` | 火球 | 6 (射程5) | a roaring orange fireball with swirling flame |
| `ember_shot` | 炎弾 | 3 | a small ember projectile, modest flame |
| `blaze_nova` | 爆炎 | 9 (射程4/範囲1) | a huge spiraling firestorm explosion, intense glow |
| `ice_lance` | 氷槍 | 6 (射程4) | a sharp crystalline ice spear, pale cyan |
| `thunder_bolt` | 雷撃 | 7 | a jagged yellow lightning bolt, crackling arcs |
| `dark_pulse` | 暗黒波 | 4 | a dark purple wave of shadow energy |
| `wind_blade` | 風刃 | 2 | a thin crescent of compressed wind, pale green |
| `frost_nova` | 凍結爆発 | 8 | a radial burst of ice shards from a frozen core |
| `void_strike` | 虚無撃 | 9 | a black void tear rimmed with violet light |
| `acid_splash` | 酸の飛沫 | 4 | splashing green corrosive liquid droplets |
| `holy_bolt` | 聖光弾 | 5 | a radiant golden-white light projectile |
| `meteor_drop` | 隕石落とし | 10 (射程6/範囲2) | a flaming meteor falling with a long fire trail, largest and most intense of all cards |
| `shadow_bolt` | 影の矢 | 4 | a dark shadowy arrow of condensed gloom |

### MOVEMENT (9)

| ファイル名 | 表示名 | 距離 | モチーフ |
|---|---|---|---|
| `dash` | ダッシュ | 2 | forward speed streaks with a dust puff |
| `leap` | 跳躍 | 3 | an upward arcing jump trajectory line |
| `roll` | 回避転がり | 1 | a circular tumbling motion arc, low to the ground |
| `charge` | 突撃 | 4 | a powerful forward lunge with impact streaks |
| `retreat` | 後退 | 2 | a backward-pointing motion arrow with dust |
| `shadow_step` | 影歩き | 4 | a figure dissolving into shadow, dark silhouette fading |
| `wind_walk` | 風走り | 2 | swirling wind currents around footsteps |
| `blink_step` | 瞬歩 | 3 | a short-range flicker with a violet afterimage |
| `teleport` | 瞬間移動 | 6 | a bright arcane portal ring with swirling energy, longest range so most elaborate |

### BUFF (13)

| ファイル名 | 表示名 | 効果 | モチーフ |
|---|---|---|---|
| `iron_skin` | 鉄の皮膚 | 物防+2 | overlapping iron plates forming skin-like armor |
| `stone_wall` | 岩壁の構え | 物防+4 | a solid rock wall barrier, heavy and grounded |
| `fortify` | 要塞化 | 物防+5 | a fortress battlement with layered stone, strongest defensive card |
| `battle_cry` | 鬨の声 | 物攻+2 | a raised war banner with radiating sound rings |
| `war_stance` | 戦闘態勢 | 物攻+2 | a braced combat stance rendered as crossed weapons and a stable base |
| `berserker` | 狂戦士 | 物攻+4 | a raging red aura with clenched spiked gauntlets |
| `swiftness` | 疾風 | 速度+2 | swirling green wind spiral suggesting quickness |
| `magic_barrier` | 魔法障壁 | 魔防+3 | a hexagonal translucent energy shield |
| `arcane_veil` | 魔力の帳 | 魔防+3 | a shimmering veil of arcane threads |
| `mana_shield` | マナシールド | 魔防+2 | a blue mana orb surrounded by a protective ring |
| `focus` | 集中 | 魔攻+2 | a concentrated point of light with converging rays |
| `arcane_surge` | 魔力放出 | 魔攻+4 | an explosive burst of violet arcane power |
| `haste` | ヘイスト | 速度+3 | a winged boot with speed trails |

### TRAP (11)

すべて**地面に設置された状態**を低い斜め上からの視点で描く。

| ファイル名 | 表示名 | 効果値 | モチーフ |
|---|---|---|---|
| `spike_trap` | 棘罠 | 4 | iron spikes protruding from a floor plate |
| `bear_trap` | 熊罠 | 5 | a classic steel jaw trap, open and ready |
| `poison_needle` | 毒針罠 | 4 | fine needles with dripping green venom |
| `pitfall` | 落とし穴 | 7 | a dark open pit with broken cover boards |
| `wire_trip` | 鉄線罠 | 2 | a thin taut tripwire stretched near the floor |
| `frost_rune` | 氷結ルーン | 4 | a pale blue rune circle with frost creeping outward |
| `thunder_rune` | 雷撃ルーン | 4 | a yellow rune circle crackling with electricity |
| `void_rune` | 虚無ルーン | 7 | a black rune circle with a void center, most ominous |
| `acid_pool` | 酸の水たまり | 4 | a bubbling green acid puddle |
| `dark_snare` | 闇の罠 | 2 | dark tendrils forming a snare on the floor |
| `flame_circle` | 炎の魔法陣 | 5 | a burning magic circle with rising flames |

> **重複していた 7 組の扱い**: `strong_strike` / `quick_stab` / `riposte` などは実データ上まったく同じ挙動だった (CARD-06 で統合または差別化予定)。イラストは上記のとおり別モチーフにしてあるが、**カード自体が統合される可能性がある**。ART 着手は CARD-06 の結論後にすること。

---

## 4. カード以外に必要な画像

### 4-1. ソウルツリー用アイコン (不足 2 種、実害あり)

`NodeIconPathResolver` が要求するが**ファイルが存在せず**、8 ノードが `test.png` にフォールバックしている。

| ファイル | 用途 | サイズ | 内容 |
|---|---|---|---|
| `icons/center.png` | ツリー中心・層拡張ノード | 128×128 透過 | a glowing soul core / central nexus orb |
| `icons/frame.png` | (旧スキル枠ノード用) | 128×128 透過 | **不要になる可能性あり**。SOUL-02 でスキル枠ノードは廃止され「魂の刻印」へ置換されるため、下記を優先 |
| `icons/soul_sigil.png` | **魂の刻印 I / II** (装備保護枠) | 128×128 透過 | an engraved soul sigil / protective seal, suggesting "keeping what you carry" |

### 4-2. ステータスアイコン 6 種の正規化

現行は **1254×1254 px の完全不透明・白背景**で、64×64 表示だと白い正方形と細部潰れが目立つ。

| 対象 | 現状 | あるべき姿 |
|---|---|---|
| `icons/stats/hp.png` 他 6 枚 | 1254×1254、白背景不透明 | **128×128、完全透過**。単色シルエット寄りの明快な形 |

作り直す場合のモチーフ: hp=ハート、speed=羽根/稲妻、phys_atk=剣、mag_atk=杖or輝球、phys_def=盾、mag_def=魔法陣付き盾。

### 4-3. カードフレーム (NPCdotcom 制作、AI 生成対象外)

**既存資産あり、かつ一度廃止した設計への巻き戻しである。**

`assets/icons/cards/card_frame.png` が **96×112 px** で実在する。さらに `HudRenderer.java:222` に次のコメントが残っている。

> 2026-05-24: card_frame.png 合成描画を廃止し、card_XX.png (完成形カード絵) を 1 枚描画するのみ。

つまり本プロジェクトは**一度フレーム合成をやって捨てている**。今回はその決定の巻き戻しにあたる。

**なぜ今回は合成へ戻すのか**: 当時は「完成カード 1 枚を描くだけ」の方が単純で速かった。しかしその単純さの代償として、名称・AP・レアリティ・効果文を画像へ焼き込む必要が生じ、**実データと 59 枚中 43 枚が食い違った**。カードを 1 枚追加・調整するたびに画像を描き直す構造でもあった。合成へ戻すのは描画の手間を増やすためではなく、**値の唯一の出所を `cards.json` に一本化する**ためである。

**既存 96×112 の扱い**: 240×336 に対して小さすぎ (2.5 倍以上の拡大が必要)、縦横比も 0.857 で 0.714 と合わない。**再利用せず、新規に 240×336 で描き起こす。** 既存ファイルは CARD-05 / G5 で旧 PNG を整理する際に併せて削除する (それまでは参照用に残す)。

| ファイル | サイズ | 扱い |
|---|---|---|
| `assets/icons/cards/card_frame.png` (既存) | 96×112 | 参照用として残す。本番経路では使わない |
| 新フレーム (パスは実装時に確定) | 240×336 透過 | イラストを透かす中央窓を持つ。AP バッジ (左上)・レアリティ (右上/下部)・名称帯 (下部) の**領域だけ**を用意し、文字は入れない |

> これは人間ゲート **G0** の承認対象。AI 生成ではなく制作担当が作る。既存 96×112 を拡大流用すると 2.5 倍以上の拡大でぼやけるため、**新規に 240×336 で描き起こすこと**。

---

### 4-4. 本ブリーフで扱わない画像課題 (意図的な除外)

レビューが挙げた画像の問題のうち、以下は**今回の生成対象から外す**。放置ではなく、別タスクとして扱う判断である。

| 課題 | 除外理由 | 引き取り先 |
|---|---|---|
| 敵スプライトの名称と外見の不一致 (`SWIFT_SLIME`→スケルトン、`TOUGH_SLIME`→ゴブリン) | `DungeonScreen.java:165` に「ユーザー意図に合わせて入れ替え」とあり**意図的な決定**。画像ではなく敵 ID と表示名の改名で解く方が、選ばれたシルエット識別性を壊さない | 別 Issue (改名) |
| ピクセル拡縮倍率が非整数 (キャラ 32×32 → 80×80 の 2.5 倍、ボス 96×96 は縮小) | カード表示の整合とは独立。全スプライトの作り直しになり規模が大きい | 別 Issue |
| 装備アイコン (`assets/equipment/` 2 枚のみ) | 装備 20 件に対しアイコンが 2 枚しかないが、EQUIP-02 で装備画面の仕様が変わるため先行生成は手戻りになる | EQUIP-02 後 |
| タイル画像 (`assets/tiles/` 4 枚) | 現状で問題が報告されていない | なし |

## 5. 生成前に確定が必要な情報 (人間ゲート G1)

**AI 画像生成に着手する前に、以下が確定していない限り 1 枚も生成しない。** レビューのコンプライアンス指摘に対応する。

| # | 確認事項 | なぜ必要か |
|---|---|---|
| 1 | 使用する生成サービス名とモデル名 | 台帳へ記録する。サービスごとに権利条件が異なる |
| 2 | 契約プラン (無料 / 有料) | 無料プランは商用利用不可のサービスがある |
| 3 | 利用規約の**版と適用日** | 規約は改定される。取得時点の版を保存する |
| 4 | **商用利用の可否** | ハッカソン提出・将来の配布に関わる |
| 5 | **公開 Git リポジトリでの再配布可否** | 生成物をリポジトリへコミットするため必須 |
| 6 | 入力・参照画像の権利 | 他者の画像を参照入力しない。する場合は権利確認 |
| 7 | 学習利用・保持条件 | 入力が学習に使われるか |
| 8 | 禁止する参照 | **実在作家名・既存作品名をプロンプトに入れない**方針を明文化する |
| 9 | 人間の加筆有無 | AI 原出力と加筆後の両方を残す |

### 生成後に台帳へ記録する項目 (ASSET-01 のスキーマに対応させる)

記録先は ASSET-01 が定める `LICENSES/ASSET_PROVENANCE.json` (スキーマは `LICENSES/asset-provenance.schema.json`)。**独自項目を並べると `releaseCheck` の検証を素通りする**ため、下表のとおり 1 対 1 で対応させる。

| ASSET-01 の検証項目 | カード画像での値 |
|---|---|
| path | `assets/cards/art/<cardId>.png` |
| 種別 | `card-art` |
| 作者 | 生成 AI + 加筆者。**公開用表記名と公開同意の状態**を持たせ、非公開の氏名やアカウント情報は入れない |
| AI 関与 | `generated` (AI のみ) / `generated-then-edited` (人間加筆あり) |
| 生成サービスとモデル | G1 で確定した値 |
| URL | サービスの規約 URL |
| ライセンス | サービスが付与する条件 |
| 改変 | 加筆内容 |
| **SHA-256** | 最終ファイルのハッシュ |
| **原本ハッシュ** | AI の元出力のハッシュ (加筆した場合は最終と別物になる) |
| 承認状態 | G4 のバッチ承認で `approved` になる。**未承認があると releaseCheck が失敗する** |
| 規約の適用版と適用日 | G1 で記録 |
| 規約の保存写しとそのハッシュ | `LICENSES/` 配下へ保存 |
| 同定方法 | どうやってその素材と特定したか |
| 配布経路別の再配布条件 | 公開 Git / 配布 JAR それぞれ |

> **注意 2 点**。(a) ART-01〜15 は「元出力ハッシュ」と「最終ハッシュ」の **2 つ**を要求する。加筆する運用なら両方を残すこと。(b) 「担当」を実名で書くと配布 manifest の匿名化要件に反する。公開表記名と同意状態で持つこと。
>
> プロンプト全文はスキーマの検証項目に含まれていない。残す価値はあるが、**スキーマを拡張しない限り `AssetProvenanceTest` は素通りする**。拡張するか、別ファイル (`docs/` 配下のプロンプト台帳) に置くかを ASSET-01 着手時に決める。

**記録先と担当を先に決めること。** 項目を列挙しただけでは記録されない。以下が未定のまま生成を始めると、後から遡れなくなる。

| 決めること | 候補 | 未定のまま進めた場合 |
|---|---|---|
| 台帳の実体 | `LICENSES/INDEX.md` へ追記 / `assets/provenance.json` を新設 | 生成物だけ増えて出所が追えない |
| 記録するタイミング | 生成直後 / バッチ承認時 | 後回しにするとプロンプトを忘れる |
| 記録する人 | 生成した本人 | 誰も書かない |
| 規約本文の保存先 | `LICENSES/` 配下にスクリーンショットか保存写し | 規約改定後に当時の条件を証明できない |

> ASSET-01 は「全 PNG / OGG / TTF に必須項目と hash がある」を完了条件としている。カード 59 枚はその最大の塊なので、**台帳の形式を決めてから生成を始める**のが手戻りが少ない。

---

## 6. 進め方 (段階移行)

| 段階 | 内容 | ゲート |
|---|---|---|
| 1 | フレーム 1 枚を制作 (240×336)。**AP バッジ矩形・名称帯矩形の px 座標表**も併せて出す | **G0** 承認 |
| 2 | 生成サービスの権利条件を確定 + 台帳の形式を決める | **G1** 承認 |
| 3 | `zangeki` 1 枚だけ生成し、フレーム + 動的テキストで合成して 4 画面 (手札 / 選択 / 図鑑 / ソウルツリー) を確認 | **G2** 承認 |
| 4 | **59 枚のレアリティ一覧を確定** (CARD-01)。強度の軸 2 が決まる | **G3** 承認 |
| 5 | 残り 58 枚を **4 枚ずつ** 生成し、接触シートで確認 | **G4** 各バッチ |
| 6 | 旧画像を本番経路から外す | **G5** 承認 |

> **G3 を落としていた**。計画書の依存は ART-01〜15 → CARD-04 → CARD-03 → CARD-01 → G3 で、**レアリティ承認が下りるまで ART バッチは始められない**。段階 4 として明示した。

**ロールバック**: G5 まで旧 PNG は削除しない。art が不良と分かった場合は該当 ID だけ `card_image_map.json` の旧マッピングへ戻す。旧 PNG の削除は 1 コミット最大 5 ファイルに分ける (CARD-05)。

**1 枚スパイク (段階 3) を通すまで量産しない。** 現行 59 枚は全数が作り直しになった前例があり、同じ失敗を繰り返さないため。

### 順序を守らないと起きること

| 順序違反 | 起きること |
|---|---|
| **フレーム (G0) 確定前にイラストを生成** | セーフエリアの位置が変わり、主題がバッジや名称帯に隠れる。**全数作り直し** |
| 権利確認 (G1) 前に生成 | 商用不可・再配布不可のサービスだった場合、生成物を全部捨てることになる |
| 1 枚スパイク (G2) 前に量産 | 合成経路の不具合 (透過・比率・縮小品質) が 59 枚分に波及する |
| CARD-06 の結論前に ART 着手 | 重複 7 組が統合されると、描いたイラストが不要になる (59 → 52 前後) |
| 台帳形式の決定前に生成 | 出所を後から遡れず、ASSET-01 が完了できない |

**最初のボトルネックはフレーム (G0)。** ここが決まらない限り、イラスト生成は 1 枚も始められない。

---

## 7. 未確定事項

| 項目 | 完了条件 | 決める人 | いつまでに | 影響 |
|---|---|---|---|---|
| CARD-06 の重複 7 組を統合するか差別化するか | 対象 7 組それぞれに「統合」か「差別化 (固有差の内容)」が記録される | ユーザー | **G4 前** | 統合すると枚数が 59 → 52 前後に減る。決める前に描くと無駄になる |
| ステータスアイコン 6 枚を作り直すか現行を透過処理するか | 6 枚が 128×128 透過で揃い、64×64 表示で白背景が出ない | ユーザー | G1 後 | 作り直しなら 6 枚追加、透過処理なら加工のみ |
| `icons/frame.png` が SOUL-02 後も必要か | 不要なら `NodeIconPathResolver` から参照が消え、テストが実在パスを検証する | 実装判断 | SOUL-03 着手時 | 不要なら 1 枚減る |
| 刻印アイコンを新規生成するか `icons/stats/` を流用するか | 選択が記録され、`NodeIconPathResolverTest` が実在パスを返す | 実装判断 | SOUL-03 着手時 | **SOUL-03 の完了条件そのもの**。新規生成なら G1 待ちになるため、暫定流用で先に通す二段構えも可 |
| プロンプト全文を台帳スキーマへ含めるか別置きか | `asset-provenance.schema.json` に項目があるか、別台帳のパスが決まっている | 実装判断 | ASSET-01 着手時 | スキーマ外だと `AssetProvenanceTest` が素通りする |
| 図鑑サムネを 40×56 へ直すか | `CardCollectionScreen` の定数が 40×56 になり、比率 0.714 で歪まない | 実装判断 | CARD-04 | 現行 32×42 は比率も倍率も合わず歪む |

> **本表は SOUL-03 完了条件の「選択の記録」を兼ねる。** 刻印アイコンの生成は G1 通過後になるため、開発を止めないなら暫定で `icons/stats/` の既存素材を流用してテストを通し、G1 後に差し替える運用が安全。
