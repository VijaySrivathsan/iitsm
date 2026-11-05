#!/usr/bin/env bash
# PostToolUse hook: after Claude edits a .java file or pom.xml, compile main + test code.
# On failure, print compiler errors to stderr and exit 2 so Claude sees them and fixes them.
set -u

input="$(cat)"
file_path="$(printf '%s' "$input" | jq -r '.tool_input.file_path // empty' 2>/dev/null)"

case "$file_path" in
  *.java|*pom.xml) ;;
  *) exit 0 ;;
esac

project_dir="${CLAUDE_PROJECT_DIR:-$(pwd)}"
cd "$project_dir" || exit 0
[ -f pom.xml ] || exit 0   # nothing to build before M1 creates pom.xml

# Hooks don't load ~/.zshrc, so pin Java 21 and Homebrew's PATH here.
export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"
if [ -x /usr/libexec/java_home ]; then
  jh="$(/usr/libexec/java_home -v 21 2>/dev/null)" && export JAVA_HOME="$jh"
fi

if ! command -v mvn >/dev/null 2>&1; then
  echo "check-build hook: mvn not found on PATH; skipping compile check." >&2
  exit 0
fi

out="$(mvn -q -B test-compile 2>&1)"
status=$?
if [ $status -ne 0 ]; then
  {
    echo "BUILD FAILED after editing $file_path. Fix these compile errors before continuing:"
    # Keep only "File.java:[line,col] message" lines and their symbol/location details, without repeats.
    printf '%s\n' "$out" \
      | sed -n '/Failed to execute goal/,$p' \
      | grep -E '\.java:\[|symbol:|location:|error:' \
      | sed 's/^\[ERROR\] *//' \
      | awk '!seen[$0]++' \
      | head -40
    printf '%s\n' "$out" | grep -q '\.java:\[' || printf '%s\n' "$out" | grep -E '\[ERROR\]' | head -15
  } >&2
  exit 2
fi
exit 0
