# 保存層の Critical 欠陥 3 件の修正記録

## 概要
カード画像量産前の最終敵対的検証により指摘された保存層の Critical 欠陥 3 件（旧セーブ再開後の恒久 Soul 全損、ラン中メタ保存での Profile Soul 0 上書き、future/不正 Checkpoint の開始時削除）を特定・解消し、全テスト 903 件の通過を確認した。

## 修正対象の不具合と修正内容

### 1. 旧セーブ再開後の死亡・クリアで恒久 Soul が消える不具合 (AC-1)
- **原因**: 旧セーブ移行・再開後、ラン終了時 (`onRunEnded()`) に Player の最終所持ソウルを Profile の `soulTotal` に直接代入していたため、旧 Profile に元々存在していた恒久ソウルが上書き消滅していた。
- **修正**: `RunSession` に `initialRunSoul` (ラン開始/ロード直後の初期ソウル) を保持させ、精算後の恒久ソウルを `S_settled = S_profile + max(0, S_final - S_initial)` の数式で算出するよう修正。

### 2. ラン中の最初の敵撃破等で Profile Soul が 0 へ上書きされる不具合 (AC-2)
- **原因**: ラン開始時に `progress.playerSoul()` が 0 に初期化されるため、ラン中に初敵撃破 (`recordEnemyDefeated`) やチュートリアル閲覧完了 (`markTutorialSeen`) 等で `saveProgressProfile()` が呼ばれると、`forSoulUpdate` 経由で Profile の `soulTotal` が 0 へ上書きされていた。
- **修正**: `saveProgressProfile()` で `ProfileDataMapper.forLayerBoundary(progress, previous)` を使用するよう変更し、メタ進捗変更時は直前の恒久ソウル `previous.soulTotal()` を据え置くよう保護。

### 3. future / 不正 Checkpoint が新規開始時に削除される不具合 (AC-3)
- **原因**: `RunLifecycle.beginRun()` の冒頭で無条件に `saveManager.deleteCheckpoint()` が実行され、`deleteCheckpoint()` に安全ガードが存在しなかったため。
- **修正**: `SaveManager.deleteCheckpoint()` および `deleteProfile()` に `isUnsafeToOverwrite` チェックを追加し、未来スキーマバージョンや破損ファイルの物理削除を拒否するよう二重ガードを追加。

## 検証結果
- `SaveLifecycleContractTest.java` に不具合保護テストケースを拡充。
- `.\gradlew.bat --no-daemon check`: 全 903 件のテスト通過 (PASS 903 / FAIL 0)。
