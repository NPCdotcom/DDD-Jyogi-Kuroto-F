# 技術選定メモ

> 確定した技術スタックの一覧。ゲーム仕様や採用理由の詳細は [GAME_DESIGN.md §3](./GAME_DESIGN.md) を参照。

## 採用
- **Java 25 LTS** (Oracle JDK 25)
- **LibGDX** (Desktop / Android backend)
- **Gradle** (Wrapper 同梱、`gradlew` でビルド)
- IntelliJ IDEA Community
- **Jackson / Gson**（シリアライズ）
- **JUnit 5**（テスト）
- **Spotless + google-java-format**（Lint/Format、CI 強制）

## 必要時に採用検討
- 物理エンジン: **Box2D**（LibGDX 同梱）
- ECS: **Ashley**（必要に応じて、自前で書く選択肢もあり）
- データ形式: JSON は Jackson / Gson、シンプルな設定はプロパティファイル

## 環境統一ルール
- JDK: **Oracle JDK 25 LTS**
- Gradle Wrapper: リポジトリにコミット
- 文字コード: UTF-8
- 改行コード: LF
- `.editorconfig` 配置済み（ルート直下）

## 検討から外したもの
- **Kotlin**: Java 縛りアピールのため不採用
- **JavaFX**: ゲーム用途で物足りない
- **Vulkan**: 難度過大（[GAME_DESIGN.md §3-2](./GAME_DESIGN.md)）
- **LibGDX HTML5 backend (Web)**: リスクが高いためスコープ外（[GAME_DESIGN.md §2-1](./GAME_DESIGN.md)）
- **iOS**: JVM 動作要件外

## ディレクトリ構成について
- 現状の `src/main/java/core/...` は [GAME_DESIGN.md §9](./GAME_DESIGN.md) のレイヤー分離構成を `.gitkeep` で先行配置した暫定状態
- LibGDX gdx-liftoff の出力で再編する想定（MVP スキャフォルド着手時に実施）
- Android backend を有効化したら `.gitignore` の LibGDX 固有節も解放する

## CI で強制する項目
- Markdown lint（docs と README）
- EditorConfig 準拠
- ブランチ名 (`feat/#\d+` / `fix/#\d+` / `develop`)
- `build.gradle` 追加後に追加するジョブ:
  - Gradle build
  - Spotless check (`./gradlew spotlessCheck`)
  - JUnit 5 テスト実行
