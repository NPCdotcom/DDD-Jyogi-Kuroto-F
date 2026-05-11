# ブランチ戦略

DDD-Jyogi-Kuroto-F の Git ブランチ運用ルール。Git 初心者でも迷わず動けることを目的とする。

---

## 1. ブランチ構成

```
main
 └── develop
      ├── feat/#1
      ├── feat/#2
      └── fix/#3
```

| ブランチ | 役割 | 直 push | 保護 |
|---|---|---|---|
| `main` | 常に動く状態。リリース対象 | 禁止 | PR + レビュー1名以上 |
| `develop` | 統合ブランチ。安定化したら main へマージ | 非推奨 | force-push 禁止 |
| `feat/#<issue番号>` | 機能追加 | — | — |
| `fix/#<issue番号>` | バグ修正 | — | — |

---

## 2. ブランチ命名規則

- 新機能: `feat/#<issue番号>`  例: `feat/#12`
- バグ修正: `fix/#<issue番号>`  例: `fix/#34`
- 作業内容を Issue 化してから、その番号を使うこと
- 命名規則違反は CI (`check-branch-name`) で検出される

---

## 3. マージフロー

```
作業ブランチ (feat/#N)
   ↓ PR
develop
   ↓ 安定化を確認してから PR
main
```

### マージ条件 (PR)

- CI 全緑
- 他メンバー1名以上のレビュー approve (AI レビューだけでは不可)
- PR テンプレの動作確認チェックリストを満たす

---

## 4. レビュー方針

- AI レビュー (Claude / Rabbit) は使用可、推奨
- ただし **人間メンバー1名以上のレビューを必ず通す** こと
- レビュー依頼は PR のメンション or Discord/HackMD で通知

---

## 5. Git 初心者向けチートシート (最小5コマンド)

最新の `develop` から派生して PR を出すまでの最小ステップ。

```bash
# 1. develop の最新を取得
git checkout develop
git pull origin develop

# 2. 作業ブランチを作成 (Issue #12 を作っておくこと)
git checkout -b feat/#12

# 3. ファイルを編集してコミット
git add .
git commit -m "feat: AP制戦闘の基盤を追加 (#12)"

# 4. リモートへ push
git push -u origin feat/#12

# 5. GitHub の Web UI または gh CLI で PR を作成
gh pr create --base develop --title "feat: AP制戦闘の基盤" --body "Closes #12"
```

### よくあるトラブル

- **「コンフリクトしました」と出た場合**: `git pull origin develop` で develop の最新を取り込み、コンフリクトを手で解消してから再 push する
- **間違ったブランチでコミットした場合**: 慌てず `git stash` で退避してから正しいブランチへ移動 → `git stash pop` で復元
- **コミットメッセージを間違えた場合**: 直前のコミットなら `git commit --amend`、それより前なら無理せずチームに相談

---

## 6. ブランチ保護設定 (リポジトリオーナー作業)

`main` / `develop` に対して以下を GitHub 側で設定する:

- `main`
  - "Require a pull request before merging" ON
  - "Require approvals" = 1
  - "Require status checks to pass before merging" = CI 全ジョブ
  - "Do not allow bypassing the above settings" ON
- `develop`
  - "Restrict who can push" は OFF (チーム全員 push 可)
  - "Allow force pushes" OFF

---

## 7. 補足: feat/fix 以外のブランチが必要になったら

- `docs/#<issue番号>`: ドキュメントのみの変更 (任意。`feat/#N` で代用可)
- `chore/#<issue番号>`: 設定・CI などの保守作業
- 上記2つも CI の `check-branch-name` 正規表現に追加すること

---

## 8. 既存ブランチの扱い

### `feature/java_environment_setup/npc`

- **内容**: Gradle 環境構築の試行ブランチ（GitHub Actions 連携なし）
- **位置付け**: **参考用ブランチ**。main にマージしない
- **扱い方**:
  - 削除はしない（試行記録として保持）
  - 環境構築の進め方を検討する際の参照源として利用可
  - 命名は旧スタイル。今後の正式環境構築は `feat/#<issue番号>` 形式で別途切る
- **今後の正式な環境構築**: Issue を立ててから `feat/#<issue番号>` で別ブランチを切る

### 過去のスタイルブランチについて

- 上記ブランチは CI の `check-branch-name` 検査の対象外（既に push 済みのため）
- 今後新規に切るブランチはすべて `feat/#\d+` / `fix/#\d+` / `docs/#\d+` / `chore/#\d+` 形式に統一する
