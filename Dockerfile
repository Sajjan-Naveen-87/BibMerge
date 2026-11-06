# BibMerge in one image: web app on port 8091, or the CLI when given arguments.
#
#   docker build -t bibmerge:final .
#   docker run --rm -p 8091:8091 bibmerge:final            # http://localhost:8091
#   docker run --rm bibmerge:final match demo/paper-a.bib demo/paper-b.bib
#   docker run --rm -v "$PWD":/data bibmerge:final merge /data/a.bib /data/b.bib -o /data/merged.bib

# 1. Front end: build the React app into bibmerge-web/dist.
FROM node:22-bookworm-slim AS web
WORKDIR /src/bibmerge-web
COPY bibmerge-web/package.json bibmerge-web/package-lock.json ./
RUN npm ci
COPY bibmerge-web/ ./
RUN npm run build

# 2. Back end: the API module copies bibmerge-web/dist into the JAR's static/.
#    Tests run in CI (./mvnw verify), so they are skipped here.
FROM eclipse-temurin:21-jdk-jammy AS jar
WORKDIR /src
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY bibmerge-core/pom.xml bibmerge-core/
COPY bibmerge-api/pom.xml bibmerge-api/
RUN ./mvnw -B -q dependency:go-offline -DexcludeReactor=true || true
COPY bibmerge-core/ bibmerge-core/
COPY bibmerge-api/ bibmerge-api/
COPY --from=web /src/bibmerge-web/dist bibmerge-web/dist
RUN ./mvnw -B -q package -DskipTests

# 3. Runtime: JRE only, no build tools, no credentials, non-root user.
FROM eclipse-temurin:21-jre-jammy
RUN useradd --system --create-home bibmerge
WORKDIR /app
COPY --from=jar /src/bibmerge-api/target/bibmerge.jar bibmerge.jar
COPY demo/ demo/
USER bibmerge
EXPOSE 8091
ENTRYPOINT ["java", "-jar", "/app/bibmerge.jar"]
