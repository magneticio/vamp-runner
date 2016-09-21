FROM java:8-jre-alpine

RUN mkdir -p /usr/local/vamp-runner

ADD ui.tar.bz2 vamp-runner.jar recipes.tar.bz2 application.conf logback.xml /usr/local/vamp-runner/

CMD ["java", "-jar", "-Dlogback.configurationFile=/usr/local/vamp-runner/logback.xml", "-Dconfig.file=/usr/local/vamp-runner/application.conf", "/usr/local/vamp-runner/vamp-runner.jar"]
