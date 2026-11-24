#!/usr/bin/env bash
# Claude Code PreToolUse hook (Bash): run the full Maven build before Claude runs `git commit`.
# Exit 0 lets the command run; exit 2 blocks it and returns stderr to Claude, which then has
# to fix the build instead of committing. Enforces the CLAUDE.md rule "build green at every commit".
set -uo pipefail

cmd=$(jq -r '.tool_input.command // empty')

# Only act on commands that run `git commit` (also when chained, e.g. `git add … && git commit …`).
if ! grep -qE '(^|[;&|([:space:]])git[[:space:]]+commit([[:space:]]|$)' <<<"$cmd"; then
  exit 0
fi

root="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel)}"
log=$(mktemp)
trap 'rm -f "$log"' EXIT

if (cd "$root/java" && ./mvnw -q -B verify) >"$log" 2>&1; then
  exit 0
fi

{
  echo "Commit blocked: './mvnw verify' failed in java/. Fix the build before committing."
  echo "--- last 40 lines of build output ---"
  tail -n 40 "$log"
} >&2
exit 2
