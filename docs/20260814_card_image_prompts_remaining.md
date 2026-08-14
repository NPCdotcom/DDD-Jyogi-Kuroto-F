# 残り54枚カード画像生成用プロンプト集 (Imagegen Prompt Collection)

## 1. 共通プロンプト仕様 & ネガティブ条件 (Common Specifications)

Dall-E 3 または `gpt-image 2.0` (imagegen) 呼出時、以下の共通ルールおよびネガティブ指示をすべてのプロンプト末尾に適用してください。

- **Style**: `2D pixel art sprite style, retro 16-bit game icon, vibrant limited color palette (max 24 colors)`
- **Composition**: `Centered composition on a solid pitch-black background (#000000) for easy transparency isolation, clear icon silhouette`
- **Exclusions (Negative Prompt)**: `NO text, NO letters, NO numbers, NO human figures, NO faces, NO UI frames, NO card borders, NO ground horizon line, NO lower text area overlap (keep bottom 30% area empty or dark)`

---

## 2. カテゴリ別 個別英文プロンプト一覧

### 【グループ 1: 物理・魔法攻撃系カード (21枚)】

| No | Card ID | 日本語名 | 属性/Tag | 推奨英文プロンプト (Prompt) |
|---|---|---|---|---|
| 1 | `strong_strike` | 強打 | PHYSICAL ATTACK | `2D pixel art icon of a powerful heavy punch impact forcefield with glowing golden shockwave rings, centered on a pitch black background, retro game style, limited color palette, clean silhouette, NO text, NO humans, NO UI frames, bottom 30% empty.` |
| 2 | `magic_bolt` | 魔法弾 | MAGICAL ATTACK | `2D pixel art icon of a glowing cyan arc energy bolt projectile flying upward right, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames, bottom 30% empty.` |
| 3 | `fireball` | 火球 | MAGICAL ATTACK | `2D pixel art icon of a fiery orange fireball projectile trailing sparks, centered on black background, retro 16-bit game sprite, limited color palette, NO text, NO humans, NO frames, bottom 30% empty.` |
| 4 | `ember_shot` | 炎弾 | MAGICAL ATTACK | `2D pixel art icon of a small red flame spark projectile with glowing embers, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 5 | `stone_throw` | 礫投げ | PHYSICAL ATTACK | `2D pixel art icon of a jagged brown stone rock flying with speed motion lines, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 6 | `heavy_slash` | 重斬り | PHYSICAL ATTACK | `2D pixel art icon of a thick crimson blade slash arc with glowing impact trail, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 7 | `quick_stab` | 速突き | PHYSICAL ATTACK | `2D pixel art icon of a sharp golden spear tip thrusting forward with sharp speed lines, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 8 | `double_strike` | 二連撃 | PHYSICAL ATTACK | `2D pixel art icon of twin crossed steel sword slash energy arcs in bright yellow, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 9 | `armor_break` | 鎧砕き | PHYSICAL ATTACK | `2D pixel art icon of a shattering steel chestplate plate cracking into pieces with sparks, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 10 | `riposte` | 受け流し | PHYSICAL ATTACK | `2D pixel art icon of a parrying sword guard spark collision burst with blue energy, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 11 | `leg_sweep` | 足払い | PHYSICAL ATTACK | `2D pixel art icon of a low sweeping wind crescent arc near ground level, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 12 | `piercing_arrow` | 貫通矢 | PHYSICAL ATTACK | `2D pixel art icon of a glowing sharp golden arrow soaring straight with trail, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 13 | `skull_crack` | 頭蓋割り | PHYSICAL ATTACK | `2D pixel art icon of a heavy iron mace head striking down with intense red shockwave, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 14 | `ice_lance` | 氷槍 | MAGICAL ATTACK | `2D pixel art icon of a sharp crystalline blue ice spear shooting forward with frost particles, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 15 | `thunder_bolt` | 雷撃 | MAGICAL ATTACK | `2D pixel art icon of a jagged bright yellow lightning bolt striking downward with sparks, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 16 | `dark_pulse` | 暗黒波 | MAGICAL ATTACK | `2D pixel art icon of an expanding dark purple shadowy energy wave with wisps, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 17 | `wind_blade` | 風刃 | MAGICAL ATTACK | `2D pixel art icon of a sharp green wind vacuum blade crescent cutting air, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 18 | `frost_nova` | 凍結爆発 | MAGICAL ATTACK | `2D pixel art icon of a massive ice crystal explosion bursting outward in cyan light, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 19 | `void_strike` | 虚無撃 | MAGICAL ATTACK | `2D pixel art icon of a swirling black void rift sphere absorbing purple light, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 20 | `acid_splash` | 酸の飛沫 | MAGICAL ATTACK | `2D pixel art icon of corrosive bright green acid droplets splashing in arc, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 21 | `holy_bolt` | 聖光弾 | MAGICAL ATTACK | `2D pixel art icon of a radiating holy white and gold light orb projectile, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 22 | `shadow_bolt` | 影の矢 | MAGICAL ATTACK | `2D pixel art icon of a dark shadow energy bolt with purple trail, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |

---

### 【グループ 2: 移動・能力強化系カード (18枚)】

| No | Card ID | 日本語名 | 属性/Tag | 推奨英文プロンプト (Prompt) |
|---|---|---|---|---|
| 23 | `blink_step` | 瞬歩 | MAGICAL MOVEMENT | `2D pixel art icon of speed motion lines with a blurred blue energy step afterimage, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 24 | `dash` | ダッシュ | PHYSICAL MOVEMENT | `2D pixel art icon of fast forward movement wind streaks with dust particles, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 25 | `leap` | 跳躍 | PHYSICAL MOVEMENT | `2D pixel art icon of an upward arc trajectory line with air distortion breeze, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 26 | `roll` | 回避転がり | PHYSICAL MOVEMENT | `2D pixel art icon of a spinning circular motion trail with dust cloud, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 27 | `shadow_step` | 影歩き | MAGICAL MOVEMENT | `2D pixel art icon of dark purple shadow wisps stepping across floor, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 28 | `charge` | 突撃 | PHYSICAL MOVEMENT | `2D pixel art icon of a heavy bull-like charging force aura in orange and red, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 29 | `wind_walk` | 風走り | MAGICAL MOVEMENT | `2D pixel art icon of swirling light green breezes flowing forward gracefully, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 30 | `retreat` | 後退 | PHYSICAL MOVEMENT | `2D pixel art icon of a backward leap motion trail with fading spark footprints, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 31 | `iron_skin` | 鉄の皮膚 | PHYSICAL BUFF | `2D pixel art icon of a metallic silver defense shield aura around center, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 32 | `battle_cry` | 鬨の声 | PHYSICAL BUFF | `2D pixel art icon of radiating golden soundwave rings expanding outward, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 33 | `focus` | 集中 | MAGICAL BUFF | `2D pixel art icon of converging blue energy specks forming a glowing aura, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 34 | `magic_barrier` | 魔法障壁 | MAGICAL BUFF | `2D pixel art icon of a translucent hexagonal blue energy barrier crest, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 35 | `swiftness` | 疾風 | PHYSICAL BUFF | `2D pixel art icon of green wind wings icon boosting speed, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 36 | `stone_wall` | 岩壁の構え | PHYSICAL BUFF | `2D pixel art icon of a solid stone wall barrier emblem with brown earth aura, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 37 | `berserker` | 狂戦士 | PHYSICAL BUFF | `2D pixel art icon of intense red rage flames bursting upward, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 38 | `arcane_surge` | 魔力放出 | MAGICAL BUFF | `2D pixel art icon of a powerful blue arcane energy surge pillar with sparks, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 39 | `haste` | ヘイスト | MAGICAL BUFF | `2D pixel art icon of a golden clockwork gear turning with speed lines, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 40 | `fortify` | 要塞化 | PHYSICAL BUFF | `2D pixel art icon of a majestic golden fortress castle shield emblem, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |

---

### 【グループ 3: 防御・罠系カード (15枚)】

| No | Card ID | 日本語名 | 属性/Tag | 推奨英文プロンプト (Prompt) |
|---|---|---|---|---|
| 41 | `mana_shield` | マナシールド | MAGICAL BUFF | `2D pixel art icon of a glowing cyan bubble shield around center, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 42 | `war_stance` | 戦闘態勢 | PHYSICAL BUFF | `2D pixel art icon of crossed orange energy swords emblem with attack aura, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 43 | `flame_circle` | 炎の魔法陣 | MAGICAL TRAP | `2D pixel art icon of a glowing red magic circle on ground with rising flames, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 44 | `arcane_veil` | 魔力の帳 | MAGICAL BUFF | `2D pixel art icon of a shimmering purple curtain veil of magical energy, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 45 | `spike_trap` | 棘罠 | PHYSICAL TRAP | `2D pixel art icon of sharp iron spikes popping up from ground, low angle view, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 46 | `bear_trap` | 熊罠 | PHYSICAL TRAP | `2D pixel art icon of a heavy steel jaw trap armed on floor, low angle view, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 47 | `poison_needle` | 毒針罠 | PHYSICAL TRAP | `2D pixel art icon of a green dripping poison needle trap mechanism, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 48 | `pitfall` | 落とし穴 | PHYSICAL TRAP | `2D pixel art icon of a dark pit hole in stone floor with cracked edges, low angle view, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 49 | `wire_trip` | 鉄線罠 | PHYSICAL TRAP | `2D pixel art icon of a tight steel tripwire stretched across ground with sparks, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 50 | `frost_rune` | 氷結ルーン | MAGICAL TRAP | `2D pixel art icon of a glowing icy blue geometric rune symbol on ground, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 51 | `thunder_rune` | 雷撃ルーン | MAGICAL TRAP | `2D pixel art icon of a yellow lightning rune symbol crackling on floor, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 52 | `void_rune` | 虚無ルーン | MAGICAL TRAP | `2D pixel art icon of a dark void emblem floating above ground with purple swirl, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 53 | `acid_pool` | 酸の水たまり | MAGICAL TRAP | `2D pixel art icon of a bubbly green acidic puddle corroding stone floor, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
| 54 | `dark_snare` | 闇の罠 | MAGICAL TRAP | `2D pixel art icon of shadowy tentacles rising from a dark floor circle, centered on black background, retro game style, limited color palette, NO text, NO humans, NO UI frames.` |
