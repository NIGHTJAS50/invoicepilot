FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN mkdir -p out/classes && find src/main/java -name '*.java' > sources.txt && javac -d out/classes @sources.txt && cp -r src/main/resources/. out/classes/

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/out ./out
COPY src/main/resources ./src/main/resources
ENTRYPOINT ["java", "-cp", "out/classes", "com.nightjas50.invoicepilot.cli.InvoicePilotApp"]