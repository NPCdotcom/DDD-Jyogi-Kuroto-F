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

| ファイル | サイズ | 要件 |
|---|---|---|
| `assets/cards/frame/card_frame.png` | 300×420 透過 | イラストを透かす中央窓を持つ。AP バッジ位置 (左上)・レアリティ位置 (右上/下部)・名称帯 (下部) の**領域だけ**を用意し、文字は入れない |

> これは人間ゲート **G0** の承認対象。AI 生成ではなく制作担当が作る。

---

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

---

## 7. 未確定事項

| 項目 | 決める人 | 影響 |
|---|---|---|
| CARD-06 の重複カード 7 組を統合するか差別化するか | ユーザー | 統合するとイラスト枚数が 59 → 52 前後に減る |
| ステータスアイコン 6 枚を作り直すか現行を透過処理するか | ユーザー | 作り直しなら 6 枚追加、透過処理なら加工のみ |
| `icons/frame.png` が SOUL-02 後も必要か | 実装判断 | 不要なら `NodeIconPathResolver` から参照を消す |
