FROM eclipse-temurin:25-jre
WORKDIR /app

# Build the jar first with: ./mvnw clean package -DskipTests
COPY target/*.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
