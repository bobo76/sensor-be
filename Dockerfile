# Stage 1: build with Maven inside the image (no host JDK/Maven needed)
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

COPY src ./src
RUN ./mvnw -B -q package -DskipTests \
 && cp target/*.jar app.jar

# Stage 2: minimal runtime
FROM eclipse-temurin:25-jre-noble
WORKDIR /app
COPY --from=build /app/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
