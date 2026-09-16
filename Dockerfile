# 빌드 스테이지: Gradle로 부팅 가능한 jar 생성
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 의존성 캐싱: gradle 설정만 먼저 복사해 의존성을 미리 받음
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# 소스 복사 후 빌드 (테스트 제외)
COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test

# 실행 스테이지: JRE만 포함한 경량 이미지
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

# 컨테이너 내 표준 포트
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
