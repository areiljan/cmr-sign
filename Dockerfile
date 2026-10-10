# build stage
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
RUN ./gradlew :service:installDist --no-daemon

# runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 app
COPY --from=build --chown=app:app /src/service/build/install/service/ ./
RUN chmod -R a+rX /app
USER app
EXPOSE 8080
ENTRYPOINT ["/app/bin/service"]
