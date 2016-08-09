# Vamp Runner

## Building Docker image

```sh
$ ./build.sh -b
```

This will create the image version related to the last tag (`git describe --abbrev=0`), e.g. `magneticio/vamp-runner:0.8.5`

## Running Docker container

If `$VAMP_API_URL` is set:

```sh
$ docker run --net=host -e VAMP_RUNNER_API_URL=$VAMP_API_URL magneticio/vamp-runner:0.8.5
```

or just something like:

```sh
$ docker run --net=host -e VAMP_RUNNER_API_URL=http://192.168.99.100:8080/api/v1 magneticio/vamp-runner:0.8.5
```

Vamp Runner is accessible on port `8088`, e.g. `http://192.168.99.100:8088`.