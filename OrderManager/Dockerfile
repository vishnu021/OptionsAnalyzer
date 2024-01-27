FROM openjdk:17-jdk-slim
ARG JAR_FILE=target/order-manager.jar
COPY ${JAR_FILE} order-manager.jar
COPY src/main/resources/selfsigned_localhost_sslserver.p12 selfsigned_localhost_sslserver.p12
EXPOSE 8080
ENTRYPOINT ["java","-jar","order-manager.jar"]