#!/usr/bin/env bash
set -Eeuo pipefail

script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd -P)

msg() {
  echo >&2 -e "${1-}"
}

die() {
  local msg=$1
  local code=${2-1} # default exit status 1
  msg "$msg"
  exit "$code"
}

parse_params() {
  while :; do
    case "${1-}" in
    -s | --skw)
      SKW="${2-}"
      shift ;;
    -b | --bucket)
      BUCKET="${2-}"
      shift ;;
    -k | --key)
      KEY="${2-}"
      shift ;;
    -t | --tag)
      TAG="${2-}"
      shift ;;
    -p | --prefix)
      PREFIX="${2-}"
      shift ;;
    -?*) die "Unknown option: $1" ;;
    *) break ;;
    esac
    shift
  done

  args=("$@")

  # check required params and arguments
  [[ -z "${SKW-}" ]] && die "Missing required parameter: --skw"
  [[ -z "${BUCKET-}" ]] && die "Missing required parameter: --bucket"
  [[ -z "${KEY-}" ]] && die "Missing required parameter: --key"
  [[ -z "${TAG-}" ]] && die "Missing required parameter: --tag"
  [[ -z "${PREFIX-}" ]] && die "Missing required parameter: --prefix"

  return 0
}

parse_params "$@"
set -x

msg "---- Prepare"
mkdir -p github_actions/nest
GIT_SHA=$(git rev-parse --short=7 HEAD)
GITHUB_RUN_NUMBER="${GITHUB_RUN_NUMBER:-local}"
echo $GIT_SHA > github_actions/$GITHUB_RUN_NUMBER.txt
echo $GIT_SHA > github_actions/nest/$GITHUB_RUN_NUMBER.txt

msg "---- Upload wildcard paths"
$SKW upload \
  -b "$BUCKET" \
  -k "$KEY" \
  -p "$PREFIX" \
  -t "$GIT_SHA-$PREFIX" -t "$TAG" \
  ./github_actions/*.txt

msg "---- Upload glob paths"
$SKW upload \
  -b "$BUCKET" \
  -k "$KEY" \
  -p "$PREFIX" \
  -t "$GIT_SHA-$PREFIX-nest" -t "$TAG" \
  ./github_actions/*.txt ./github_actions/**/*.txt

msg "---- list keys"
$SKW keys \
  -b "$BUCKET" \

msg "----list tags"
$SKW tags \
  -b "$BUCKET" \
  "$KEY"

msg "---- Download"
$SKW download \
  -b "$BUCKET" \
  -k "$KEY" \
  -t "$GIT_SHA-$PREFIX" \
  ./github_actions/down

msg "---- list got files"
ls ./github_actions/down/**