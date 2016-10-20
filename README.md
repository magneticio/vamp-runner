# Vamp Runner

- running recipes as a part of [Vamp](https://github.com/magneticio/vamp) integration testing
- running show cases (presentations)

Web UI theme is based on [akveo/blur-admin](https://github.com/akveo/blur-admin).

## Building

Making and copying all files:

```sh
$ ./build.sh -m
```

`target/docker` directory will contain:

```
$ tree target/docker/
  target/docker/
  ├── Dockerfile
  ├── application.conf
  ├── logback.xml
  ├── recipes.tar.bz2
  ├── ui.tar.bz2
  └── vamp-runner.jar
```

Building Docker image (will perform `-m` beforehand):

```sh
$ ./build.sh -b
```

This will create: `magneticio/vamp-runner:katana`

## Running

### Docker container

If `$VAMP_API_URL` is set:

```sh
$ docker run --net=host -e VAMP_RUNNER_API_URL=$VAMP_API_URL magneticio/vamp-runner:katana
```

or just something like:

```sh
$ docker run --net=host -e VAMP_RUNNER_API_URL=http://192.168.99.100:8080/api/v1 magneticio/vamp-runner:katana
```

Vamp Runner is accessible on port `8088`, e.g. `http://192.168.99.100:8088`.

### Vamp deployment

```yaml
name: vamp-runner
gateways:
  8088: vamp-runner/port
clusters:
  vamp-runner:
    services:
      breed:
        name: vamp-runner:katana
        deployable: magneticio/vamp-runner:katana
        ports:
          port: 8088/http
        environment_variables:
          VAMP_RUNNER_API_URL: http://192.168.99.100:8080/api/v1
      scale:
        cpu: 0.2       
        memory: 256MB
        instances: 1
```

Note: override `VAMP_RUNNER_API_URL` value if needed.

### From command line without web UI

Main class: `io.vamp.runner.VampConsoleRunner`

```sh
Usage:
  -h     --help       Print this help.
  -l     --list       List all recipes.
  -a     --all        Run all recipes.
  -r     --run        Run named recipe(s).
```

Example, list all recipes and run `Auto Scaling` and `Canary Release`: 

```sh
$ java \
       -Dvamp.runner.api.url=http://192.168.99.100:8080/api/v1 \
       -Dlogback.configurationFile=logback.xml \
       -Dconfig.file=application.conf \
       -cp vamp-runner.jar \
       io.vamp.runner.VampConsoleRunner --list --run "Auto Scaling" --run "Canary Release"
```

Note: you need to have configuration files, check out [building](#building) section.

### From command line with web UI 

```sh
$ java \
       -Dvamp.runner.api.url=http://192.168.99.100:8080/api/v1 \
       -Dlogback.configurationFile=logback.xml \
       -Dconfig.file=application.conf \
       -jar vamp-runner.jar
```

Note: you need to have configuration files, check out [building](#building) section.
