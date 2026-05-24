#!/bin/bash
# PreToolUse(Bash) hook: 危険な破壊的コマンドを拒否する
#
# 対象パターン:
# - rm -rf (再帰削除)
# - git push --force (履歴上書き)
# - git reset --hard origin (リモートに追従、ローカル変更喪失)
# - gh repo delete (リポジトリ削除)
# - sudo (権限昇格)
# - gh repo edit --visibility (公開状態変更)
#
# ユーザーが明示的に指示した場合のみ実行を許可する設計。

set -e

INPUT=$(cat)

# jq が無い環境でも動くよう grep ベースで抽出
COMMAND=$(echo "$INPUT" | grep -oE '"command"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | sed -E 's/^"command"[[:space:]]*:[[:space:]]*"//; s/"$//')

if [ -z "$COMMAND" ]; then
  exit 0
fi

DESTRUCTIVE_PATTERNS=(
  'rm[[:space:]]+-[rRf]+f?[[:space:]]+/'
  'rm[[:space:]]+-r[fR]'
  'git[[:space:]]+push[[:space:]]+.*--force'
  'git[[:space:]]+push[[:space:]]+.*-f([[:space:]]|$)'
  'git[[:space:]]+reset[[:space:]]+--hard[[:space:]]+origin'
  'gh[[:space:]]+repo[[:space:]]+delete'
  'gh[[:space:]]+repo[[:space:]]+edit[[:space:]]+.*--visibility'
  '^sudo[[:space:]]'
  '[[:space:]]sudo[[:space:]]'
)

for pattern in "${DESTRUCTIVE_PATTERNS[@]}"; do
  if echo "$COMMAND" | grep -qE "$pattern"; then
    cat << EOF
{
  "hookSpecificOutput": {
    "hookEventName": "PreToolUse",
    "permissionDecision": "deny",
    "permissionDecisionReason": "破壊的コマンドパターンを検出: '${pattern}'。ユーザーが明示的に指示した場合のみ実行を許可してください (`.claude/hooks/block-destructive.sh` を参照)。"
  }
}
EOF
    exit 0
  fi
done

exit 0
