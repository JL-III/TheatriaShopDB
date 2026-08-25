# ShopDB build orchestration.
#
# `make build` is the entry point: it builds the React frontend, stages the
# bundle into src/main/resources/frontend (served by the plugin's embedded web
# server at the HTTP root), and packages the plugin jar. The staged frontend
# resources are generated (gitignored); without that step the plugin serves
# the API only.
#
# Frontend (mirrors MC-Ledger / TheatriaMarket): the web UI source lives in the
# top-level frontend/ directory (create-react-app, react-scripts 4). Node is
# pinned via .tool-versions (asdf); the build script adds the OpenSSL legacy
# flag webpack 4 needs on modern Node. The API URL baked into the bundle comes
# from frontend/.env.production; override it per-build with:
#
#   REACT_APP_BACKEND=https://shopdb.playtheatria.com/api/v3 make build
#
# JDK: the plugin targets Java 21 bytecode (Paper 1.21+ servers run Java 21+),
# so build with any JDK 21 or newer — modern JDKs (21/25) work out of the box.
#
# The result is target/ShopDB-<version>.jar — a Paper plugin that serves the
# website at / and the API at /api/v3 from inside the game server, backed by
# SQLite in the plugin's data folder. Drop it into plugins/.

MVN   ?= mvn
SHELL := /bin/bash

RESOURCES_FRONTEND := src/main/resources/frontend

.DEFAULT_GOAL := build
.PHONY: build frontend package clean clean-frontend java help

## build: Build the frontend and package the plugin jar (default target)
build: frontend package

## frontend: Build the React app and stage it into Maven resources
frontend:
	./scripts/build-frontend.sh

## package: Package the plugin jar (expects a staged frontend)
package:
	@echo "==> Building with JAVA_HOME=$${JAVA_HOME:-<system default>}"
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
