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

````markdown
## Day 4 - PostgreSQL 연동 및 Customer API 구현

### 오늘 한 것

- PostgreSQL 17 Docker 이미지 다운로드
- `ai-postgres` PostgreSQL 컨테이너 생성 및 실행
- PostgreSQL 초기 설정
  - Database: `ai_business`
  - User: `aiuser`
- PostgreSQL 컨테이너에 직접 접속하여 SQL 실행 확인
- Docker Network `ai-network` 생성
- `ai-postgres` 컨테이너를 `ai-network`에 연결
- Spring Boot에 Spring Data JPA 의존성 추가
- Spring Boot에 PostgreSQL JDBC Driver 추가
- `application.yaml`에 PostgreSQL 연결 정보 설정
- Windows IntelliJ에서 Ubuntu의 PostgreSQL 컨테이너 연결 성공
- `Customer` Entity 생성
- Hibernate를 통해 `customer` 테이블 자동 생성 확인
- `CustomerRepository` 생성
- 고객 등록 API 구현
  - `POST /api/customers`
- 고객 전체 조회 API 구현
  - `GET /api/customers`
- 고객 단건 조회 API 구현
  - `GET /api/customers/{id}`
- IntelliJ HTTP Client를 이용한 API 테스트
- PostgreSQL에서 실제 데이터 저장 확인

### 주요 명령어

PostgreSQL 이미지 다운로드

```bash
docker pull postgres:17
````

PostgreSQL 컨테이너 생성 및 실행

```bash
docker run -d \
  --name ai-postgres \
  -e POSTGRES_DB=ai_business \
  -e POSTGRES_USER=aiuser \
  -e POSTGRES_PASSWORD=aipass \
  -p 5432:5432 \
  postgres:17
```

실행 중인 컨테이너 확인

```bash
docker ps
```

PostgreSQL 접속

```bash
docker exec -it ai-postgres psql -U aiuser -d ai_business
```

현재 Database와 User 확인

```sql
SELECT current_database(), current_user;
```

테이블 목록 확인

```sql
\dt
```

`customer` 테이블 구조 확인

```sql
\d customer
```

`customer` 데이터 확인

```sql
SELECT * FROM customer;
```

PostgreSQL 종료

```text
\q
```

Docker Network 생성

```bash
docker network create ai-network
```

Docker Network 확인

```bash
docker network ls
```

PostgreSQL 컨테이너를 Docker Network에 연결

```bash
docker network connect ai-network ai-postgres
```

### Spring Boot DB 의존성

`build.gradle`

```gradle
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
runtimeOnly 'org.postgresql:postgresql'
```

### PostgreSQL 연결 설정

Docker 컨테이너 간 통신 시:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://ai-postgres:5432/ai_business
    username: aiuser
    password: aipass

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

Windows IntelliJ에서 Ubuntu PostgreSQL에 직접 접속하여 테스트할 때는
`ai-postgres` 대신 Ubuntu 서버 IP를 사용한다.

```yaml
spring:
  datasource:
    url: jdbc:postgresql://192.168.50.123:5432/ai_business
    username: aiuser
    password: aipass
```

### Customer Entity

```java
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String email;
}
```

### Customer Repository

```java
public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
```

### Customer Controller

```java
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @PostMapping
    public Customer create(@RequestBody Customer customer) {
        return customerRepository.save(customer);
    }

    @GetMapping
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @GetMapping("/{id}")
    public Customer findOne(@PathVariable Long id) {
        return customerRepository.findById(id)
                .orElseThrow();
    }
}
```

### API 테스트

고객 등록

```http
POST http://localhost:9000/api/customers
Content-Type: application/json

