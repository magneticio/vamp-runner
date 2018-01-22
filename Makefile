# See: http://clarkgrubb.com/makefile-style-guide
SHELL             := bash
.SHELLFLAGS       := -eu -o pipefail -c
.DEFAULT_GOAL     := default
.DELETE_ON_ERROR:
.SUFFIXES:

# Constants, these can be overwritten in your Makefile.local
BUILD_SERVER     := magneticio/buildserver:latest
DIR_SBT       := "$(HOME)"/.sbt/boot
DIR_IVY       := "$(HOME)"/.ivy2
DIR_NPM       := "$(HOME)"/.npm
DIR_GYP       := "$(HOME)"/.node-gyp

# if Makefile.local exists, include it.
ifneq ("$(wildcard Makefile.local)", "")
	include Makefile.local
endif

# Targets
.PHONY: all
all: default

# Using our buildserver which contains all the necessary dependencies
.PHONY: default
default:
	docker pull $(BUILD_SERVER)
	docker run \
		--rm \
		--volume /var/run/docker.sock:/var/run/docker.sock \
		--volume $(DIR_SBT):/home/vamp/.sbt/boot \
		--volume $(DIR_IVY):/home/vamp/.ivy2 \
		--volume $(DIR_NPM):/home/vamp/.npm \
		--volume $(DIR_GYP):/home/vamp/.node-gyp \
		--volume "$(CURDIR)":/srv/src \
		--workdir=/srv/src \
		--env BUILD_UID=$(shell id -u) \
		--env BUILD_GID=$(shell stat -c '%g' /var/run/docker.sock) \
		$(BUILD_SERVER) \
			make VAMP_GIT_BRANCH=${VAMP_GIT_BRANCH} VAMP_TAG_PREFIX=${VAMP_TAG_PREFIX} build

.PHONY: build
build:
	./build.sh --build

.PHONY: clean
clean:
	rm -rf target project/project project/target
	rm -rf ui/node_modules
