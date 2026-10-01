FROM eclipse-temurin:21-jre
WORKDIR /app
COPY build/libs/execution-service-1.0.0.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
