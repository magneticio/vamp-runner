# Vamp Runner

Running recipes against Vamp.

Example: `java -jar -Dlogback.configurationFile=/PATH_TO/logback.xml -Dconfig.file=/PATH_TO/application.conf /vamp/vamp-runner.jar --help`

```
Usage:
  -h     --help       Print this help.
  -l     --list       List all recipes.
  -a     --all        Run all recipes.
  -r     --run        Run named recipe(s).
```

Building Docker image: `./docker.sh`

Running as Docker container: `docker run magneticio/vamp-docker:runner <params>`
By default it's assumed that Vamp and VGA run on localhost, if that is not the case custom `application.conf` should be provided.

Providing custom application ([application.conf](https://github.com/magneticio/vamp-runner/blob/master/src/main/resources/reference.conf)) and log ([logback.xml](https://github.com/magneticio/vamp-runner/blob/master/conf/logback.xml)) configuration:
`docker run -v /ABS_PATH_TO_CONFIGURATION_DIR/conf:/vamp/conf magneticio/vamp-docker:runner <params>`

Minimal `application.conf` in case of Vamp/VGA hosts:
```
vamp.runner {
  api-url = "..." # e.g. "http://192.168.99.100:8080/api/v1"
  vamp-gateway-agent-host = "..." # e.g. "192.168.99.100"
}
```
