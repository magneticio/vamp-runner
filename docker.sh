#!/usr/bin/env bash

dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

docker_target=${dir}/target/docker

sbt clean assembly

rm -Rf ${docker_target} 2> /dev/null && mkdir -p ${docker_target}
cp $(find "${dir}/target/scala-2.11" -name 'vamp-runner-assembly-*.jar' | sort | tail -1) ${docker_target}/vamp-runner.jar
cp -R ${dir}/conf/logback.xml ${docker_target}
echo "vamp.runner {}" > ${docker_target}/application.conf

cat <<EOF >${docker_target}/run.sh
#!/usr/bin/env bash

APP_CONFIG=/vamp/application.conf
LOG_CONFIG=/vamp/logback.xml

if [ -e "/vamp/conf/application.conf" ] ; then
    APP_CONFIG=/vamp/conf/application.conf
fi

if [ -e "/vamp/conf/logback.xml" ] ; then
    LOG_CONFIG=/vamp/conf/logback.xml
fi

java -jar -Dlogback.configurationFile=\${LOG_CONFIG} -Dconfig.file=\${APP_CONFIG} /vamp/vamp-runner.jar "\$@"
EOF

cat <<EOF >${docker_target}/Dockerfile
FROM java:openjdk-8-jre

ADD vamp-runner.jar application.conf logback.xml run.sh /vamp/
RUN chmod +x /vamp/run.sh

VOLUME ["/vamp/conf"]

ENTRYPOINT ["/vamp/run.sh"]
CMD ["--help"]
EOF

cd ${docker_target}
docker build -t magneticio/vamp-docker:runner .
