# TheatriaShopDB build orchestration.
#
# `make build` is the entry point: it builds the React frontend, stages the
# bundle into src/main/resources/META-INF/resources (served by Quarkus at the
# HTTP root), and packages the server uber-jar. The staged frontend resources
# are generated (gitignored); without that step the jar serves the API only.
#
# Frontend (mirrors MC-Ledger / TheatriaMarket): the web UI source lives in the
# top-level frontend/ directory (create-react-app, react-scripts 4). Node is
# pinned via .tool-versions (asdf); the build script adds the OpenSSL legacy
# flag webpack 4 needs on modern Node. The API URL baked into the bundle comes
# from frontend/.env.production; override it per-build with:
#
#   REACT_APP_BACKEND=https://shopdb.playtheatria.com/api/v3 make build
#
# The result is target/shopdb-<version>-runner.jar — one self-contained
# process serving the website at / and the API at /api/v3, backed by SQLite.
# See README.md for the runtime environment variables.

MVN   ?= mvn
SHELL := /bin/bash

RESOURCES_FRONTEND := src/main/resources/META-INF/resources

# Quarkus 2.14's build tooling (ByteBuddy via Hibernate 5.6) cannot read class
# files newer than Java 20, so the jar must be built with JDK 11-17. On macOS,
# auto-select an installed JDK 17 via /usr/libexec/java_home; elsewhere (or if
# no 17 is installed) the ambient JAVA_HOME must point at a JDK 11-17.
JAVA17_HOME := $(shell /usr/libexec/java_home -v 17 2>/dev/null)
ifneq ($(JAVA17_HOME),)
export JAVA_HOME := $(JAVA17_HOME)
endif

.DEFAULT_GOAL := build
.PHONY: build frontend package clean clean-frontend java help

## build: Build the frontend and package the server uber-jar (default target)
build: frontend package

## frontend: Build the React app and stage it into Maven resources
frontend:
	./scripts/build-frontend.sh

## package: Package the server uber-jar (expects a staged frontend)
package:
	@echo "==> Building with JAVA_HOME=$${JAVA_HOME:-<unset>}"
	$(MVN) clean package

## clean: Remove Maven and frontend build artifacts
clean: clean-frontend
	$(MVN) clean
	rm -rf frontend/build

## clean-frontend: Remove the staged frontend bundle from resources
clean-frontend:
	rm -rf $(RESOURCES_FRONTEND)

## java: Show which JDK the build will use
java:
	@if [ -n "$(JAVA_HOME)" ]; then "$(JAVA_HOME)/bin/java" -version; else java -version; fi

## help: List available targets
help:
	@grep -E '^## ' $(MAKEFILE_LIST) | sed 's/## /  /'
