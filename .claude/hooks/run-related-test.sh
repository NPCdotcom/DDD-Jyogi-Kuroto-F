#!/bin/bash
# PostToolUse(Edit|Write) hook: 編集された Java ファイルに対応するテストを実行
#
# - src/main/java/.../Foo.java を編集 → FooTest を実行
# - src/test/java/.../FooTest.java を編集 → そのまま実行
# - Java ファイル以外はスキップ
#
# テスト失敗時も hook は exit 0 (テスト結果は Claude に追加コンテキストとして渡るのみ)

set -e

FILE_PATH="$1"

if [[ ! "$FILE_PATH" =~ \.java$ ]]; then
  exit 0
fi

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"
cd "$PROJECT_DIR"

# JAVA_HOME 設定
if [ -n "$WINDIR" ] || [ -n "$SYSTEMROOT" ]; then
  export JAVA_HOME="C:/Program Files/Java/jdk-25.0.3"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

# gradlew が無ければスキップ
if [ ! -f "./gradlew" ] && [ ! -f "./gradlew.bat" ]; then
  exit 0
fi

if [ -n "$WINDIR" ] || [ -n "$SYSTEMROOT" ]; then
  GRADLEW="./gradlew.bat"
else
  GRADLEW="./gradlew"
fi

# クラス名抽出
CLASS_NAME=""
if [[ "$FILE_PATH" =~ src/main/java/.*/([A-Z][A-Za-z0-9_]*)\.java$ ]]; then
  CLASS_NAME="${BASH_REMATCH[1]}Test"
elif [[ "$FILE_PATH" =~ src/test/java/.*/([A-Z][A-Za-z0-9_]*Test)\.java$ ]]; then
  CLASS_NAME="${BASH_REMATCH[1]}"
fi

if [ -z "$CLASS_NAME" ]; then
  exit 0
fi

# 該当テストクラスが存在するか確認 (存在しなければスキップ)
if ! find src/test -name "${CLASS_NAME}.java" -print -quit 2>/dev/null | grep -q .; then
  exit 0
fi

# テスト実行 (短時間で済むよう --tests でフィルタ)
OUTPUT=$("$GRADLEW" --quiet --no-daemon test --tests "*${CLASS_NAME}" 2>&1 || true)
SUMMARY=$(echo "$OUTPUT" | grep -E "tests completed|BUILD|FAILED" | tail -3)

if echo "$OUTPUT" | grep -qE "FAILED|BUILD FAILED"; then
  cat << EOF
{
  "hookSpecificOutput": {
    "hookEventName": "PostToolUse",
    "additionalContext": "⚠️ ${CLASS_NAME} に失敗テストあり:\n${SUMMARY}"
  }
}
EOF
elif [ -n "$SUMMARY" ]; then
  cat << EOF
{
  "hookSpecificOutput": {
    "hookEventName": "PostToolUse",
    "additionalContext": "✅ ${CLASS_NAME} PASS: ${SUMMARY}"
  }
}
EOF
fi

exit 0
