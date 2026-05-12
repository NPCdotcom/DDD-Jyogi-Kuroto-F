# assets/fonts/

ゲーム内で使う日本語フォントの配置場所。Git にコミットしないバイナリの取扱規程は
[../../docs/AssetGuidelines.md](../../docs/AssetGuidelines.md) を参照。

---

## 採用フォント

| ファイル名 | 用途 | ライセンス | 備考 |
|---|---|---|---|
| `DotGothic16-Regular.ttf` | HUD・タイトル・ログの日本語表示 | SIL OFL 1.1 ([../../LICENSES/DotGothic16_OFL.txt](../../LICENSES/DotGothic16_OFL.txt)) | 16 ドットのピクセル日本語フォント。本プロジェクトの 2D ピクセル方針と相性が良い |

このファイルが置かれていれば日本語 UI、無ければ英語 UI (デフォルト BitmapFont) で起動する。
フォント有無は実行時に `presentation.render.Fonts` が `Gdx.files.internal("fonts/DotGothic16-Regular.ttf").exists()` で判定する。

---

## 取得手順

### 公式 Google Fonts (推奨)

1. <https://fonts.google.com/specimen/DotGothic16> を開く
2. 右上 **Get font** → **Download all** で ZIP を取得
3. 解凍した `static/DotGothic16-Regular.ttf` (または `DotGothic16-Version{x.x}/fonts/ttf/DotGothic16-Regular.ttf`) をこのディレクトリにコピー
4. 同梱の `OFL.txt` を `../../LICENSES/DotGothic16_OFL.txt` にコピー

### 配置後にやること

1. `LICENSES/INDEX.md` に DotGothic16 のエントリを追加
   (`docs/AssetGuidelines.md §5` のフォーマット参照)
2. `./gradlew run` で起動し、HUD が日本語ドット表記になっているか確認

---

## 描画上の注意

- DotGothic16 は 16 ドット格子のフォント。`Fonts.java` では Nearest フィルタを使ってアンチエイリアスを無効化し、ドット感を維持している
- サイズも 16 / 32 / 48 と 16 の倍数を採用 (pixel-perfect)。中途半端な倍率にすると粒子のサイズが揃わず汚くなる
- カラーフィルタ (色付け) は OK。`font.setColor(...)` で問題なく色変えできる

---

## なぜ Git にコミットしないか

- フォントは 2MB 程度でリポジトリが膨らむ
- 配布ライセンス自体は OFL で可だが、各環境で取得するほうが「最新版」を保ちやすい
- どうしてもコミットしたい場合は Git LFS の導入を別途検討する
