# ブランチ戦略

DDD-Jyogi-Kuroto-F の Git ブランチ運用ルール。Git 初心者でも迷わず動けることを目的とする。

---

## 1. ブランチ構成

```
main
 ├── feat/#1
 ├── feat/#2
 └── fix/#3
```

| ブランチ | 役割 | 直 push | 保護 |
|---|---|---|---|
| `main` | 常に動く状態。作業ブランチの取り込み先 (リリース対象) | 禁止 | PR + レビュー1名以上 |
| `feat/#<issue番号>` | 機能追加 | — | — |
| `fix/#<issue番号>` | バグ修正 | — | — |

> **2026-10-30 変更**: 統合ブランチ `develop` は廃止した。main に完全に取り込み済みで、2026-05-19 以降は使われていなかったため。新しい PR は main に向ける。GitHub のルールセット「develop」が削除を禁止しているため、ブランチ自体は管理者がルールセットを外すまで残る (使わないこと)。

---

## 2. ブランチ命名規則

- 新機能: `feat/#<issue番号>`  例: `feat/#12`
- バグ修正: `fix/#<issue番号>`  例: `fix/#34`
- 作業内容を Issue 化してから、その番号を使うこと
- 命名規則違反は PR レビューで指摘する（CI による自動検出は導入しない方針）

---

## 3. マージフロー

```
作業ブランチ (feat/#N)
   ↓ PR (人間レビュー1名以上)
main
```

### マージ条件 (PR)

- 他メンバー1名以上のレビュー approve (AI レビューだけでは不可)
- PR テンプレの動作確認チェックリストを満たす

---

## 4. レビュー方針

- AI レビュー (Claude / Rabbit) は使用可、推奨
- ただし **人間メンバー1名以上のレビューを必ず通す** こと
- レビュー依頼は PR のメンション or Discord/HackMD で通知

---

## 5. Git 初心者向けチートシート (最小5コマンド)

最新の `main` から派生して PR を出すまでの最小ステップ。

```bash
# 1. main の最新を取得
git checkout main
git pull origin main

# 2. 作業ブランチを作成 (Issue #12 を作っておくこと)
git checkout -b feat/#12

# 3. ファイルを編集してコミット
git add .
git commit -m "feat: AP制戦闘の基盤を追加 (#12)"

# 4. リモートへ push
git push -u origin feat/#12

# 5. GitHub の Web UI または gh CLI で PR を作成
gh pr create --base main --title "feat: AP制戦闘の基盤" --body "Closes #12"
```

### よくあるトラブル

- **「コンフリクトしました」と出た場合**: `git pull origin main` で main の最新を取り込み、コンフリクトを手で解消してから再 push する
- **間違ったブランチでコミットした場合**: 慌てず `git stash` で退避してから正しいブランチへ移動 → `git stash pop` で復元
- **コミットメッセージを間違えた場合**: 直前のコミットなら `git commit --amend`、それより前なら無理せずチームに相談

---

## 6. ブランチ保護設定 (リポジトリオーナー作業)

`main` に対して以下を GitHub 側で設定する:

- `main`
  - "Require a pull request before merging" ON
  - "Require approvals" = 1
  - "Do not allow bypassing the above settings" ON

> 2026-10-30 時点の実際の設定: リポジトリのルールセット「main」は、削除と force push だけを禁止している (PR とレビューの必須化はルールとしては未設定)。PR + レビュー1名以上は、チームの規約として守る。

---

## 7. 補足: feat/fix 以外のブランチが必要になったら

- `docs/#<issue番号>`: ドキュメントのみの変更 (任意。`feat/#N` で代用可)
- `chore/#<issue番号>`: 設定・CI などの保守作業

---

## 8. 既存ブランチの扱い (2026-10-30 整理)

- `mvp`: 削除した。MVP 動作確認時点の先端は、タグ `archive/mvp-20260519` で参照できる (`git checkout archive/mvp-20260519`)。
- `develop`: 廃止した (§1 の注記を参照)。
- `feature/java_environment_setup/npc` (Gradle 環境構築の試行ブランチ): GitHub 上にはすでに存在しない。
- 今後新規に切るブランチは、すべて `feat/#\d+` / `fix/#\d+` / `docs/#\d+` / `chore/#\d+` 形式に統一する。
