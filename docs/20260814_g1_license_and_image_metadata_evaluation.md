# G1 ゲート評価および画像生成メタデータ記録

## 概要

本書は、全 59 枚のカード画像（初回パイロット 5 枚 ＋ 追加生成 54 枚）に関する G1 人間承認ゲート（ライセンス・商用利用・再配布条件の確認）のためのメタデータ評価記録および管理台帳である。

---

## 1. G1 ゲート評価方針

| 項目 | 評価内容 | 状態 |
|---|---|---|
| **生成サービス / モデル** | AI 画像生成プラットフォーム (Imagen 3 / 派生モデル) | 記録済み |
| **商用利用条件** | 生成物に対する利用権・商用利用許諾範囲の確認 | 人間承認待ち |
| **Git 公開リポジトリ再配布** | リポジトリ公開時のライセンス適合性確認 | 人間承認待ち |
| **原本退避** | `build/review/generated/` から repo 外バックアップディレクトリへ退避済み | ✅ 完了 |
| **コード描画枠との分離** | カード枠・文字・属性ヘッダーはコード描画（1x1 white texture）を採用し、画像は純粋アートのみ | ✅ 完了 |

---

## 2. 生成画像およびメタデータ管理

退避先: `generated_assets_backup/`（repo 外）

* **パイロットバッチ (5件)**:
  - `zangeki`, `blaze_nova`, `meteor_drop`, `overhead_smash` (v1/v2), `teleport`
* **一括ドラフトバッチ (54件)**:
  - 近接・攻撃系: `heavy_slash`, `quick_stab`, `double_strike`, `skull_crack`, `riposte`, `armor_break` 等
  - 遠隔・射撃系: `ember_shot`, `ice_lance`, `piercing_arrow`, `stone_throw` 等
  - 魔法・属性系: `fireball`, `frost_nova`, `arcane_surge`, `dark_pulse`, `holy_bolt` 等
  - 防御・支援系: `fortify`, `iron_skin`, `mana_shield`, `magic_barrier`, `arcane_veil`, `haste`, `berserker`, `focus` 等
  - 移動・罠系: `dash`, `roll`, `leap`, `retreat`, `blink_step`, `shadow_step`, `spike_trap`, `bear_trap`, `pitfall`, `frost_rune`, `dark_snare`, `acid_pool`, `acid_splash` 等

各画像には一意な provenance JSON（生成プロンプト、モデルシード、SHA-256 ハッシュ値）が付随して管理されている。

---

## 3. G1 承認後の Phase 7 昇格手順

1. **人間による G1 承認**: 商用利用および再配布規約の最終承認。
2. **`assets/cards/` への配置**: 承認された 240x336 / 120x168 アート画像を `assets/cards/art/` 配下に配置。
3. **`CardImageRegistry` の切替**: 実画像ロードおよび `CardRenderer.drawCardComposite` への接続。
4. **CI 検証の有効化**: `CardImageValidatorTest` にて本番画像全 59 件の解像度・破損チェックを実行。
