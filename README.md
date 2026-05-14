# DDD-Jyogi-Kuroto-F

> **「誰でも | どれでも | どんな | 場所でも」 動くゲーム** — *Doko-demo Rogue*

Java 単体で **PC・スマホで動く** 2D ピクセルアートのローグライト。
死を繰り返して強くなる、3〜5分の最小ループ。

---

## What is this?

部内ハッカソンの定番フォーマット（JS + Web）を外し、**Java でマルチプラットフォーム対応** に挑戦する作品。
LibGDX を採用し、Windows / macOS / Linux / Android で「どこでも動く」を体現する。

> *(スクリーンショット差し替え予定)*

---

## コア体験

- **「死を繰り返して強くなる」** — ローグライク × メタ進行のハイブリッド
- 1プレイ **3〜5分** で完結する高密度ループ
- **タルコフ風死亡継承**: ソウル / スキル / スキル枠は持ち帰り、金貨 / 装備は喪失。生還=大儲け、死亡=大損
- 永続強化と難易度上昇のせめぎ合いがリプレイ性の核

## 主要システム

| システム | 概要 | ステージ |
|---|---|---|
| **AP制バトル** | 行動ポイント (= 速度ステ) の変動ターン制。**MVP はスキル枠 4 個装着型、MVP 後はカード式デッキ (§15-3) と併存** | MVP ✓ |
| **動的生成ダンジョン** | マップ / 敵配置 / アイテム / UI を段階的に動的化 | MVP ✓ |
| **ソウル経済** | ソウル消費でステ・カードを永続強化 (死後の編成画面) | MVP ✓ |
| **カード式デッキ構築** | スキル枠と併存。タグ × 属性で 8 サブカテゴリ、AP コストでプレイ | MVP 後 §15-3 |
| **多層ダンジョン + ノード分岐** | 初期 3 層 (ソウルで拡張)、Slay the Spire 風の層内マップ | MVP 後 §15-6 |
| **装備システム** | ステ補正 + UI テーマ変動 + 装備固有カードの自動追加 | MVP 後 §15-9 |
| **ソウルツリー** | 円樹形の永続強化ツリー、経路選択で暗黙的に職業が決まる | MVP 後 §15-7 |
| **ポップアップ式 UI** | FancyMenu Mod 的サブウィンドウ方式、基本解像度 1920×1080 | MVP 後 §15-1 |

---

## 対象環境

- **Desktop**: Windows / macOS / Linux （必須）
- **Android**: MVP 後に対応（必須）
- Web / iOS: スコープ外

---

## 技術スタック

- **Java 25 LTS** (Oracle JDK 25)
- **LibGDX** + Gradle (Wrapper 同梱)
- IntelliJ IDEA Community
- Google Java Style Guide + Spotless / google-java-format
- Jackson / Gson / JUnit 5

---

## 開発状況

部内ハッカソン作品。**今週金曜 MVP** → **来週土日に本番** 提出予定。
**§15「MVP 後の機能仕様」** を本番提出までに段階実装する方針。

MVP コア実装 (1 階層 / AP 制戦闘 / 死亡時ソウル保持 / 階段踏破でクリア) は完成済み。詳細な進行は [tasks/todo.md](tasks/todo.md) を参照。

## ローカル開発

### 1. 前提

| 必須 | 内容 | 取得 |
|---|---|---|
| JDK | **Oracle JDK 25 LTS** | <https://www.oracle.com/java/technologies/downloads/> |

Gradle 9.5.0 の wrapper (`gradlew` / `gradlew.bat` / `gradle/wrapper/`) はリポジトリに同梱済み。`gradle` コマンドを別途インストールする必要は無い。

### 2. 初回セットアップ

```bash
git clone https://github.com/NPCdotcom/DDD-Jyogi-Kuroto-F.git
cd DDD-Jyogi-Kuroto-F
java --version  # => 25.x.x が表示されることを確認
```

JDK 25 が PATH に通っていれば、次の `gradlew` 経由で必要な依存をすべて自動取得する。

### 3. 実行

```bash
# ゲーム起動 (Desktop)
./gradlew run            # macOS / Linux
.\gradlew.bat run        # Windows (PowerShell — `.\` 必須)
gradlew.bat run          # Windows (cmd.exe)

# ドメイン層テスト
./gradlew test

# フォーマット適用 (Spotless + google-java-format)
./gradlew spotlessApply

# 配布用 JAR
./gradlew fatJar         # build/libs/*-all.jar
```

### 4. 日本語フォント (任意)

HUD・タイトルを日本語表示にしたい場合は [DotGothic16](https://fonts.google.com/specimen/DotGothic16) を `assets/fonts/DotGothic16-Regular.ttf` に配置する (約 2 MB)。未配置でも英語 UI で起動する。
詳細は [assets/fonts/README.md](assets/fonts/README.md) と [docs/AssetGuidelines.md](docs/AssetGuidelines.md) を参照。

### 5. 操作キー

| キー | 操作 |
|---|---|
| `W` / `A` / `S` / `D`, 矢印キー | 1 マス移動 (AP 1 消費) |
| `1` / `2` / `3` / `4` | スキル枠の発動 (隣接した敵に対し) |
| `SPACE` | 待機 (AP 1 消費) |
| `ENTER` | ターン終了 (AP を持ち越して敵ターンへ) |

AP を使い切ると自動でターン終了。階段 `>` に到達すると CLEARED。HP 0 で GAME OVER。

## 詳細ドキュメント

- **ゲーム仕様の確定書（Single Source of Truth）**: [docs/GAME_DESIGN.md](docs/GAME_DESIGN.md)
  - §11: MVP 定義 (今週金曜の最小達成ライン)
  - **§15: MVP 後の機能仕様** (本番対応の指針 — カード / 層構造 / ソウルツリー / 装備 / ポップアップ UI など)
- ドキュメント一覧: [docs/INDEX.md](docs/INDEX.md)
- 開発者ガイド: [docs/ContributingGuide.md](docs/ContributingGuide.md)
- ブランチ戦略: [docs/BranchingStrategy.md](docs/BranchingStrategy.md)
- 実装タスクと設計原則: [tasks/todo.md](tasks/todo.md)

---

## ライセンス

提出時に決定。
