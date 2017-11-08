# See: http://clarkgrubb.com/makefile-style-guide
SHELL             := bash
.SHELLFLAGS       := -eu -o pipefail -c
.DEFAULT_GOAL     := default
.DELETE_ON_ERROR:
.SUFFIXES:

# Constants, these can be overwritten in your Makefile.local
BUILD_SERVER     := magneticio/buildserver:latest
DIR_SBT       := $(HOME)/.sbt/boot
DIR_IVY       := $(HOME)/.ivy2
DIR_NPM       := $(HOME)/.npm
DIR_GYP       := $(HOME)/.node-gyp
DOCKER_BINARY := docker

# if Makefile.local exists, include it.
ifneq ("$(wildcard Makefile.local)", "")
	include Makefile.local
endif

# Targets
.PHONY: all
all: default

# Using our buildserver which contains all the necessary dependencies
.PHONY: default
default: clean-check
	docker pull $(BUILD_SERVER)
	docker run \
		--rm \
		--volume /var/run/docker.sock:/var/run/docker.sock \
		--volume $(shell command -v $(DOCKER_BINARY)):/usr/bin/docker \
		--volume $(DIR_SBT):/home/vamp/.sbt/boot \
		--volume $(DIR_IVY):/home/vamp/.ivy2 \
		--volume $(DIR_NPM):/home/vamp/.npm \
		--volume $(DIR_GYP):/home/vamp/.node-gyp \
		--volume $(CURDIR):/srv/src \
		--workdir=/srv/src \
		--env BUILD_UID=$(shell id -u) \
		--env BUILD_GID=$(shell stat -c '%g' /var/run/docker.sock) \
		$(BUILD_SERVER) \
			make VAMP_GIT_BRANCH=${VAMP_GIT_BRANCH} build

.PHONY: build
build:
	./build.sh --build

.PHONY: clean
clean:
	rm -rf target project/project project/target
	rm -rf ui/node_modules

.PHONY: clean-check
clean-check:
	if [ $$(find -uid 0 -print -quit | wc -l) -eq 1 ]; then \
		docker run \
		--rm \
		--volume $(CURDIR):/srv/src \
		--workdir=/srv/src \
		$(BUILD_SERVER) \
			make clean; \
	fi
