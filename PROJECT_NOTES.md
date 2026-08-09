# AI Business System

## 목표
6개월 안에 돈 받고 납품 가능한 AI 웹 시스템을 혼자 구축한다.

## 현재 환경
- Windows 개발 PC
- Ubuntu 베어본 서버
- Java 21
- Spring Boot 4.1
- Git / GitHub
- Docker 설치됨

## Day 1
- Spring Boot 프로젝트 생성
- `/api/hello` API 생성
- Git 초기화 및 첫 커밋
- GitHub 저장소 생성 및 push
- Ubuntu 서버에 Java 21 설치
- Ubuntu에서 GitHub 프로젝트 clone / pull
- Gradle build 성공
- Spring Boot 9000 포트 실행
- 외부 접속 성공

## Day 2 - Spring Boot Docker 컨테이너화

### 오늘 한 것
- 프로젝트 루트에 `Dockerfile` 생성
- Java 21 JRE 기반 Docker 이미지 구성
- Spring Boot JAR 파일을 Docker 이미지에 포함
- `ai-business-system` Docker 이미지 빌드 성공
- Docker 컨테이너 `ai-business` 생성 및 실행
- Ubuntu 9000 포트와 컨테이너 9000 포트 연결
- `/api/hello` 호출 정상 동작 확인
- `docker stop`, `docker start`로 컨테이너 중지/재실행 확인

### 오늘 사용한 주요 명령어

```bash
./gradlew build

docker build -t ai-business-system .

docker images

docker run -d --name ai-business -p 9000:9000 ai-business-system

docker ps

curl http://localhost:9000/api/hello

docker stop ai-business

docker start ai-business
```
### 오늘 이해한 개념

- Dockerfile = Docker 이미지를 만드는 설명서
- Image = 애플리케이션과 실행 환경을 포장한 결과물
- Container = Image를 실제 실행한 것
- `-p 9000:9000` = Ubuntu 9000 포트와 컨테이너 9000 포트를 연결
- Docker 이미지에 Java를 포함할 수 있으므로 운영 서버에 Java를 직접 설치할 필요는 없음
- 현재는 `./gradlew build`를 Docker 밖에서 하기 때문에 Ubuntu에 Java가 필요함
- 추후 Multi-stage Build를 사용하면 빌드까지 Docker 안에서 처리 가능

### 다음 할 일

- Docker Multi-stage Build 적용
- PostgreSQL Docker 컨테이너 구성
- Spring Boot와 PostgreSQL 연결
- Docker Compose 기초

## Day 3 - Docker Multi-stage Build

### 오늘 한 것

- 기존 Dockerfile을 Multi-stage Build 방식으로 변경
- Ubuntu의 기존 `build` 폴더를 삭제한 상태에서 Docker 이미지 빌드 진행
- Ubuntu에서 직접 `./gradlew build`를 실행하지 않고 Docker 내부에서 Spring Boot 빌드
- Builder 단계에서 Java 21 JDK를 이용해 Spring Boot 실행용 JAR 생성
- 실행 단계에서는 Java 21 JRE와 생성된 JAR만 사용
- `ai-business-system` Docker 이미지 빌드 성공
- 기존 `ai-business` 컨테이너 제거
- 새로 빌드한 이미지로 `ai-business` 컨테이너 재생성
- Ubuntu 9000 포트와 Container 9000 포트 연결
- `/api/hello` 내부 호출 정상 확인
- 외부 브라우저에서 `/api/hello` 정상 접속 확인
- 실행 중인 Container 내부에 접속하여 `/app/app.jar` 확인

### 현재 Dockerfile

```dockerfile
# 1단계: Spring Boot 빌드
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY . .

RUN ./gradlew clean bootJar --no-daemon


# 2단계: Spring Boot 실행
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 주요 명령어

```bash
# 기존 빌드 결과 삭제
rm -rf build

# Docker 내부에서 빌드 + Image 생성
docker build -t ai-business-system .

# 기존 Container 정지 및 삭제
docker stop ai-business
docker rm ai-business

# 새 Image로 Container 생성
docker run -d --name ai-business -p 9000:9000 ai-business-system

# 실행 상태 확인
docker ps

# Spring Boot API 확인
curl http://localhost:9000/api/hello

# 실행 중인 Container 내부 접속
docker exec -it ai-business sh

# Container 내부 확인
pwd
ls -lh /app

# Container에서 나오기
exit
```

### 오늘 이해한 개념

- Multi-stage Build는 **빌드 환경과 실제 실행 환경을 분리하는 방식**이다.
- 첫 번째 `builder` 단계에서는 `JDK 21`과 Gradle Wrapper를 사용하여 Java 소스를 컴파일하고 Spring Boot JAR를 만든다.
- 두 번째 실행 단계에서는 빌드에 필요한 JDK나 Gradle은 버리고 `JRE 21 + app.jar`만 사용한다.
- 따라서 실제 운영 서버에는 Java나 Gradle을 직접 설치하지 않아도 되고 Docker만 있으면 애플리케이션을 빌드하고 실행할 수 있다.
- `COPY --from=builder`는 앞 단계에서 만들어진 파일을 최종 Image로 가져오는 명령이다.
- `COPY . .`는 현재 프로젝트 소스를 Builder Container의 `/app`으로 복사한다.
- `./gradlew clean bootJar`의 `clean`은 이전 빌드 결과를 삭제하고, `bootJar`는 Spring Boot 실행용 JAR를 생성한다.
- Day 2에서 사용한 `./gradlew build`는 일반 JAR와 Spring Boot 실행 JAR가 함께 만들어질 수 있기 때문에 `*.jar`를 사용했을 때 충돌이 발생했다.
- Day 3에서는 `clean bootJar`만 실행하므로 실행용 JAR 하나만 생성되어 `*.jar`로 복사할 수 있다.
- Docker Image는 실행 프로그램의 포장본이고 Container는 그 Image를 실제로 실행한 것이다.
- Image를 새로 `docker build`해도 이미 실행 중인 Container가 자동으로 새 Image로 변경되지는 않는다.
- 새로운 Image를 적용하려면 기존 Container를 제거하고 새로운 Image를 기반으로 Container를 다시 생성해야 한다.
- 최종 Container 내부에서는 Dockerfile의 `WORKDIR /app` 설정 때문에 실행용 JAR가 `/app/app.jar`로 존재한다.

### Day 2와 Day 3의 차이

Day 2:

```text
Ubuntu
 ├─ Java 21
 ├─ ./gradlew build
 │      ↓
 │     JAR
 │
 └─ docker build
        ↓
      Image
        ↓
    Container
```

Day 3:

```text
Ubuntu
 │
 └─ Docker
      │
      ├─ Builder Stage
      │    ├─ JDK 21
      │    ├─ Gradle
      │    └─ 소스 → JAR
      │
      └─ Runtime Stage
           ├─ JRE 21
           └─ app.jar
                ↓
            Container
```

### 다음 할 일

- PostgreSQL Docker Container 구성
- Spring Boot에 PostgreSQL Driver / JPA 추가
- Spring Boot와 PostgreSQL Container 연결
- 간단한 테이블을 만들고 실제 데이터 저장/조회
- 이후 Docker Compose로 Spring Boot + PostgreSQL 통합 관리