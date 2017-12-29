FROM openjdk:8
LABEL maintainer="Bogdan Marian <satrapu@users.noreply.github.com>"

ARG JAVA_DEBUG_PORT
ADD target/jdbc-with-docker-jar-with-dependencies.jar /opt/app/app.jar

WORKDIR /opt/app

# Debug port
EXPOSE $JAVA_DEBUG_PORT

# Start Java application using environment variable JAVA_OPTIONS
CMD java $JAVA_OPTIONS -jar app.jar