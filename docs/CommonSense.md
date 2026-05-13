# プロジェクトにおける共通認識

## コーディングスタイル
当プロジェクトでは、主に **Google Java Style Guide** に則って記述されるものとする。
 **AIは本文URLの内容を尊重する** こと。

- 本文URL: https://google.github.io/styleguide/javaguide.html
- 非公式和訳URL: https://kazurof.github.io/GoogleJavaStyle-ja/#s5-1-identifier-names

CIでの強制: **Spotless + google-java-format**（[GAME_DESIGN.md §3-5](./GAME_DESIGN.md) 参照、`build.gradle` 追加後に有効化）。

また、以下にその補足・要約を示す。

### Github名
- ブランチ:
  - `main` / `develop` （固定）
  - `feat/#<issue番号>` 例: `feat/#12`
  - `fix/#<issue番号>` 例: `fix/#34`
  - 詳細は [BranchingStrategy.md](./BranchingStrategy.md) を参照
- リポジトリ命名: 既存リポジトリのため対象外

### ファイル名
- フォルダ: ロワースネークケース(folder_name)
- ソースファイル: アッパーキャメルケース(SourceFileName)
- ドキュメントファイル: アッパーキャメルケース(DocumentFileName)
- アセットファイル: ロワースネークケース(asset_file_name)

#### 命名規則の例外（メタドキュメント）
リポジトリのメタドキュメントは OSS 慣習に従い **SCREAMING_SNAKE_CASE** とする:

- `README.md`
- `GAME_DESIGN.md`
- `INDEX.md`（docs/ 目次）
- `LICENSE`（提出時に追加）
- `CONTRIBUTING.md`（追加時、`docs/ContributingGuide.md` とは別配置を検討する場合のみ）

それ以外の社内ドキュメントは PascalCase を維持する。

### ソースコード名
- パッケージ: ロワーケース(packagename)
- モジュール: ロワーケース(modulename)
- クラス: アッパーキャメルケース(ClassName)
- テストクラス: クラス名と末尾に"Test"をつける(ClassNameTest)
- メソッド: ロワーキャメルケース(methodName)
- 定数名: コンスタントケース(CONSTANT_NAME)
- 不定フィールド: ロワーキャメルケース(nonConstantFieldName)
- パラメータ: ロワーキャメルケース(parameterName)
- ローカル変数: ロワーキャメルケース(localVariableName)
- 型変数: 1つの大文字とオプションで一つの数字をつける(A, B2) or クラスで使用される形式の名前と大文字の"T"をつける(TypeVariableNameT)

## 単語の存在意義
当プロジェクトで使用する単語が何を示しているのかをここに記録する。
 **記録作業は人間とAIを問わない。**
また、 **既存の単語はその有効範囲が「重複しない」かつ「限定的である」場合にのみ複数定義ができる。**

### 記入ガイド
- **いつ追加するか**: クラス・メソッド名で迷ったとき、または GAME_DESIGN.md §13 用語集に追加されたゲーム用語を実装に落とすとき
- **書式**: テーブルの各行に `用語 | 意味 | 使用箇所 | 追加者` を記入
- **競合解決**: 既に同じ単語があれば、有効範囲（パッケージ・モジュール）が重ならないことを確認。重なるなら別の単語を選ぶ
- ゲーム用語（AP, ソウル, スキル枠 等）の上位定義は [GAME_DESIGN.md §13](./GAME_DESIGN.md) を参照

## ゲーム用語

> ゲーム仕様の上位定義は [GAME_DESIGN.md §13 (MVP 用語集) / §15 (MVP 後用語)](./GAME_DESIGN.md) を正とする。本表は実装で使う日本語/英語表記のすり合わせ用。