{
  "name": "홍길동",
  "email": "hong@test.com"
}
```

고객 전체 조회

```http
GET http://localhost:9000/api/customers
```

고객 단건 조회

```http
GET http://localhost:9000/api/customers/1
```

### 오늘 이해한 개념

* PostgreSQL도 Docker Image를 이용해 Container로 실행할 수 있다.
* `docker run -e` 옵션으로 Container에 환경변수를 전달할 수 있다.
* PostgreSQL Docker Image는 `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` 환경변수를 이용해 초기 DB와 사용자를 생성한다.
* Docker Container끼리 통신하려면 같은 Docker Network에 연결할 수 있다.
* 같은 Docker Network에서는 Container 이름을 호스트명처럼 사용할 수 있다.
* 따라서 Spring Boot Container에서는 PostgreSQL을 `ai-postgres:5432`로 찾을 수 있다.
* Windows에서 실행되는 Spring Boot는 Docker Network의 `ai-postgres`라는 이름을 알 수 없으므로 Ubuntu 서버 IP를 이용해 접속한다.
* JDBC Driver는 Java와 PostgreSQL이 통신할 수 있도록 해준다.
* JPA는 Java 객체와 DB 테이블을 연결해서 다룰 수 있도록 해준다.
* Hibernate는 JPA 구현체로서 실제 SQL 생성 및 DB 작업을 수행한다.
* `@Entity`가 붙은 Java 클래스는 DB 테이블과 매핑할 수 있다.
* `spring.jpa.hibernate.ddl-auto=update` 설정을 통해 Entity를 기반으로 테이블을 자동 생성/변경할 수 있다.
* `JpaRepository`를 상속하면 기본적인 CRUD 기능을 사용할 수 있다.
* `save()`는 데이터를 저장한다.
* `findAll()`은 여러 데이터를 `List<Customer>` 형태로 조회한다.
* `findById()`는 데이터가 존재하지 않을 수도 있기 때문에 `Optional<Customer>`를 반환한다.
* `@PathVariable`을 이용하면 URL에 포함된 값을 Controller의 파라미터로 받을 수 있다.
* Spring Boot는 Java 객체와 `List<Customer>`를 JSON 응답으로 자동 변환한다.
* `@RequestBody`를 이용하면 요청으로 들어온 JSON을 Java 객체로 변환할 수 있다.
* `@GeneratedValue(strategy = GenerationType.IDENTITY)`를 사용하면 PostgreSQL이 PK 값을 자동 생성한다.

### 현재 데이터 흐름

```text
HTTP Request
     ↓
CustomerController
     ↓
CustomerRepository
     ↓
JPA / Hibernate
     ↓
PostgreSQL JDBC Driver
     ↓
PostgreSQL
     ↓
customer Table
```

### 다음 할 일

* Customer 수정 API 구현 (`PUT`)
* Customer 삭제 API 구현 (`DELETE`)
* Customer CRUD 완성
* Spring Boot Container를 `ai-network`에 연결
* Spring Boot Container와 PostgreSQL Container 간 실제 통신 확인
* DB 접속 정보를 환경변수로 분리
* Docker Compose 기초
* Docker Compose로 Spring Boot + PostgreSQL 통합 실행

좋습니다. 오늘은 **Day 5로 정리**하면 되겠습니다. 기존에 정한 `오늘 한 것 → 주요 명령어 → 오늘 이해한 개념 → 다음 할 일` 형식 그대로, `PROJECT_NOTES.md`에 바로 복붙할 수 있게 드리겠습니다.

````markdown
## Day 5 - Customer CRUD 완성 및 Docker Compose 구성

### 오늘 한 것

- 기존 학습 내용 간단히 복습
  - Docker Image와 Container의 관계 복습
  - Docker Network의 역할 복습
  - JPA Entity와 PostgreSQL 테이블의 관계 복습

- Customer 수정 API 구현
  - `PUT /api/customers/{id}`
  - `findById()`로 기존 Customer 조회
  - 요청 Body의 name, email 값으로 기존 Customer 수정
  - `save()`를 이용해 UPDATE 처리
  - HTTP Client를 이용해 수정 정상 동작 확인

- Customer 삭제 API 구현
  - `DELETE /api/customers/{id}`
  - `deleteById()`를 이용해 Customer 삭제
  - Customer CRUD 완성

- Spring Boot 컨테이너와 PostgreSQL 컨테이너 연결
  - `ai-business`와 `ai-postgres`를 같은 Docker Network에서 통신하도록 구성
  - Spring Boot에서 컨테이너 이름 `ai-postgres`를 DB Host로 사용
  - `/api/customers` 호출을 통해 DB 연결 정상 동작 확인

- Docker Compose 학습 및 구성
  - 프로젝트 루트에 `docker-compose.yml` 생성
  - PostgreSQL 서비스를 `ai-postgres`로 정의
  - Spring Boot 서비스를 `ai-business`로 정의
  - PostgreSQL 환경변수 설정
  - Spring Boot 이미지를 Dockerfile을 이용해 자동 Build하도록 설정
  - Spring Boot DB 접속 정보를 환경변수로 분리
  - `depends_on`을 이용해 서비스 의존관계 설정
  - Docker Compose 실행

- Docker Compose 실행 중 문제 해결
  - 기존 PostgreSQL 컨테이너가 Ubuntu 5432 포트를 사용하여 포트 충돌 발생
  - 기존 수동 생성 컨테이너를 제거하여 포트 충돌 해결
  - Spring Boot 컨테이너가 `Exited (1)` 상태로 종료되는 문제 확인
  - `docker compose logs`를 이용해 원인 확인
  - `UnknownHostException: ai-postgres` 확인
  - `docker inspect`를 이용해 실제 Docker Network 연결 상태 확인
  - PostgreSQL 컨테이너를 Compose 기본 Network에 연결
  - `ai-postgres` Network Alias 설정
  - Spring Boot 재시작 후 PostgreSQL 연결 성공
  - `/api/customers` 호출 성공

### 현재 docker-compose.yml

```yaml
services:

  ai-postgres:
    image: postgres:17
    environment:
      POSTGRES_DB: ai_business
      POSTGRES_USER: aiuser
      POSTGRES_PASSWORD: aipass
    ports:
      - "5432:5432"

  ai-business:
    build: .
    ports:
      - "9000:9000"
    environment:
      DB_URL: jdbc:postgresql://ai-postgres:5432/ai_business
      DB_USERNAME: aiuser
      DB_PASSWORD: aipass
    depends_on:
      - ai-postgres
