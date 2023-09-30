FROM openjdk:17.0.1-jdk-slim
MAINTAINER rlegorreta@legosoft.com.mx
EXPOSE 8190:8190
VOLUME /tmp
ADD target/iamui-1.1.0.jar app.jar

CMD java -jar -Dspring.profiles.active=local app.jar
