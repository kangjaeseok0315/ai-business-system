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
