# 全59枚カード画像生成マニフェスト ＆ 機械自動検証基盤仕様書

## 1. 画像生成・受入基準 (Acceptance Criteria & Validation Rules)

生成する全カード画像は、以下の制約と仕様に 100% 適合しなければならない。本規格の遵守は [`CardImageValidatorTest.java`](file:///c:/.program/DDD-Jyogi-Kuroto-F/.claude/worktrees/consistency-sprint-batch1/src/test/java/core/presentation/render/CardImageValidatorTest.java) により自動機械判定される。

- **画像サイズ・形式**: `240 × 336` ピクセル (RGBA 透過 PNG)
- **色数制限**: 可視（Alpha > 0）ピクセルのユニーク色数が **24 色以下** であること (ドット絵風・軽量化)
- **四隅透過**: 四隅 `(0,0)`, `(239,0)`, `(0,335)`, `(239,335)` ピクセルが完全透過 (`Alpha == 0`)
- **禁止要素**: 文字・アルファベット・数字、フレーム/外枠、人物の顔・肉体、水平地平線、透過残りの純粋マゼンタ (`#FF00FF`)
- **レイアウト予約帯**:
  - 上部 `240 × 240` px: アート主領域
  - 下部 `96` px (y=240〜335): カードテキスト枠で覆われるため、描画オブジェクトの侵入ゼロ（完全透過領域）

---

## 2. 全59枚カード画像生成一覧 (Card Image Manifest Catalog)

| No | Card ID | Asset Name | 日本語名 | Tag | Element | AP | Rarity | アートプロンプト・モチーフ方針 (文字・枠・人物排除) | ステータス |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `blaze_nova` | `card_56` | 爆炎 | ATTACK | MAGICAL | 3 | RARE | Expanding fiery nova explosion with orange and magenta sparks | 第1バッチ済 (Review) |
| 2 | `meteor_drop` | `card_19` | 隕石落とし | ATTACK | MAGICAL | 3 | RARE | Giant flaming meteor falling with smoky trail | 第1バッチ済 (Review) |
| 3 | `overhead_smash` | `card_06` | 振り下ろし | ATTACK | PHYSICAL | 3 | UNCOMMON | Heavy iron warhammer smashing down with impact shockwave | 第1バッチ済 (v2 Review) |
| 4 | `teleport` | `card_30` | 瞬間移動 | MOVEMENT | MAGICAL | 3 | UNCOMMON | Arcane portal with swirling purple magic particles | 第1バッチ済 (Review) |
| 5 | `zangeki` | `card_51` | 斬撃 | ATTACK | PHYSICAL | 1 | COMMON | Sharp diagonal steel sword slash effect with glowing light | レビュー隔離済 |
| 6 | `strong_strike` | `card_52` | 強打 | ATTACK | PHYSICAL | 1 | COMMON | Heavy blunt strike impact with glowing force lines | 未生成 |
| 7 | `magic_bolt` | `card_53` | 魔法弾 | ATTACK | MAGICAL | 2 | COMMON | Glowing cyan energy orb shooting forward | 未生成 |
| 8 | `fireball` | `card_54` | 火球 | ATTACK | MAGICAL | 2 | COMMON | Swirling orange fireball flying through air | 未生成 |
| 9 | `ember_shot` | `card_55` | 炎弾 | ATTACK | MAGICAL | 1 | COMMON | Small flame projectile with red sparks | 未生成 |
| 10 | `blink_step` | `card_57` | 瞬歩 | MOVEMENT | MAGICAL | 1 | COMMON | Speed lines with blurred blue afterimage step | 未生成 |
| 11 | `flame_circle` | `card_58` | 炎の魔法陣 | TRAP | MAGICAL | 2 | COMMON | Glowing red magic circle on ground with rising flames | 未生成 |
| 12 | `arcane_veil` | `card_59` | 魔力の帳 | BUFF | MAGICAL | 2 | COMMON | Shimmering violet barrier aura surrounding center | 未生成 |
| 13 | `stone_throw` | `card_01` | 礫投げ | ATTACK | PHYSICAL | 1 | COMMON | Flying jagged stone rock with motion blur lines | 未生成 |
| 14 | `heavy_slash` | `card_02` | 重斬り | ATTACK | PHYSICAL | 2 | COMMON | Large curved steel slash arc with red impact aura | 未生成 |
| 15 | `quick_stab` | `card_03` | 速突き | ATTACK | PHYSICAL | 1 | COMMON | Sharp thrust line of spear tip with speed streaks | 未生成 |
| 16 | `double_strike` | `card_04` | 二連撃 | ATTACK | PHYSICAL | 2 | COMMON | Twin crossed sword slash trails in golden energy | 未生成 |
| 17 | `armor_break` | `card_05` | 鎧砕き | ATTACK | PHYSICAL | 2 | COMMON | Shattering metal plate with impact cracks | 未生成 |
| 18 | `riposte` | `card_07` | 受け流し | ATTACK | PHYSICAL | 1 | COMMON | Parrying blade effect with sparks bursting outward | 未生成 |
| 19 | `leg_sweep` | `card_08` | 足払い | ATTACK | PHYSICAL | 1 | COMMON | Low sweeping arc of wind force near ground | 未生成 |
| 20 | `piercing_arrow` | `card_09` | 貫通矢 | ATTACK | PHYSICAL | 2 | COMMON | Glowing golden arrow shooting straight with trail | 未生成 |
| 21 | `skull_crack` | `card_10` | 頭蓋割り | ATTACK | PHYSICAL | 3 | UNCOMMON | Heavy mace strike with downward shockwave radiating | 未生成 |
| 22 | `ice_lance` | `card_11` | 氷槍 | ATTACK | MAGICAL | 2 | COMMON | Sharp crystalline ice spear flying with frost dust | 未生成 |
| 23 | `thunder_bolt` | `card_12` | 雷撃 | ATTACK | MAGICAL | 2 | COMMON | Jagged yellow lightning bolt striking down | 未生成 |
| 24 | `dark_pulse` | `card_13` | 暗黒波 | ATTACK | MAGICAL | 2 | COMMON | Expanding dark purple energy wave with shadow particles | 未生成 |
| 25 | `wind_blade` | `card_14` | 風刃 | ATTACK | MAGICAL | 1 | COMMON | Crescent green vacuum blade slicing air | 未生成 |
| 26 | `frost_nova` | `card_15` | 凍結爆発 | ATTACK | MAGICAL | 3 | RARE | Bursting icy explosion spreading frost crystals outward | 未生成 |
| 27 | `void_strike` | `card_16` | 虚無撃 | ATTACK | MAGICAL | 3 | RARE | Swirling black void rift collapsing inward with light | 未生成 |
| 28 | `acid_splash` | `card_17` | 酸の飛沫 | ATTACK | MAGICAL | 1 | COMMON | Corrosive green acid droplets splashing in arc | 未生成 |
| 29 | `holy_bolt` | `card_18` | 聖光弾 | ATTACK | MAGICAL | 2 | COMMON | Radiating radiant white light sphere shooting forward | 未生成 |
| 30 | `shadow_bolt` | `card_20` | 影の矢 | ATTACK | MAGICAL | 1 | COMMON | Dark shadowy projectile with purple wisps | 未生成 |
| 31 | `dash` | `card_21` | ダッシュ | MOVEMENT | PHYSICAL | 1 | COMMON | Forward motion wind lines with dust trail | 未生成 |
| 32 | `leap` | `card_22` | 跳躍 | MOVEMENT | PHYSICAL | 1 | COMMON | Upward curving arc trail with air distortion | 未生成 |
| 33 | `roll` | `card_24` | 回避転がり | MOVEMENT | PHYSICAL | 1 | COMMON | Circular motion trail with ground dust cloud | 未生成 |
| 34 | `shadow_step` | `card_25` | 影歩き | MOVEMENT | MAGICAL | 2 | COMMON | Dark wisp footprints teleporting across floor | 未生成 |
| 35 | `charge` | `card_26` | 突撃 | MOVEMENT | PHYSICAL | 2 | COMMON | Powerful forward bull rush aura with ground cracks | 未生成 |
| 36 | `wind_walk` | `card_28` | 風走り | MOVEMENT | MAGICAL | 1 | COMMON | Whirling green breezes flowing forward | 未生成 |
| 37 | `retreat` | `card_29` | 後退 | MOVEMENT | PHYSICAL | 1 | COMMON | Backward leap trail with fading footprint sparks | 未生成 |
| 38 | `iron_skin` | `card_31` | 鉄の皮膚 | BUFF | PHYSICAL | 1 | COMMON | Metallic silver shield aura surrounding center | 未生成 |
| 39 | `battle_cry` | `card_32` | 鬨の声 | BUFF | PHYSICAL | 1 | COMMON | Soundwave rings radiating outward in golden light | 未生成 |
| 40 | `focus` | `card_33` | 集中 | BUFF | MAGICAL | 1 | COMMON | Converging blue energy particles forming aura | 未生成 |
| 41 | `magic_barrier` | `card_34` | 魔法障壁 | BUFF | MAGICAL | 2 | COMMON | Hexagonal blue energy barrier shield emblem | 未生成 |
| 42 | `swiftness` | `card_35` | 疾風 | BUFF | PHYSICAL | 1 | COMMON | Feather-like green wind wings boosting speed | 未生成 |
| 43 | `stone_wall` | `card_60` | 岩壁の構え | BUFF | PHYSICAL | 2 | COMMON | Solid stone barrier wall icon with earth aura | 未生成 |
| 44 | `berserker` | `card_36` | 狂戦士 | BUFF | PHYSICAL | 2 | UNCOMMON | Burning red rage flames bursting upward | 未生成 |
| 45 | `arcane_surge` | `card_37` | 魔力放出 | BUFF | MAGICAL | 2 | UNCOMMON | Intense blue arcane lightning pillar surging | 未生成 |
| 46 | `haste` | `card_61` | ヘイスト | BUFF | MAGICAL | 2 | UNCOMMON | Golden clockwork wheel turning with speed lines | 未生成 |
| 47 | `fortify` | `card_38` | 要塞化 | BUFF | PHYSICAL | 3 | RARE | Castle fortress emblem with impenetrable golden shield | 未生成 |
| 48 | `mana_shield` | `card_39` | マナシールド | BUFF | MAGICAL | 1 | COMMON | Translucent cyan bubble shield glowing around center | 未生成 |
| 49 | `war_stance` | `card_40` | 戦闘態勢 | BUFF | PHYSICAL | 1 | COMMON | Crossed swords icon in glowing orange aura | 未生成 |
| 50 | `spike_trap` | `card_41` | 棘罠 | TRAP | PHYSICAL | 2 | COMMON | Sharp iron spikes bursting out of ground pit | 未生成 |
| 51 | `bear_trap` | `card_42` | 熊罠 | TRAP | PHYSICAL | 2 | COMMON | Heavy steel jaw trap armed on wooden floor | 未生成 |
| 52 | `poison_needle` | `card_43` | 毒針罠 | TRAP | PHYSICAL | 2 | COMMON | Green dripping poison dart trap mechanism | 未生成 |
| 53 | `pitfall` | `card_44` | 落とし穴 | TRAP | PHYSICAL | 3 | UNCOMMON | Dark deep hole pit in stone floor with crumbling edges | 未生成 |
| 54 | `wire_trip` | `card_45` | 鉄線罠 | TRAP | PHYSICAL | 1 | COMMON | Taut steel tripwire stretched across ground with sparks | 未生成 |
| 55 | `frost_rune` | `card_46` | 氷結ルーン | TRAP | MAGICAL | 2 | COMMON | Glowing blue ice rune seal carved into ground | 未生成 |
| 56 | `thunder_rune` | `card_47` | 雷撃ルーン | TRAP | MAGICAL | 2 | COMMON | Yellow electric rune symbol crackling with sparks | 未生成 |
| 57 | `void_rune` | `card_48` | 虚無ルーン | TRAP | MAGICAL | 3 | RARE | Dark void emblem floating above ground with purple swirl | 未生成 |
| 58 | `acid_pool` | `card_49` | 酸の水たまり | TRAP | MAGICAL | 2 | COMMON | Bubbly green acidic puddle corroding floor | 未生成 |
| 59 | `dark_snare` | `card_50` | 闇の罠 | TRAP | MAGICAL | 1 | COMMON | Shadowy tentacles rising from dark floor circle | 未生成 |

---

## 3. 自動検証基盤の実行・合否確認コマンド

本基盤における全画像の合否判定および新画像の検証は、以下のコマンドを実行することでいつでも全自動で判定される：

```bash
.\gradlew.bat --no-daemon check
```
