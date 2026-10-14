# ui build stage
FROM node:24-alpine AS build-ui
WORKDIR /ui
COPY service/ui/ ./
RUN --mount=type=cache,target=/root/.npm npm ci
RUN npm run build

# service build stage
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
RUN ./gradlew :service:installDist --no-daemon

# runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 app
COPY --from=build --chown=app:app /src/service/build/install/service/ ./
COPY --from=build-ui --chown=app:app /ui/build ./ui/public
ARG VERSION=dev
RUN sed -i "s/\\\$VERSION/$VERSION/" ui/public/index.html
RUN chmod -R a+rX /app
USER app
EXPOSE 8080
ENTRYPOINT ["/app/bin/service"]
