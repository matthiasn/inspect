#!/usr/bin/env bash

yarn install
rm -rf ./dist
lein dist

PLATFORMS=$1
ELECTRON_BUILDER_COMPRESSION_LEVEL=3

echo "Publishing Release"
./node_modules/.bin/electron-builder --publish always $1
