#!/usr/bin/env bash

#npm version patch
rm -rf ./dist
lein dist

export ELECTRON_BUILDER_COMPRESSION_LEVEL=3

DEBUG=electron-builder,electron-builder:* electron-builder -c electron-builder-beta.yml --publish always -mwl

open dist
