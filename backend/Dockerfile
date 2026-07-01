FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY backend/pom.xml ./backend/pom.xml
COPY backend/src ./backend/src
COPY frontend ./frontend
RUN mvn -B -f backend/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /workspace/backend/target/todolist-project-1.0-SNAPSHOT.jar app.jar
COPY --from=build /workspace/frontend ./frontend
EXPOSE 8080
ENV PORT=8080
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
