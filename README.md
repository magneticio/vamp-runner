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

Note: override `VAMP_RUNNER_API_URL` value if needed. On Kubernetes you can use `vamp` as the URL for the vamp location. On DC/OS it's `http://10.20.0.100:8080` or `vamp.marathon.mesos`. Make sure you use a port in the `VAMP_RUNNER_API_URL` otherwise Vamp Runner cannot connect to the Vamp API.

Note: replace `katana` references with the version tag of the running Vamp version. 

### DC/OS

Example configuration:

```json
{
  "id": "/vamp-runner",
  "backoffFactor": 1.15,
  "backoffSeconds": 1,
  "container": {
    "portMappings": [
      {
        "containerPort": 8088,
        "hostPort": 0,
        "labels": {
          "VIP_0": "10.20.0.100:8088"
        },
        "protocol": "tcp",
        "servicePort": 10001,
        "name": "vip0"
      }
    ],
    "type": "DOCKER",
    "volumes": [],
    "docker": {
      "image": "magneticio/vamp-runner:katana",
      "forcePullImage": true,
      "privileged": false,
      "parameters": []
    }
  },
  "cpus": 0.5,
  "disk": 0,
  "env": {
    "VAMP_RUNNER_API_URL": "http://10.20.0.100:8080"
  },
  "healthChecks": [
    {
      "gracePeriodSeconds": 30,
      "intervalSeconds": 10,
      "maxConsecutiveFailures": 0,
      "portIndex": 0,
      "timeoutSeconds": 5,
      "delaySeconds": 15,
      "protocol": "TCP"
    }
  ],
  "instances": 1,
  "labels": {
    "DCOS_SERVICE_NAME": "runner",
    "DCOS_SERVICE_SCHEME": "http",
    "DCOS_SERVICE_PORT_INDEX": "0"
  },
  "maxLaunchDelaySeconds": 3600,
  "mem": 1024,
  "gpus": 0,
  "networks": [
    {
      "mode": "container/bridge"
    }
  ],
  "requirePorts": false,
  "upgradeStrategy": {
    "maximumOverCapacity": 1,
    "minimumHealthCapacity": 1
  },
  "killSelection": "YOUNGEST_FIRST",
  "unreachableStrategy": {
    "inactiveAfterSeconds": 0,
    "expungeAfterSeconds": 0
  },
  "fetch": [],
  "constraints": []
}
```

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
