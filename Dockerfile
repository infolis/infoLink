# Run as
# docker run -d \
#   -p 8080:8080 \
#   -v ./docker/infolis-config.properties:/etc/infolis-config.properties \
#   -v ./docker/infolis-files:/infolis-files \
#   --name infolink-app \
#   infolis/infolink
FROM tomcat:7-jre8

RUN apt-get update && apt-get install -y openjdk-8-jdk-headless

# hackety hack to cache gradle deps and reduce download time for subsequent builds
RUN mkdir -p /tmp/infoLink
ADD build.gradle settings.gradle gradle.properties gradlew /tmp/infoLink/
ADD gradle /tmp/infoLink/gradle
ADD keywordTagging /tmp/infoLink/keywordTagging
RUN cd /tmp/infoLink && ./gradlew build -x compileJava -x compileTestJava

ADD . /tmp/infoLink

ENV JAVA_OPTS "-Xms4096m -Xmx12g -Xloggc:/tmp/infolis-gc.log -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:-PrintTenuringDistribution -XX:+PrintGCCause -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=2M"
ENV JAVA_TOOL_OPTIONS "-Dfile.encoding=UTF-8"
RUN cd /tmp/infoLink  \
    && ./gradlew clean war  \
    && mv -vt $CATALINA_HOME/webapps/ build/libs/infoLink-1.0.war
