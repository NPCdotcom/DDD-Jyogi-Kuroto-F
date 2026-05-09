# 技術選定メモ

## 採用
- Java 21 (Eclipse Temurin)
- LibGDX 1.12+
- Gradle (Wrapper同梱)
- IntelliJ IDEA Community

## 検討中
- 物理エンジン: Box2D (LibGDX同梱) を使うか
- ECS: Ashleyを使うか、自前で書くか
- データ形式: JSONをJacksonで読むか、シンプルにプロパティファイルか

## 環境統一ルール
- JDK: Temurin 21 LTS
- Gradle Wrapper: リポジトリにコミット
- 文字コード: UTF-8
- 改行コード: LF
- .editorconfig 配置

## 検討から外したもの
- Kotlin: Java縛りアピールのため不採用
- JavaFX: ゲーム用途で物足りない