````

### 현재 application.yaml

```yaml
spring:
  application:
    name: backend

  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

server:
  port: 9000
```

### 오늘 사용한 주요 명령어

```bash
# Docker 이미지 확인
docker images

# 실행 중인 컨테이너 확인
docker ps

# Docker Network 확인
docker network ls

# Docker Compose 실행
docker compose up -d --build

# Compose가 관리하는 컨테이너 확인
docker compose ps

# 종료된 컨테이너까지 확인
docker compose ps -a

# Spring Boot 로그 확인
docker compose logs ai-business

# 컨테이너 상세 정보 확인
docker inspect ai-business-system-ai-postgres-1

docker inspect ai-business-system-ai-business-1

# PostgreSQL을 Compose Network에 연결
docker network connect \
  --alias ai-postgres \
  ai-business-system_default \
  ai-business-system-ai-postgres-1

# Spring Boot 서비스 다시 시작
docker compose start ai-business

# API 동작 확인
curl http://localhost:9000/api/customers
```

### 오늘 이해한 개념

#### 1. Docker Image와 Container

* Image는 애플리케이션과 실행 환경을 담은 실행 가능한 패키지
* Container는 Image를 실제로 실행한 인스턴스
* 하나의 Image로 여러 Container를 만들 수 있음
* 새로운 Image를 Build해도 기존 Container가 자동으로 새로운 Image를 사용하는 것은 아님
* 새로운 Image를 실제 서비스에 적용하려면 Container를 새 Image 기준으로 다시 생성해야 함

#### 2. Docker Network

Docker Container끼리 통신하려면 같은 Docker Network에 연결할 수 있다.

같은 Network에 연결된 Container는 Container 또는 Service 이름을 이용해 서로를 찾을 수 있다.

예:

ai-business
↓
ai-postgres:5432
↓
Docker Network
↓
PostgreSQL

따라서 Spring Boot에서는 PostgreSQL의 IP를 직접 지정하지 않고 다음과 같이 사용할 수 있다.

jdbc:postgresql://ai-postgres:5432/ai_business

#### 3. Docker Compose 기본 Network

수동으로 Docker를 구성할 때는 다음 명령어로 Network를 직접 만들었다.

docker network create ai-network

Docker Compose를 사용하면 별도로 Network를 정의하지 않아도 프로젝트 전용 기본 Network를 자동으로 생성한다.

현재 생성된 Network:

ai-business-system_default

기존:

ai-network
├─ ai-business
└─ ai-postgres

Docker Compose:

ai-business-system_default
├─ ai-business
└─ ai-postgres

두 Network는 서로 다른 Network이지만 역할은 동일하다.

#### 4. Docker Compose

Docker Compose는 여러 Container의 실행 설정을 하나의 YAML 파일로 관리한다.

기존에는 직접 다음 작업을 수행했다.

* docker build
* docker run
* 환경변수 지정
* 포트 연결
* Network 생성 및 연결
* Container 시작/종료

Docker Compose를 사용하면 이러한 설정을 `docker-compose.yml`에 선언하고 관리할 수 있다.

docker compose up -d --build

명령으로 Image Build와 여러 서비스를 함께 실행할 수 있다.

#### 5. build: .

ai-business는 Docker Hub에서 완성된 Image를 받아 사용하는 것이 아니라 프로젝트의 Dockerfile을 이용해 직접 Image를 만든다.

따라서:

build: .

을 사용한다.

