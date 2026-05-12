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
- `src/main/java/core/{domain,application,infrastructure,presentation}/...` のレイヤー分離は [GAME_DESIGN.md §9](./GAME_DESIGN.md) の方針で確定。実装の対応関係は [SystemSummary.md §3](./SystemSummary.md) を参照
- gdx-liftoff の出力ではなく、シングルモジュールで手書きの `build.gradle` を採用（KISS）。LibGDX を後から差し替えやすいよう、ドメイン層は LibGDX 非依存に保つ
- Android backend を有効化する段階で `.gitignore` の LibGDX 固有節を解放し、ルート `build.gradle` を multi-project に拡張する想定

## CI で強制する項目
- Markdown lint（docs と README）
- EditorConfig 準拠
- ブランチ名 (`feat/#\d+` / `fix/#\d+` / `develop`)
- `build.gradle` 追加後に追加するジョブ:
  - Gradle build
  - Spotless check (`./gradlew spotlessCheck`)
  - JUnit 5 テスト実行
