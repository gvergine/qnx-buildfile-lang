#!/bin/bash
docker run -it --rm \
    --name my-maven-project \
    -v "$(pwd)/qnx.buildfile.lang.parent":/usr/src/mymaven \
    -v "$HOME/.m2":/var/maven/.m2 \
    -u 1000 -e MAVEN_CONFIG=/var/maven/.m2 \
    -w /usr/src/mymaven maven:amazoncorretto \
    mvn -Duser.home=/var/maven \
        -Djdk.xml.maxGeneralEntitySizeLimit=0 \
        -Djdk.xml.totalEntitySizeLimit=0 \
        $@