이는 기존의:

docker build -t ai-business-system .

에서 마지막 `.`과 같은 Build Context 개념이다.

#### 6. 환경변수를 이용한 Spring Boot 설정

기존에는 application.yaml에 DB 정보를 직접 작성했다.

현재는:

url: ${DB_URL}
username: ${DB_USERNAME}
password: ${DB_PASSWORD}

형태로 변경했다.

실제 값은 Docker Compose가 Spring Boot Container에 환경변수로 전달한다.

docker-compose.yml
↓
DB_URL
DB_USERNAME
DB_PASSWORD
↓
ai-business Container
↓
application.yaml
↓
Spring Boot DataSource

이를 통해 실행 환경에 따라 DB 설정을 외부에서 주입할 수 있다.

#### 7. depends_on

ai-business는 ai-postgres에 의존하므로:

depends_on:

* ai-postgres

를 설정했다.

`depends_on`은 Container의 시작 순서를 제어할 수 있지만 PostgreSQL이 실제 DB 요청을 받을 준비가 완료될 때까지 기다리는 것을 보장하는 것은 아니다.

추후 `healthcheck`를 이용해 DB 준비 상태까지 확인하도록 개선할 수 있다.

#### 8. 로그를 이용한 장애 원인 확인

Spring Boot Container가 실행되지 않았을 때:

docker compose ps -a

로 종료 상태를 확인하고:

docker compose logs ai-business

로 애플리케이션 로그를 확인했다.

실제 오류:

UnknownHostException: ai-postgres

이를 통해 단순히 Spring Boot 문제라고 판단하지 않고 Docker Network/DNS 문제라는 것을 확인했다.

`docker inspect`를 이용하면 Container의 Network, 환경변수, 포트 등 실제 Docker 설정을 상세하게 확인할 수 있다.

### 현재 시스템 구조

사용자
│
│ HTTP :9000
▼
Ubuntu
│
▼
Docker Compose
│
└─ ai-business-system_default
│
├─ ai-business
│    └─ Spring Boot
│         │
│         │ JDBC
│         ▼
│
└─ ai-postgres
└─ PostgreSQL
└─ ai_business
└─ customer

Customer API:

POST   /api/customers
GET    /api/customers
GET    /api/customers/{id}
PUT    /api/customers/{id}
DELETE /api/customers/{id}

CRUD 전체 구현 완료.

### 다음 할 일

* Docker Compose Network 구성을 다시 검증
* 수동 `docker network connect` 없이 Compose만으로 전체 환경이 재생성되는지 확인
* PostgreSQL Healthcheck 적용
* PostgreSQL 데이터 영속화를 위한 Volume 학습
* Docker Compose 재시작 / 종료 / 재빌드 명령어 학습
* Customer API 예외 처리

    * 존재하지 않는 Customer 조회 시 404 처리
    * 존재하지 않는 Customer 수정/삭제 처리
* DTO / Service 계층 학습

````

그리고 **다음 시작점은 매우 중요합니다.**

오늘 마지막에 `docker network connect`를 수동으로 실행해서 성공시켰기 때문에, 아직 Compose 구성이 **완전히 끝난 것은 아닙니다.**

다음에는 가장 먼저:

```text
docker compose down
        ↓
docker compose up -d --build
        ↓
아무런 수동 network connect 없이
        ↓
GET /api/customers 성공?
````

을 검증해야 합니다.

이게 성공해야 비로소 **“이 서버를 날리고 다시 만들어도 `docker-compose.yml`만 있으면 환경을 재현할 수 있다”**는 Compose의 핵심을 제대로 달성한 겁니다.

오늘은 CRUD까지 완성하고 Docker Compose에 장애 추적까지 했으니 진도가 꽤 많이 나갔습니다.



````markdown
## Day 6 - Docker Compose 재구성 검증

### 오늘 한 것

- Docker Compose로 Spring Boot + PostgreSQL 전체 환경 재구성
- `docker compose up -d --build` 실행
- `docker-compose.yml`의 `build: .` 설정을 통해 Spring Boot 이미지 자동 빌드
- PostgreSQL 컨테이너 자동 실행
- Spring Boot 컨테이너 자동 실행
- Docker Compose 기본 Network를 통한 컨테이너 간 통신 확인
- 별도의 `docker network connect` 없이 Spring Boot ↔ PostgreSQL 연결 성공
- `/api/customers` API 호출을 통해 전체 구성 정상 동작 확인
- Windows IntelliJ에서도 환경변수를 이용한 로컬 개발 환경 정상 동작 확인

### 주요 명령어

```bash
# docker-compose.yml이 있는 프로젝트 디렉터리로 이동
cd ~/apps/ai-business-system

