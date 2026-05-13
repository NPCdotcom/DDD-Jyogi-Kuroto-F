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

## 詳細ドキュメント

- **ゲーム仕様の確定書（Single Source of Truth）**: [docs/GAME_DESIGN.md](docs/GAME_DESIGN.md)
  - §11: MVP 定義 (今週金曜の最小達成ライン)
  - **§15: MVP 後の機能仕様** (本番対応の指針 — カード / 層構造 / ソウルツリー / 装備 / ポップアップ UI など)
- ドキュメント一覧: [docs/INDEX.md](docs/INDEX.md)
- 開発者ガイド: [docs/ContributingGuide.md](docs/ContributingGuide.md)
- ブランチ戦略: [docs/BranchingStrategy.md](docs/BranchingStrategy.md)

---

## ライセンス

提出時に決定。
