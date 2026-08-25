#!/usr/bin/env bash
#
# Builds the React frontend and stages the production bundle into the Maven
# resources directory so it gets packaged inside the plugin jar.
#
# The plugin serves the classpath resource directory "frontend" at the HTTP
# root (see com.playtheatria.shopdb.web.StaticFiles), so the staged bundle
# becomes the website and the REST API keeps its /api/v3 prefix. The staged
# directory is generated, not committed (see .gitignore), so it must be
# produced before `mvn package` or the jar ships without the website.
#
# REACT_APP_BACKEND is baked into the bundle at build time (create-react-app
# reads env vars statically) and must be an absolute URL — parts of the app
# construct `new URL(...)` from it, which rejects relative paths. The default
# comes from frontend/.env.production; set the variable to override it:
#
#   REACT_APP_BACKEND=https://shopdb.playtheatria.com/api/v3 ./scripts/build-frontend.sh
#
set -euo pipefail

# Resolve the repo root relative to this script so it works from any CWD.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

FRONTEND_DIR="$ROOT_DIR/frontend"
BUILD_DIR="$FRONTEND_DIR/build"
RESOURCES_DIR="$ROOT_DIR/src/main/resources/frontend"

# Absolute asset paths so client-side routes (e.g. /players/name) load their
# JS/CSS correctly after a refresh; the plugin falls back to index.html for
# unknown paths.
export PUBLIC_URL="/"

# create-react-app treats warnings as errors when CI is set, which makes the
# build brittle. Default to a non-CI build but let callers override.
export CI="${CI:-false}"

# react-scripts 4 uses webpack 4, whose hashing needs OpenSSL legacy algorithms
# on Node 17+. The pinned Node version lives in .tool-versions (asdf).
NODE_MAJOR="$(node -p 'process.versions.node.split(".")[0]')"
if [ "$NODE_MAJOR" -ge 17 ]; then
  export NODE_OPTIONS="${NODE_OPTIONS:-} --openssl-legacy-provider"
fi

echo "==> Installing frontend dependencies"
if [ -f "$FRONTEND_DIR/package-lock.json" ]; then
  npm --prefix "$FRONTEND_DIR" ci
else
  npm --prefix "$FRONTEND_DIR" install
fi

echo "==> Building frontend production bundle (REACT_APP_BACKEND=${REACT_APP_BACKEND:-<from frontend/.env.production>})"
npm --prefix "$FRONTEND_DIR" run build

if [ ! -f "$BUILD_DIR/index.html" ]; then
  echo "ERROR: frontend build did not produce $BUILD_DIR/index.html" >&2
  exit 1
fi

echo "==> Staging bundle into src/main/resources/frontend"
rm -rf "$RESOURCES_DIR"
mkdir -p "$RESOURCES_DIR"
cp -R "$BUILD_DIR/." "$RESOURCES_DIR/"

echo "==> Frontend staged ($(find "$RESOURCES_DIR" -type f | wc -l | tr -d ' ') files)"
