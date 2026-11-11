#!/usr/bin/env bash
set -euo pipefail

commit=$(git rev-parse --verify "${1:-HEAD}^{commit}")
padrao_versao='^2\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$'
base=''

while IFS= read -r tag; do
  if [[ "$tag" =~ $padrao_versao ]]; then
    base="$tag"
    break
  fi
done < <(git tag --merged "$commit" --list '2.*' --sort=-version:refname)

if [[ -z "$base" ]]; then
  echo '2.0.0'
  exit 0
fi

commit_base=$(git rev-parse "refs/tags/$base^{commit}")
if [[ "$commit_base" == "$commit" ]]; then
  echo "$base"
  exit 0
fi

IFS=. read -r major minor patch <<< "$base"
padrao_feat='^feat(\([^)]*\))?!?[[:space:]]*:'
padrao_merge='^Merge pull request #[0-9]+ from [^/[:space:]]+/(feat|feature)/'

while IFS= read -r assunto; do
  if [[ "$assunto" =~ $padrao_feat || "$assunto" =~ $padrao_merge ]]; then
    printf '%s.%s.0\n' "$major" "$((minor + 1))"
    exit 0
  fi
done < <(git log --first-parent --format=%s "$commit_base..$commit")

printf '%s.%s.%s\n' "$major" "$minor" "$((patch + 1))"