# Compose 서비스 상태 확인
docker compose ps

# 이미지 빌드 + 컨테이너 생성 및 실행
docker compose up -d --build

# API 동작 확인
curl http://localhost:9000/api/customers
````

### 오늘 이해한 개념

#### 1. Docker Compose를 이용한 Build

기존에는 Spring Boot 이미지를 직접 빌드했다.

```bash
docker build -t ai-business-system .
```

Docker Compose에서는 `docker-compose.yml`에 다음과 같이 정의했다.

```yaml
ai-business:
  build: .
```

따라서:

```bash
docker compose up -d --build
```

를 실행하면 Docker Compose가 `docker-compose.yml`을 읽고 `build: .` 설정에 따라 현재 프로젝트의 Dockerfile을 이용하여 Spring Boot 이미지를 빌드한다.

즉,

docker-compose.yml
↓
build: .
↓
Dockerfile
↓
Spring Boot JAR Build
↓
Docker Image 생성
↓
Container 생성 및 실행

의 순서로 동작한다.

#### 2. Docker Compose의 역할

Docker Compose는 단순히 이미지를 빌드하는 도구가 아니라 여러 컨테이너의 구성과 실행을 하나의 파일로 관리한다.

현재 `docker-compose.yml`은 다음 내용을 관리한다.

* Spring Boot 이미지 빌드
* Spring Boot 컨테이너 실행
* PostgreSQL 이미지 사용
* PostgreSQL 컨테이너 실행
* 포트 연결
* 환경변수 전달
* 서비스 의존관계
* Docker Network 구성

따라서 기존에 여러 `docker build`, `docker run`, `docker network` 명령으로 직접 구성했던 작업을 `docker-compose.yml` 하나로 관리할 수 있다.

#### 3. Compose 기본 Network

Docker Compose는 별도의 Network 설정을 작성하지 않아도 프로젝트용 기본 Network를 자동으로 생성한다.

현재:

ai-business-system_default
├─ ai-business
└─ ai-postgres

두 서비스가 같은 Network에 연결되기 때문에 Spring Boot에서 다음 주소로 PostgreSQL에 접근할 수 있다.

```text
jdbc:postgresql://ai-postgres:5432/ai_business
```

이번 테스트에서는 별도의 `docker network connect` 명령 없이도 컨테이너 간 통신이 정상적으로 이루어지는 것을 확인했다.

#### 4. 환경에 따른 설정값 주입

`application.yaml`에서는 DB 접속정보를 직접 가지고 있지 않고 환경변수를 사용한다.

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

Windows IntelliJ에서 실행할 때는 IntelliJ Run Configuration에서 환경변수를 전달한다.

Docker 환경에서는 `docker-compose.yml`에서 환경변수를 전달한다.

따라서 동일한 Spring Boot 소스를 수정하지 않고 서로 다른 실행 환경에서 사용할 수 있다.

Windows IntelliJ
↓
IntelliJ 환경변수
↓
application.yaml
↓
Ubuntu PostgreSQL

Docker Compose
↓
Compose 환경변수
↓
application.yaml
↓
ai-postgres

### 오늘 최종 확인

```bash
docker compose ps
```

Spring Boot와 PostgreSQL 모두 `Up` 상태 확인.

```bash
curl http://localhost:9000/api/customers
```

응답:

```json
[]
```

HTTP 요청 → Spring Boot → PostgreSQL 조회 → JSON 응답까지 정상 동작 확인.

### 다음 할 일

* Docker Compose 명령어 추가 학습
* PostgreSQL Volume을 이용한 데이터 영속화
* PostgreSQL Healthcheck 적용
* 이후 Spring Boot 애플리케이션 구조 학습

````

그리고 오늘 잡으신 **“docker-compose 파일로 빌드한 거구나”**에서 한 가지만 정확하게 기억하시면 됩니다.

`docker-compose.yml` 자체가 빌드하는 것은 아니고,

> **Docker Compose가 `docker-compose.yml`을 읽고 → `build: .`을 발견하고 → Dockerfile을 사용해서 이미지를 빌드한다.**

입니다.

이 관계만 머릿속에 넣어두시면 됩니다.

```text
docker compose 명령
       ↓
docker-compose.yml
       ↓
    build: .
       ↓
   Dockerfile
       ↓
     Image
       ↓
   Container
````

오늘은 이 정도 정리하고 끝내셔도 딱 좋습니다.
