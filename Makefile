# See: http://clarkgrubb.com/makefile-style-guide
SHELL             := bash
.SHELLFLAGS       := -eu -o pipefail -c
.DEFAULT_GOAL     := default
.DELETE_ON_ERROR:
.SUFFIXES:

# Constants, these can be overwritten in your Makefile.local
CONTAINER := magneticio/buildserver:latest

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
	docker pull $(CONTAINER)
	docker run \
		--rm \
		-e VAMP_GIT_BRANCH=${VAMP_GIT_BRANCH} \
		--volume /var/run/docker.sock:/var/run/docker.sock \
		--volume $(shell command -v docker):/usr/bin/docker \
		--volume $(CURDIR):/srv/src \
		--volume $(HOME)/.sbt:/root/.sbt \
		--volume $(HOME)/.ivy2:/root/.ivy2 \
		--workdir=/srv/src \
		$(CONTAINER) \
			make build


.PHONY: build
build:
	./build.sh --build


.PHONY: clean
clean:
	./build.sh --remove
