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

### ピクセルアートと 300×420 の相性

300×420 は 16-bit 風ピクセルアートの整数グリッドに乗らない。生成 AI は「ピクセル風に見える高解像度画像」を出すのが実態で、真のドット絵にはならない。

- **許容する**: 手札では 120×168 へ縮小するため、生成時に厳密なピクセルグリッドが揃っていなくても実害は小さい
- **揃えたい場合**: 60×84 で生成して 5 倍の最近傍拡大で 300×420 にする。ただし生成 AI は低解像度指定を苦手とするため、1 枚スパイクで品質を比較してから決める

### 検収基準 (1 枚ごとにこれで合否を出す)

| # | 基準 | 判定方法 |
|---|---|---|
| 1 | 文字・数字が 1 つも無い | 目視 |
| 2 | 枠・縁取りが無い | 目視 |
| 3 | 背景が完全透過 | 暗色背景に重ねて矩形が出ないこと |
| 4 | 300×420 ちょうど | ファイル情報 |
| 5 | 上下左右 24px のセーフエリアに主題が掛かっていない | ガイド重ね |
| 6 | 120×168 へ縮小してもモチーフが判別できる | 縮小表示で確認 |
| 7 | 同バッチ 4 枚の色調・線の太さが揃っている | 接触シートで比較 |

---

## 1. 出力仕様

| 項目 | 値 |
|---|---|
| キャンバス | **300 × 420 px** (縦長、実行時に手札で 120×168 へ等比縮小) |
| 背景 | **完全透過 (アルファ 0)**。PNG-32 |
| セーフエリア | 上下左右 **各 24 px** は空ける。フレームと AP バッジが重なる領域 |
| 主題の配置 | 中央やや上寄り。下部 **120 px** はテキスト帯が重なるので、重要な要素を置かない |
| 形式 | PNG (可逆)。JPEG 不可 (透過が失われる) |
| ファイル名 | `art/<cardId>.png` (例: `art/fireball.png`)。連番 `card_NN` ではなく **カード ID** を使う |

> 現行画像は 133×171 〜 807×1186 まで世代ごとにバラバラで、縦横比も線の太さも揃っていない。今回は全 59 枚を同一キャンバスで作る。

### 既存仕様書との関係 (重要)

[docs/AssetProductionSpec.md](AssetProductionSpec.md) §A-3 は **150×188 px** を「実測値」として定めている。本ブリーフの 300×420 は**それを置き換える**。

- 150×188 (縦横比 0.798) と手札描画 120×168 (0.714) は**比率が違う**ため、現行画像は実行時に必ず歪んでいた
- 300×420 は 120×168 の**ちょうど 2.5 倍**で比率が一致する (300/420 = 120/168 = 0.714)
- CARD-05 完了後に `AssetProductionSpec.md` §A-3 を 300×420 へ改訂し、150×188 の記述を削除すること。両方が生きている状態を残さない

### ソウルツリー上での表示 (縦長のままでは歪む)

`SoulTreeScreen.NODE_TEX_SIZE = 64f` により、CardGrant ノードは **64×64 の正方形**でカード画像を描く。縦長 300×420 をそのまま入れると横に潰れる。

対応は次のいずれかを CARD-04 で決める。本ブリーフでは**イラスト側は 300×420 のみを作る**前提とし、正方形版を別途生成させない。

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

### 強度の目安 (効果値をモチーフの派手さへ反映)

数値そのものは描かないが、**大きい効果ほど画面占有と発光を強く**して視覚的な序列を作る。

| 効果値 | 表現 |
|---|---|
| 1〜3 | 小さく静かな表現。単一の要素 |
| 4〜6 | 標準。中程度の発光・軌跡 |
| 7〜10 | 大きく激しい表現。強い発光、破片や衝撃波 |

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

**既存資産あり**: `assets/icons/cards/card_frame.png` が **96×112 px** で実在する。ただし 300×420 に対して小さすぎ、縦横比も 0.857 で 0.714 と合わないため、**そのままでは使えない**。

| ファイル | サイズ | 扱い |
|---|---|---|
| `assets/icons/cards/card_frame.png` (既存) | 96×112 | 参照用として残す。本番経路では使わない |
| 新フレーム (パスは実装時に確定) | 300×420 透過 | イラストを透かす中央窓を持つ。AP バッジ (左上)・レアリティ (右上/下部)・名称帯 (下部) の**領域だけ**を用意し、文字は入れない |

> これは人間ゲート **G0** の承認対象。AI 生成ではなく制作担当が作る。既存 96×112 を拡大流用すると 2.5 倍以上の拡大でぼやけるため、**新規に 300×420 で描き起こすこと**。

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

### 生成後に台帳へ記録する項目 (ASSET-01)

各画像について: ファイル名 / 生成サービス / モデル / 生成日 / プロンプト全文 / 人間加筆の有無と担当 / ライセンス / 再配布条件 / 原本ハッシュ。

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
| 1 | フレーム 1 枚を制作 (300×420) | **G0** 承認 |
| 2 | 生成サービスの権利条件を確定 | **G1** 承認 |
| 3 | `zangeki` 1 枚だけ生成し、フレーム + 動的テキストで合成して 4 画面 (手札 / 選択 / 図鑑 / ソウルツリー) を確認 | **G2** 承認 |
| 4 | 残り 58 枚を **4 枚ずつ** 生成し、接触シートで確認 | **G4** 各バッチ |
| 5 | 旧画像を本番経路から外す | **G5** 承認 |

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

| 項目 | 決める人 | 影響 |
|---|---|---|
| CARD-06 の重複カード 7 組を統合するか差別化するか | ユーザー | 統合するとイラスト枚数が 59 → 52 前後に減る |
| ステータスアイコン 6 枚を作り直すか現行を透過処理するか | ユーザー | 作り直しなら 6 枚追加、透過処理なら加工のみ |
| `icons/frame.png` が SOUL-02 後も必要か | 実装判断 | 不要なら `NodeIconPathResolver` から参照を消す |
