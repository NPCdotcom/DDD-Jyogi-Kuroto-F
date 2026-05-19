#!/bin/bash
# Stop hook: Claude 応答終了時に全テストを走らせて品質ゲート
#
# ユーザー要望: 「テスト毎回 ON」「仕様固めて最初にテスト書けば AI が書いてもちゃんと動く」
# 「意味のあるテストか」が重要なので、ここでは件数だけでなく失敗時の詳細も Claude に返す

set -e

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

# src/main 配下に Java ファイルが無ければスキップ (実装着手前は no-op)
if ! find src/main/java -name "*.java" -print -quit 2>/dev/null | grep -q .; then
  exit 0
fi

if [ -n "$WINDIR" ] || [ -n "$SYSTEMROOT" ]; then
  GRADLEW="./gradlew.bat"
else
  GRADLEW="./gradlew"
fi

# 全テスト実行
OUTPUT=$("$GRADLEW" --quiet --no-daemon test 2>&1 || true)
SUMMARY=$(echo "$OUTPUT" | grep -E "tests completed|BUILD" | tail -3)

if echo "$OUTPUT" | grep -qE "FAILED|BUILD FAILED"; then
  # 失敗テストの詳細を抽出
  FAILED_TESTS=$(echo "$OUTPUT" | grep -E "FAILED$" | head -5)
  cat << EOF
{
  "hookSpecificOutput": {
    "hookEventName": "Stop",
    "additionalContext": "❌ Stop Hook の全テスト走行で失敗あり:\n${SUMMARY}\n\n失敗テスト:\n${FAILED_TESTS}\n\n次のターンで修正対応してください。"
  }
}
EOF
elif [ -n "$SUMMARY" ]; then
  cat << EOF
{
  "hookSpecificOutput": {
    "hookEventName": "Stop",
    "additionalContext": "✅ 全テスト PASS: ${SUMMARY}"
  }
}
EOF
fi

exit 0
