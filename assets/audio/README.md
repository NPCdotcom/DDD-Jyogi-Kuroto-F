# 音声素材の配置ガイド (チームメイト向け)

`SoundManager` は起動時にこのディレクトリから音声ファイルを読み込む。
**ファイルが無い種別は自動的に無音 (no-op) になる** — 揃っていなくてもゲームは正常に動く。
素材ができ次第、下記のファイル名・パスで投入すれば自動で鳴る。

> **形式・音量目安・将来追加候補**の詳細は [docs/AssetProductionSpec.md §A-2](../../docs/AssetProductionSpec.md) を参照。
> 本ファイルは配置パスと種別の Single Source of Truth。

## 配置パス

```
assets/audio/
  bgm/
    title.ogg          … タイトル / メニュー系画面の BGM (ループ)
    dungeon.ogg        … ダンジョン戦闘の BGM (ループ)
  se/
    enemy_defeated.ogg … 敵撃破
    player_damaged.ogg … プレイヤー被ダメージ
    deal_damage.ogg    … プレイヤーが敵に与ダメージ
    card_used.ogg      … カード使用
    button.ogg         … ボタン操作 / 画面遷移 / 選択確定
    floor_advance.ogg  … 層遷移
```

## 形式

- BGM: `Music` で再生 (ストリーミング)。`.ogg` 推奨。
- SE: `Sound` で再生 (オンメモリ)。短い `.ogg` / `.wav`。
- 音量はゲーム内「設定」画面の BGM / SE スライダーで調整される (実ファイル側で正規化は不要)。

種別を増やす場合は `core/infrastructure/audio/BgmKind` / `SeKind` に enum 値を追加する。
