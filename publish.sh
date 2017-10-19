#!/usr/bin/env bash

#npm version patch
rm -rf ./dist
lein dist

export ELECTRON_BUILDER_COMPRESSION_LEVEL=3
electron-builder --publish always -ml
