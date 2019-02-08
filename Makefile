# OS detection adapted from: https://gist.github.com/sighingnow/deee806603ec9274fd47
OSFLAG 	:=
LEIN 	:=
YARN := $(shell command -v yarn 2> /dev/null)
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

package: install package-only

build-deps:
ifndef LEIN
	$(error "Leiningen not found, please install from https://leiningen.org")
endif
ifndef YARN
	$(error "yarn not found, please install from https://yarnpkg.com")
endif
ifndef JLINK
	$(error "jlink not found, please install JDK10+")
endif

clean: build-deps
	@echo Cleaning up...
	@rm -rf ./bin
	@rm -rf ./dist
	@eval $(LEIN) clean
	@rm -f ./yarn.lock

deps: clean
	@echo Fetching Leiningen dependencies...
	@eval $(LEIN) deps

npm-deps: clean
	@echo Fetching NPM dependencies...
	@yarn install

sass:
	@echo Building CSS...
	@eval $(LEIN) sass4clj once

cljs: deps npm-deps
	@echo Building ClojureScript for main electron process...
	@eval $(LEIN) cljsbuild once main
	@echo Building ClojureScript for electron renderer process...
	@eval $(LEIN) cljsbuild once view
	@echo Building ClojureScript for electron updater process...
	@eval $(LEIN) cljsbuild once updater

figwheel:
	@lein cljs-figwheel

install: clean deps sass cljs

package-only:
	@echo Building executable...
	./node_modules/.bin/electron-builder $(OSFLAG)

publish-github:
	@echo Publishing to GitHub Releases - requires GH_TOKEN in ENV...
	./node_modules/.bin/electron-builder -c electron-builder.yml --publish always $(OSFLAG)

release: install publish-github