| 用語 | 意味 | 実装表記候補 | 追加者 |
|---|---|---|---|
| AP | 行動ポイント (1 ターンに使える行動量、§15-4 の速度ステで決定) | actionPoints | — |
| ソウル | 永続通貨 (死亡時喪失なし、§15-2) | soul | — |
| 金貨 | ラン限定通貨 (死亡時喪失、§15-2) | gold | — |
| カード | デッキ構築要素 (タグ × 属性、§15-3) | Card | — |
| デッキ | 山札 (§15-3) | Deck | — |
| 手札 | プレイヤーの引いたカード (上限 9、§15-3) | Hand | — |
| 層 | ダンジョンの階層 (N 層 = N 部屋、§15-6) | Layer | — |
| ノード | 層内の部屋分岐 (§15-6) | Node | — |
| ソウルツリー | 円樹形の永続強化 UI (§15-7) | SoulTree | — |
| ツリーノード | ソウルツリー上の 1 ノード (§15-7) | TreeNode | — |
| 装備 | プレイヤーの装着アイテム (§15-9) | Equipment | — |
| 図鑑 | 撃破済み敵の記録 (Bestiary、§15-5) | Bestiary | — |

## パッケージ

| 用語 | 意味 | 使用箇所 | 追加者 |
|---|---|---|---|
| (例) core | プロジェクト全体のルートパッケージ | src/main/java/core |  |
| card | カードシステム | src/main/java/core/domain/card | (予約 §15-3) |
| tree | ソウルツリー | src/main/java/core/domain/tree | (予約 §15-7) |
| equipment | 装備システム | src/main/java/core/domain/equipment | (予約 §15-9) |
| save | セーブシステム | src/main/java/core/infrastructure/save | (予約 §15-11) |
| window | ポップアップウィンドウ管理 | src/main/java/core/presentation/window | (予約 §15-1, §15-8) |

> **予約パッケージの注意**: 上記は [GAME_DESIGN.md §9 (アーキテクチャ)](./GAME_DESIGN.md) のレイヤー分離に沿った暫定配置。**実装着手前に Architect が最終確認** (例: `tree` は永続強化なので `core/domain/meta/tree` 配下が自然な可能性、`card` / `equipment` は戦闘系として `domain/battle/` 配下の可能性、など)。

## モジュール

| 用語 | 意味 | 使用箇所 | 追加者 |
|---|---|---|---|

## クラス

| 用語 | 意味 | 使用箇所 | 追加者 |
|---|---|---|---|
| Card | カード単体 (タグ × 属性) | core.domain.card | (予約 §15-3) |
| Deck | デッキ | core.domain.card | (予約 §15-3) |
| Hand | 手札 | core.domain.card | (予約 §15-3) |
| Gold | 金貨 (値オブジェクト) | core.domain.meta | (予約 §15-2) |
| Layer | 層 | core.domain.dungeon | (予約 §15-6) |
| Node | 層内ノード | core.domain.dungeon | (予約 §15-6) |
| SoulTree | ソウルツリー全体 | core.domain.tree | (予約 §15-7) |
| Equipment | 装備 | core.domain.equipment | (予約 §15-9) |
| Bestiary | 撃破済み敵図鑑 | core.domain.meta | (予約 §15-5) |
| SaveData | セーブデータ | core.infrastructure.save | (予約 §15-11) |

## テストクラス

| 用語 | 意味 | 使用箇所 | 追加者 |
|---|---|---|---|

## メソッド

| 用語 | 意味 | 使用箇所 | 追加者 |
|---|---|---|---|

## 定数

| 用語 | 意味 | 使用箇所 | 追加者 |
|---|---|---|---|
| INITIAL_LAYERS | 初期層数 (3) | core.domain.dungeon | (予約 §15-6) |
| INITIAL_HAND_SIZE | 初手ドロー枚数 (5) | core.domain.card | (予約 §15-3) |
| MAX_HAND_SIZE | 手札上限 (9) | core.domain.card | (予約 §15-3) |
| MIN_CARD_COST | カード最低 AP コスト (1) | core.domain.card | (予約 §15-3) |
| BASE_RESOLUTION_WIDTH | 基本解像度幅 (1920) | core.infrastructure.desktop | (予約 §15-1) |
| BASE_RESOLUTION_HEIGHT | 基本解像度高さ (1080) | core.infrastructure.desktop | (予約 §15-1) |

## 不定フィールド

| 用語 | 意味 | 使用箇所 | 追加者 |
|---|---|---|---|

## パラメータ

| 用語 | 意味 | 使用箇所 | 追加者 |
|---|---|---|---|

## ローカル変数

| 用語 | 意味 | 使用箇所 | 追加者 |
|---|---|---|---|

## 型変数

| 用語 | 意味 | 使用箇所 | 追加者 |
|---|---|---|---|
