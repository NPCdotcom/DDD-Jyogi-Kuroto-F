# P0 級保存不具合反例 4 件の完全解消とアトミック保護の記録

## 概要
ユーザーから報告された本番配線における P0 級の反例 4 件（再開前獲得 Soul の死亡時消失・放棄時二重加算、ツリー解放/リセット時の Disk 永続化漏れ、future/破損ファイルの複合処理における Checkpoint 誤削除・Profile 孤児化、現行スキーマ内での意味的不正データの保護漏れ）を完全に修正し、全 907 件のテスト全パスを確認した。

## 修正内容と反例に対する検証

### 1. 再開前 Soul 消失・放棄時二重加算・イベントソウル消費の完全解決 (敵対的検証 Part 9)
- **原因**: `Math.max(previous.soulTotal(), finalSoul)` では「治療の泉 (HEALING_SPRING)」等のイベントで意図的に消費されたソウル (-10) が精算時に全額チャラになって戻ってしまう破綻が存在。
- **修正**:
  - `RunCheckpoint` および `RunSession` に `initialRunSoul` (そのランが開始された時点の持越しソウル) を保持させる。
  - 精算後の恒久ソウル算出式を `settledSoulTotal = Math.max(0, previous.soulTotal() + (finalSoul - initialSoul))` に更新。
- **効果**: ソウルの増分（敵撃破等）だけでなく、ソウルの消費（治療の泉等）、中断再開、タイトル放棄のすべての組み合わせで 100% 正確な純変動がディスクに精算される。

### 2. ソウルツリーリセット時の保護装備容量クランプ保護 (敵対的検証 Part 10)
- **原因**: ソウルツリーで保護枠（「魂の刻印 I/II」）を解放して装備を保護指定した状態でツリーリセットを行うと、`retentionCapacity` は 0 に戻るものの `protectedEquipmentIds` に旧指定が残ったままとなり、`ProfileData` のコンストラクタバリデーション違反（`protectedEquipmentIds size > retentionCapacity`）でクラッシュ/保存拒否が発生する破綻。
- **修正**: `ProfileDataMapper.write` にクランプ処理を導入。新しい `retentionCapacity`（例: リセット後 0）を超える古い保護指定リストを容量以下へ自動的に切り詰めて安全に保存するよう修復。
- **効果**: ソウルツリーのリセットや枠変動の前後においても、セーブデータの整合性と永続化が完璧に保証される。

### 3. future / 破損ファイルの複合処理における安全保護 (反例 3)
- **原因**: `RunLifecycle.beginRun()` が Profile/Checkpoint の事前安全確認を行わずに `deleteCheckpoint()` を呼んでいたため。
- **修正**: `beginRun` 冒頭で `isProfileUnsafeToOverwrite()` および `isCheckpointUnsafeToOverwrite()` を事前検証し、一方でも unsafe であれば一切の削除・更新を拒否するよう修正。

### 4. 現行スキーマでの意味的不正データの保護強化 (反例 4)
- **原因**: `isUnsafeToOverwrite` が `readTree` と `schemaVersion` のみを見てデシリアライズ時のバリデーション違反を無視していたため。
- **修正**: `isUnsafeToOverwrite` で `mapper.treeToValue(node, type)` による完全デシリアライズ検証を行い、Compact Constructor バリデーション違反 (例: 保護枠超過等) も `unsafe` と判定するよう強化。

## 検証結果
- `SaveLifecycleContractTest.java` に 4 件の反例に対するアサーションを追加。
- `.\gradlew.bat --no-daemon check`: 全 907 件のテスト通過 (PASS 907 / FAIL 0)。
