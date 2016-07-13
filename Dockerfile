# Run as
# $ docker run --rm -p 8080:8090 
FROM tomcat:7-jre8

RUN apt-get update && apt-get install -y git openjdk-8-jdk-headless
#     && rm /var/lib/apt/lists/* /root/.gradle /tmp/infoLink
# RUN git clone --recursive --depth 1 https://github.com/infolis/infoLink /tmp/infoLink

# hackety hack to cache gradle deps download time
RUN mkdir -p /tmp/infoLink
ADD build.gradle settings.gradle gradle.properties gradlew /tmp/infoLink/
ADD gradle /tmp/infoLink/gradle
ADD keywordTagging /tmp/infoLink/keywordTagging
RUN cd /tmp/infoLink && ./gradlew build -x compileJava

ADD . /tmp/infoLink

ENV JAVA_OPTS "-Xms4096m -Xmx4096m -XX:MaxMetaspaceSize=512m"
ENV JAVA_TOOL_OPTIONS "-Dfile.encoding=UTF-8"
RUN cd /tmp/infoLink  \
    && ./gradlew clean war  \
    && mv -vt $CATALINA_HOME/webapps/ build/libs/infoLink-1.0.war
    # && echo "JAVA_OPTS=\"$JAVA_OPTS\"" >> /etc/default/tomcat7  \
    # && echo "JAVA_TOOL_OPTIONS=\"$JAVA_TOOL_OPTIONS\"" >> /etc/default/tomcat7

RUN touch /tmp/infolis.log && chmod a+rw /tmp/infolis.log
VOLUME /infolis
