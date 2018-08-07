# OS detection adapted from: https://gist.github.com/sighingnow/deee806603ec9274fd47
OSFLAG 	:=
LEIN 	:=
NPM := $(shell command -v npm 2> /dev/null)
JLINK := $(shell command -v jlink 2> /dev/null)

ifeq ($(OS),Windows_NT)
	LEIN := $(shell command -v lein.bat 2> /dev/null)
	OSFLAG := -w
else
	LEIN := $(shell command -v lein 2> /dev/null)
	UNAME_S := $(shell uname -s)
	ifeq ($(UNAME_S),Linux)
		OSFLAG := -l
	endif
	ifeq ($(UNAME_S),Darwin)
		OSFLAG := -m
	endif
	ifeq ($(UNAME_S),CYGWIN_NT-10.0)
		LEIN := $(shell command -v lein.bat 2> /dev/null)
		OSFLAG := -w
	endif
endif

package: electron package-only

build-deps:
ifndef LEIN
	$(error "Leiningen not found, please install from https://leiningen.org")
endif
ifndef NPM
	$(error "npm not found, please install from https://nodejs.org")
endif

clean: build-deps
	@echo Cleaning up...
	@eval $(LEIN) clean
	@rm -rf ./dist

deps: clean
	@echo Fetching Leiningen dependencies...
	@eval $(LEIN) deps

npm-deps: clean
	@echo Fetching NPM dependencies...
	@npm install

sass:
	@echo Building CSS...
	@eval $(LEIN) sass4clj once

cljs-main: deps npm-deps
	@echo Building ClojureScript for main electron process...
	@eval $(LEIN) cljsbuild once main

cljs-view: deps npm-deps
	@echo Building ClojureScript for electron view process...
	@eval $(LEIN) cljsbuild once view

cljs-updater: deps npm-deps
	@echo Building ClojureScript for electron updater process...
	@eval $(LEIN) cljsbuild once updater

electron: sass cljs-main cljs-view cljs-updater

package-only:
	@echo Building executable...
	./node_modules/.bin/electron-builder $(OSFLAG)

beta: electron
	@echo Publishing beta - requires S3 credentials in ENV...
	./node_modules/.bin/electron-builder -c electron-builder-beta.yml --publish always $(OSFLAG)

release: electron
	@echo Publishing release - requires S3 credentials in ENV...
	./node_modules/.bin/electron-builder --publish always $(OSFLAG)
