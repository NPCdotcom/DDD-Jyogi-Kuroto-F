#!/bin/bash
# PostToolUse(Edit|Write) hook: Java ファイルが編集されたら Spotless を適用
#
# 注意: Spotless は引数指定での部分実行ができないため、Java 編集のたびに
# プロジェクト全体に spotlessApply をかける。差分のないファイルは Up-to-date で無視される。

set -e

FILE_PATH="$1"

# Java ファイル以外はスキップ
if [[ ! "$FILE_PATH" =~ \.java$ ]]; then
  exit 0
fi

# プロジェクトディレクトリ
PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"
cd "$PROJECT_DIR"

# JAVA_HOME 設定 (Windows: Oracle JDK 25 デフォルトパス)
if [ -n "$WINDIR" ] || [ -n "$SYSTEMROOT" ]; then
  export JAVA_HOME="C:/Program Files/Java/jdk-25.0.3"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

# gradlew が無ければ何もしない (整備初期で wrapper 未生成のケース)
if [ ! -f "./gradlew" ] && [ ! -f "./gradlew.bat" ]; then
  exit 0
fi

# Windows なら gradlew.bat、それ以外は gradlew
if [ -n "$WINDIR" ] || [ -n "$SYSTEMROOT" ]; then
  GRADLEW="./gradlew.bat"
else
  GRADLEW="./gradlew"
fi

# Spotless 適用 (失敗しても hook は exit 0 で続行)
"$GRADLEW" --quiet --no-daemon spotlessApply 2>&1 | tail -3 || true

exit 0
