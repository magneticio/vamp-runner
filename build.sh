#!/usr/bin/env bash

dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

reset=`tput sgr0`
red=`tput setaf 1`
green=`tput setaf 2`
yellow=`tput setaf 3`

if [ "$(git describe --tags)" = "$(git describe --abbrev=0)" ]; then
    version=`git describe --tags`
else
    version="katana"
fi

target=${dir}'/target'
target_docker=${target}'/docker'

cd ${dir}

function parse_command_line() {
    flag_help=0
    flag_make=0
    flag_build=0

    for key in "$@"
    do
    case ${key} in
        -h|--help)
        flag_help=1
        ;;
        -m|--make)
        flag_make=1
        ;;
        -b|--build)
        flag_make=1
        flag_build=1
        ;;
        *)
        ;;
    esac
    done
}

function join { local IFS="$1"; shift; echo "$*"; }

function print_help() {
    echo "${green}Usage of $0:${reset}"
    echo "${yellow}  -h  |--help       ${green}Help.${reset}"
    echo "${yellow}  -m  |--make       ${green}Copy Docker files to '${target}' directory.${reset}"
    echo "${yellow}  -b  |--build      ${green}Build Docker images.${reset}"
}

function sbt_make {
    cd ${dir}
    echo "${green}building jar: ${yellow}sbt clean test assembly${reset}"
    sbt clean test assembly
    mkdir -p ${target_docker}
    cp $(find "${target}/scala-2.11" -name 'vamp-runner-assembly-*.jar' | sort | tail -1) ${target_docker}/vamp-runner.jar
}

function gulp_make {
    cd ${dir}/ui
    echo "${green}building ui${reset}"
    rm -Rf bower_components node_modules release ui ui.tar.bz2 2> /dev/null

    npm install \
    bower \
      gulp \
      gulp-cli

    npm install
    ./node_modules/.bin/bower --allow-root install
    ./node_modules/.bin/gulp build

    mv release ui && tar -cvjSf ui.tar.bz2 ui
    mv ui release && mv ui.tar.bz2 ${target_docker}/
}

function docker_make {
    cd ${dir}
    echo "${green}copying recipes${reset}"
    tar -cvjSf recipes.tar.bz2 recipes
    mv recipes.tar.bz2 ${target_docker}/

    echo "${green}generating configuration files${reset}"
    cp ${dir}/conf/logback.xml ${target_docker}

    recipes=()

    for file in `find recipes | grep recipe.json`
    do
      [[ ${file} != *"/"* ]]
        recipes+=(\""/usr/local/vamp-runner/${file}\"")
    done

    recipe_list=`join , "${recipes[@]}"`

    cat > ${target_docker}/application.conf << EOF
vamp.runner {
  info.interval = 10 seconds
  http {
    port = 8088
    ui {
      index = "/usr/local/vamp-runner/ui/index.html"
      directory = "/usr/local/vamp-runner/ui"
    }
  }
  recipes {
    timeout = {
      short = 5 seconds
      long  = 60 seconds
    }
    files = [
      ${recipe_list}
    ]
  }
}
EOF

    echo "${green}generating docker file${reset}"
    cat > ${target_docker}/Dockerfile << EOF
FROM java:8-jre-alpine

RUN mkdir -p /usr/local/vamp-runner

ADD ui.tar.bz2 vamp-runner.jar recipes.tar.bz2 application.conf logback.xml /usr/local/vamp-runner/

CMD ["java", "-jar", "-Dlogback.configurationFile=/usr/local/vamp-runner/logback.xml", "-Dconfig.file=/usr/local/vamp-runner/application.conf", "/usr/local/vamp-runner/vamp-runner.jar"]
EOF
}

function docker_build {
    cd ${target_docker}
    echo "${green}building docker image: ${yellow}magneticio/vamp-runner:${version}${reset}"
    docker build -t magneticio/vamp-runner:${version} .
}

function process() {
    sbt_make
    gulp_make
    docker_make

    if [ ${flag_build} -eq 1 ]; then
      docker_build
    fi

    echo "${green}done.${reset}"
}

parse_command_line $@

echo "${green}
██╗   ██╗ █████╗ ███╗   ███╗██████╗     ██████╗ ██╗   ██╗███╗   ██╗███╗   ██╗███████╗██████╗
██║   ██║██╔══██╗████╗ ████║██╔══██╗    ██╔══██╗██║   ██║████╗  ██║████╗  ██║██╔════╝██╔══██╗
██║   ██║███████║██╔████╔██║██████╔╝    ██████╔╝██║   ██║██╔██╗ ██║██╔██╗ ██║█████╗  ██████╔╝
╚██╗ ██╔╝██╔══██║██║╚██╔╝██║██╔═══╝     ██╔══██╗██║   ██║██║╚██╗██║██║╚██╗██║██╔══╝  ██╔══██╗
 ╚████╔╝ ██║  ██║██║ ╚═╝ ██║██║         ██║  ██║╚██████╔╝██║ ╚████║██║ ╚████║███████╗██║  ██║
  ╚═══╝  ╚═╝  ╚═╝╚═╝     ╚═╝╚═╝         ╚═╝  ╚═╝ ╚═════╝ ╚═╝  ╚═══╝╚═╝  ╚═══╝╚══════╝╚═╝  ╚═╝
                                                                               by magnetic.io
${reset}"

if [ ${flag_help} -eq 1 ] || [[ $# -eq 0 ]]; then
    print_help
fi

if [ ${flag_make} -eq 1 ] || [ ${flag_build} -eq 1 ]; then
    rm -Rf ${target} 2> /dev/null
    process
fi
