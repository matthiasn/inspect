#!/usr/bin/env bash

sudo apt-get install openjdk-8-jre-headless
sudo apt-get install openssl
sudo apt-get install libsasl2-dev
sudo apt-get install liblz1
sudo apt-get install icnsutils
sudo apt-get install graphicsmagick
sudo apt install ruby-sass
sudo apt-get install libgconf-2-4

curl https://sh.rustup.rs -sSf | sh
source $HOME/.profile

curl -o- https://raw.githubusercontent.com/creationix/nvm/v0.33.5/install.sh | bash
source $HOME/.bashrc
nvm install 8.7

npm install -g electron
npm install -g electron-builder
npm install -g electron-cli
npm install -g electron-build-env
npm install -g electron-publisher-s3
npm install -g neon
npm install -g neon-cli
npm install -g node-gyp

npm install
npm run build
