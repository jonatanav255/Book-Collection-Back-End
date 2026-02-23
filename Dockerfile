FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "echo $GOOGLE_CREDENTIALS_JSON > /app/gcloud-creds.json && export GOOGLE_APPLICATION_CREDENTIALS=/app/gcloud-creds.json && java -jar app.jar"]
