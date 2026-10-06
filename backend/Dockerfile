# 타깃 두 가지:
#   runtime (기본)  : 소스에서 빌드. 어떤 플랫폼(Railway 등)에서도 이 파일만으로 이미지가 만들어진다.
#   prebuilt        : CI가 테스트를 통과시킨 build/libs/app.jar를 그대로 담는다. 서버 배포(deploy/deploy.sh)용.

FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:17-jre AS base
RUN useradd --system --uid 10001 --no-create-home app
WORKDIR /app
# 컨테이너 메모리 제한의 75%를 힙으로 쓴다. OOM이면 즉시 종료해 재시작되게 한다.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
USER app
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

FROM base AS prebuilt
COPY --chown=app build/libs/app.jar /app/app.jar

FROM base AS runtime
COPY --from=build --chown=app /src/build/libs/app.jar /app/app.jar
