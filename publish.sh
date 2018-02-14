#!/usr/bin/env bash

yarn install
rm -rf ./dist
lein dist

PLATFORMS=$1
ELECTRON_BUILDER_COMPRESSION_LEVEL=3

if [ "$2" == "release" ]; then
  echo "Publishing Release"
  ./node_modules/.bin/electron-builder --publish always $1
else
  echo "Publishing Beta Version"
  ./node_modules/.bin/electron-builder -c electron-builder-beta.yml --publish always $1
fi
