FROM ubuntu:16.04

ENV JAVA_OPTS "-Xms4096m -Xmx4096m -XX:MaxPermSize=512m -D"
ENV JAVA_TOOL_OPTIONS "-Dfile.encoding=UTF-8"

RUN apt-get update \
    && apt-get install -y git openjdk-8-jdk-headless tomcat7 \
    && git clone --recursive --depth 1 https://github.com/infolis/infoLink /tmp/infoLink \
    && cd /tmp/infoLink  \
    &&     ./gradlew war  \
    &&     mv build/libs/infoLink-1.0.war /var/lib/tomcat7/webapps  \
    && echo "JAVA_OPTS=\"$JAVA_OPTS\"" >> /etc/default/tomcat7  \
    && echo "JAVA_TOOL_OPTIONS=\"$JAVA_TOOL_OPTIONS\"" >> /etc/default/tomcat7  \
    && apt-get remove -y --auto-remove --purge git  \
    && rm /var/lib/apt/lists/* /root/.gradle /tmp/infoLink

EXPOSE 8080
RUN ['service', 'tomcat7', 'start']
