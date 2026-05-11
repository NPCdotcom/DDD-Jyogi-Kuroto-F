# スケジュール

> ハッカソン期間中の作業マイルストン。仕様の詳細は [GAME_DESIGN.md](./GAME_DESIGN.md) 参照。

## マイルストーン

| ID | 目標 | 内容 |
|---|---|---|
| **M1 = MVP** | **今週金曜** | 1階層ダンジョン + AP制スキル戦闘 + 死亡時ソウル継承の最小ループ ([GAME_DESIGN.md §11-1](./GAME_DESIGN.md)) |
| **M2 = ハッカソン本番** | **来週土日** | 提出 + デモ |

### M1 (MVP) 合格チェックリスト

以下が全部 ✓ になれば MVP 達成とみなす。

- [ ] タイトル画面が起動する（Desktop, Windows）
- [ ] 1階層のダンジョンに入れる
- [ ] プレイヤーが移動できる（AP制、APコスト消費）
- [ ] 敵もマップ上に存在し、プレイヤーと AP 制で交互に行動する（変動ターン制）
- [ ] スキル枠（4枠）からスキルを発動して敵にダメージを与えられる
- [ ] HP 0 で死亡判定が走る
- [ ] 死亡時にソウルが保持される（金貨・装備は喪失）
- [ ] ローカルで JAR or `gradlew run` で起動できる

> 仕様詳細は [GAME_DESIGN.md §11-2](./GAME_DESIGN.md)。

## 運用方針

- Issue は **誰がどれを取っても良い** （カテゴリ・優先度ラベルで取捨選択）
- MVP までは P0 ラベルの Issue を優先
- MVP 達成後は P1 → P2 の順
- 詳細な機能リストは [RolesDivision.md](./RolesDivision.md)、ブランチ運用は [BranchingStrategy.md](./BranchingStrategy.md) を参照

## リスク管理

- **致命リスク**: データぶっ壊れる
  - 緩和策: GitHub にプッシュ徹底、main / develop はブランチ保護
- **Git スキル差による事故**（誤マージ、競合解消ミス）
  - 緩和策: ブランチ保護 + PR テンプレ + Git 初心者向けチートシート（[BranchingStrategy.md §5](./BranchingStrategy.md)）
- **仕様認識のズレ**
  - 緩和策: GAME_DESIGN.md を Single Source of Truth とし、議論=HackMD / 確定=本リポジトリ docs/ で運用
