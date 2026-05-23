# 素材ガイドライン

DDD-Jyogi-Kuroto-F で使用する **アセット (フォント・画像・音)** の収集・取り込み・ライセンス管理ルール。
仕様の詳細は [GAME_DESIGN.md §8 AI 使用方針](./GAME_DESIGN.md) も参照。

> **制作仕様** (寸法・配置パス・命名・取り込み手順) はチームメイト向けの単一ドキュメント
> [docs/AssetProductionSpec.md](./AssetProductionSpec.md) に集約。本書は**取得・ライセンス管理**に
> フォーカスする。

---

## 1. 基本方針

1. **使う前にライセンスを確認する**。確認できない素材は使わない
2. **クレジット表記を 1 か所に集約する** ([LICENSES/INDEX.md](../LICENSES/INDEX.md))
3. **改変したら出典と改変点を残す**
4. **AI 生成物も「制作者が誰か」「学習元規約に違反していないか」をチェックする**
5. **配布物に含めるのは商用可ライセンスのみ**（提出時に整理）

---

## 2. 素材カテゴリと推奨ソース

| カテゴリ | 推奨ソース | 代表ライセンス | 備考 |
|---|---|---|---|
| 日本語フォント | [Google Fonts (DotGothic16)](https://fonts.google.com/specimen/DotGothic16) / [Noto Sans JP](https://fonts.google.com/noto/specimen/Noto+Sans+JP) / [源真ゴシック](http://jikasei.me/font/genshin/) | SIL OFL 1.1 | 再配布・改変・商用可。本プロジェクトのピクセル方針には DotGothic16 が好相性 |
| 英語フォント | LibGDX 同梱 BitmapFont / [Google Fonts](https://fonts.google.com/) | OFL / Apache-2.0 | デフォルトでも MVP は足りる |
| ピクセルタイル | [Kenney.nl](https://kenney.nl/) / [OpenGameArt.org](https://opengameart.org/) | CC0 中心 | CC0 を最優先 |
| キャラスプライト | [Kenney.nl 1-Bit Pack](https://kenney.nl/assets/1-bit-pack) / [OpenGameArt.org](https://opengameart.org/) | CC0 / CC-BY | アニメーション付きを優先 |
| UI 装飾 | [Kenney UI Pack](https://kenney.nl/assets/ui-pack) | CC0 | 9-patch 形式が便利 |
| 効果音 (SE) | [freesound.org](https://freesound.org/) / [効果音ラボ](https://soundeffect-lab.info/) | CC0 / CC-BY / 効果音ラボ規約 | 短く軽い ogg を選ぶ |
| BGM | [DOVA-SYNDROME](https://dova-s.jp/) / [魔王魂](https://maou.audio/) | サイト固有規約 | クレジット要件を必ず確認 |
| AI 生成画像 | GPT-4 image / Stable Diffusion / NovelAI | プロジェクト判断 | [GAME_DESIGN §8](./GAME_DESIGN.md) の方針に従う |
| AI 生成音声 | VOICEVOX / Coefont / Style-Bert-VITS2 | 各キャラの規約 | キャラ別の利用範囲に注意 |

> **避けるべきソース**: ライセンス表記が無い / 出典不明 / Pinterest や Twitter からの直接 DL / 「フリー素材」とだけ書いてあるサイト。

---

## 3. ライセンス区分の優先順位

1. **CC0 (Public Domain)** — クレジット不要、無制約。MVP では最優先
2. **SIL OFL (フォント専用)** — 再配布・改変可。クレジット維持必須
3. **CC-BY 4.0** — クレジット必須。配布物に含めるのは可
4. **CC-BY-SA 4.0** — 派生物も同ライセンス。プロジェクト全体に伝染するので慎重
5. **CC-BY-NC** — 非商用のみ。**ハッカソンの賞金等が絡む場合は不可**、判断する
6. **独自規約** — サイトの利用規約を全読してから採用

---

## 4. ディレクトリ配置

```
assets/                       # ゲーム実行時に読まれるリソース (build.gradle で sourceSets に追加)
├── fonts/
│   ├── NotoSansJP-Regular.otf
│   └── README.md             # フォント別の取得元・バージョンメモ
├── tiles/
│   └── dungeon_tileset.png
├── sprites/
│   ├── player_idle.png
│   └── slime.png
├── ui/
│   └── frame_9patch.png
└── audio/
    ├── bgm/
    │   └── dungeon_theme.ogg
    └── se/
        ├── attack.ogg
        └── damage.ogg

LICENSES/                     # ライセンス文書置き場 (リポジトリにコミット)
├── INDEX.md                  # 素材→ライセンスのインデックス
├── NotoSansJP_OFL.txt        # SIL OFL 1.1 の原文
└── KenneyAssets_CC0.txt      # Kenney 素材の CC0 ステートメント
```

> `assets/` は MVP 段階で空。素材を入れる時に `build.gradle` の
> `sourceSets.main.resources.srcDirs` に `'assets'` を追加する。

---

## 5. LICENSES/INDEX.md の書き方

1 素材 1 行で、以下のフォーマットを守る。

```
| ファイル | 取得日 | 取得元 URL | ライセンス | クレジット | 改変 |
|---|---|---|---|---|---|
| assets/fonts/NotoSansJP-Regular.otf | 2026-05-12 | https://fonts.google.com/noto/specimen/Noto+Sans+JP | SIL OFL 1.1 | © Google LLC | なし |
| assets/tiles/dungeon_tileset.png | 2026-05-14 | https://kenney.nl/assets/1-bit-pack | CC0 | © Kenney (任意) | 32x32 にリサンプル |
```

- **取得日** は実際に DL した日 (素材バージョン特定の手がかり)
- **クレジット** は表記が必須なら必ず原文どおり、不要なら "Optional"
- **改変** は「リサイズ・色変更・トリミング」など具体的に

---

## 6. LibGDX への取り込み手順

### 6-1. フォント (.ttf / .otf)

LibGDX 標準の `BitmapFont` は英語のみ。日本語が必要な場合は **gdx-freetype** 拡張を使う。

```gradle
implementation "com.badlogicgames.gdx:gdx-freetype:${gdxVersion}"
runtimeOnly    "com.badlogicgames.gdx:gdx-freetype-platform:${gdxVersion}:natives-desktop"
```

```java
FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/NotoSansJP-Regular.otf"));
FreeTypeFontParameter param = new FreeTypeFontParameter();
param.size = 16;
param.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "あいうえおかきく…"; // 必要文字を列挙
BitmapFont font = gen.generateFont(param);
gen.dispose();
```

- フォント生成は **重い**。Screen の `show()` で 1 回だけ生成して使い回す。`dispose()` を忘れない
- HUD で使う日本語文字を `characters` に明示しないと描画されない

### 6-2. 画像

- 単発: `new Texture(Gdx.files.internal("tiles/floor.png"))`
- まとめ: `TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("tiles/dungeon.atlas"));`
- 9-patch UI: `NinePatch np = new NinePatch(new Texture(...), 4, 4, 4, 4);`

### 6-3. 音

- 短い SE: `Sound se = Gdx.audio.newSound(Gdx.files.internal("audio/se/attack.ogg"));`
- BGM: `Music bgm = Gdx.audio.newMusic(Gdx.files.internal("audio/bgm/dungeon_theme.ogg")); bgm.setLooping(true);`

### 6-4. AssetManager (MVP 後の集約)

リソースが増えてきたら `AssetManager` で一括ロード・参照に切り替える。MVP では直接ロード可。

```java
AssetManager am = new AssetManager();
am.load("tiles/floor.png", Texture.class);
am.load("audio/se/attack.ogg", Sound.class);
am.finishLoading();
```

---

## 7. AI 生成物の取り扱い

[GAME_DESIGN §8](./GAME_DESIGN.md) のうち本ドキュメントで補足する点:

- AI 生成画像 (GPT 等): 生成プロンプトを `LICENSES/INDEX.md` のクレジット欄に明記する
- 商用利用可否は **そのサービスの利用規約** に従う
- AI 生成音声 (VOICEVOX 等): キャラ単位で **クレジット表記が義務付けられる** ことが多い。必ず読む
- 学習元が不明な無料素材は、ハッカソン提出後の公開段階で **差し替えやすい構造** で実装する (= AssetManager 経由で参照しておけば置き換えが楽)

---

## 8. 削除・差し替えの手順

ライセンス疑義が発覚した素材を消す場合:

1. `assets/` から削除
2. `LICENSES/INDEX.md` から該当行を削除
3. `LICENSES/<該当>.txt` も削除
4. PR で差し替え理由を書く
5. 過去コミットに残るのは許容 (ライセンス疑義の素材だけだったコミットは履歴から除外を検討)

---

## 9. チェックリスト (PR 作成前)

- [ ] `LICENSES/INDEX.md` に新規素材の行を追加した
- [ ] ライセンス本文を `LICENSES/<名前>.txt` に置いた (CC0 以外)
- [ ] クレジット表記が必要な場合、ゲーム内クレジット画面 or README に反映予定がある
- [ ] 商用利用可否を確認した (ハッカソン提出時)
- [ ] AI 生成物は学習元規約と照らした
- [ ] バイナリは `.gitattributes` で `binary` 指定されている (大きい場合)
