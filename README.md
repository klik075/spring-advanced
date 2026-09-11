# 붉은 달의 성채 게임 저장 서버 구현
게임의 진행 상태와 덱 정보를 저장하고 조회할 수 있는 게임 서버 구현 프로젝트

발제 링크 : https://app.notion.com/p/teamsparta/260908-3d52dc3ef51480219c13f80a19e0939e

프로젝트 기간 : 2026.9.11(금) 총 1일

## 1. API 명세

| 메소드 | 경로                       | 성공 | 하는 일                        |
|--------|----------------------------|-----:|--------------------------------|
| POST   | `/games`                   |  201 | 게임 생성 및 시작 덱 생성      |
| GET    | `/games`                   |  200 | 게임 목록 조회 (ID 내림차순)   |
| GET    | `/games/{gameId}`          |  200 | 게임 상세 정보 및 전체 덱 조회 |
| PATCH  | `/games/{gameId}`          |  204 | 플레이어 이름 변경             |
| PUT    | `/games/{gameId}/progress` |  200 | 진행 상태 및 전체 덱 저장      |
| DELETE | `/games/{gameId}`          |  204 | 게임 및 덱 삭제                |
| GET    | `/rankings`                |  200 | 시즌 클리어 랭킹 조회          |

## 2. 에러 응답
| 상태 | 언제                                                                                            |
| --- |-------------------------------------------------------------------------------------------------|
| 400 | 요청 본문 형식, enum 값, 필드 제약 위반. 경로·쿼리 파라미터에 숫자가 아닌 값을 보낸 경우도 포함 |
| 404 | 존재하지 않는 게임 ID                                                                           |
| 409 | 이미 끝난 게임에 대한 진행 저장                                                                 |

## 3. 미션
### Lv 1. 설정 파일 작성: Docker MySQL 연결
Git Clone 후 `docker compose up -d` 명령어를 실행하여 MySQL 컨테이너를 생성하고 실행한다.

해당 명령어는 `docker-compose.yml` 파일의 설정을 기준으로 필요한 Docker 리소스를 구성한다.

로컬에 필요한 이미지가 없는 경우 Docker가 이미지를 먼저 가져온 후 컨테이너를 생성하고 실행한다.

---
### Lv 2. 의존성 주입(DI)
Spring 애플리케이션이 시작되면 `@SpringBootApplication`에 포함된 Component Scan이 실행된다.

Component Scan은 지정된 패키지와 하위 패키지를 탐색하면서 `@Component`, `@Service`, `@Repository`, `@Controller` 등의 애노테이션이 붙은 클래스를 찾는다.

Spring은 이렇게 찾은 클래스의 객체를 생성하고 IoC Container에 등록하여 관리한다. 이처럼 Spring이 생성하고 관리하는 객체를 Bean이라고 한다.

IoC Container는 Spring이 Bean을 생성하고 저장하며, Bean 사이의 의존성 주입(DI)과 생명주기 등을 관리하는 컨테이너다.

GameService 클래스에 `@Service`를 붙이면 Component Scan을 통해 해당 클래스를 발견하고, Spring이 GameService 객체를 Bean으로 생성하여 IoC Container에 등록한다. 

이후 GameController가 GameService를 의존할 경우 Spring이 IoC Container에 등록된 GameService Bean을 주입(DI)한다.

---
### Lv 3. RESTful API: 게임 목록 조회
RESTful API에서는 URL에 동작을 표현하기보다 리소스를 명사로 표현하고, HTTP Method를 통해 행위를 구분하는 것이 일반적이다.

GameController의 `@GetMapping("/game")`보다 여러 Game 리소스의 컬렉션을 나타내는 `@GetMapping("/games")`가 더 적절하고 API 명세의 경로를 보면 `/games`로 정의 되어 있다.

RESTful API에서는 URL에 get, create, delete와 같은 동작을 포함하지 않고, HTTP Method인 GET, POST, PUT, PATCH, DELETE 등을 사용하여 리소스에 대한 동작을 표현한다.

Spring에서는 `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping` 등의 애노테이션을 사용하여 각각의 HTTP Method 요청을 처리한다.

---
### Lv 4. @Transactional
Transaction(트랜잭션)은 하나의 논리적인 작업 단위다. 

Transaction의 핵심적인 특징 중 하나는 원자성(Atomicity)이다. 하나의 Transaction에 포함된 작업은 모두 성공하여 반영되거나, 중간에 문제가 발생하면 전체 작업이 롤백되어 이전 상태로 되돌아간다.

메서드에 @Transactional을 적용하면 Spring이 해당 메서드의 실행을 하나의 Transaction으로 관리한다. 메서드가 정상적으로 종료되면 COMMIT하여 변경 사항을 반영하고, Transaction 중 예외가 발생하면 ROLLBACK하여 변경 사항을 취소한다.

`@Transactional`은 다음과 같은 경우에 주로 사용한다.

1. 여러 데이터를 하나의 작업 단위로 변경해야 하는 경우
2. 여러 테이블의 데이터를 함께 변경해야 하는 경우
3. JPA의 변경 감지(Dirty Checking)를 사용하는 경우

조회와 같이 데이터를 변경하지 않는 작업에는 `@Transactional(readOnly = true)`를 사용할 수 있다. 이는 해당 Transaction이 읽기 전용이라는 의도를 Spring과 JPA에 전달한다

반대로 createGame()처럼 Game과 Deck을 생성하고 저장하는 작업에 `@Transactional(readOnly = true)`를 사용하면 안 된다. createGame()은 데이터를 변경하는 작업이므로 일반적인 `@Transactional`을 사용해야 한다.

`@Transactional`에서 중요한 개념 중 하나가 Transaction Boundary(트랜잭션 경계)다. `@Transactional`이 적용된 메서드가 일반적인 Spring 프록시를 통해 호출되면 해당 메서드의 실행 범위를 하나의 Transaction으로 관리한다.

Spring 애플리케이션에서는 하나의 비즈니스 작업을 담당하는 Service 계층에 Transaction Boundary를 두는 경우가 일반적이다. Service 메서드에서 여러 Repository를 호출하더라도 하나의 Transaction으로 묶어 작업의 일관성을 유지할 수 있다.

---
### Lv 5. Bean Validation: 게임 생성
Bean Validation은 객체의 필드에 검증 규칙(Constraint)을 선언하고, 객체가 해당 규칙을 만족하는지 검사하는 표준 기술​이다.

Spring Boot에서는 일반적으로 Hibernate Validator를 Bean Validation의 구현체로 사용하여 실제 검증을 수행한다.

주요 Validation Constraint 종류

- @NotNull : 값이 null인지 검사한다.
- @NotEmpty : null과 빈 값 ""을 허용하지 않는다.
- @NotBlank : null, 빈 문자열, 공백만 있는 문자열을 모두 허용하지 않는다.
- @Size(min = 값, max = 값) : 문자열, Collection 등의 크기를 검사한다. (min = 2, max = 10)을 같이 사용하여 최소, 최대를 지정한다.
- @Min(값) : 숫자 필드가 값 이상이어야 한다.
- @Max(값) : 숫자 필드가 값 이하이어야 한다.
- @Positive : 숫자 필드가 0보다 커야 한다.
- @PositiveOrZero : 숫자 필드가 0 이상이어야 한다.
- @Email : 이메일 형식인지 검사한다.

`@Valid`는 Controller의 요청 객체에 선언된 Bean Validation Constraint를 검사하도록 검증을 트리거하는 역할을 한다.

Controller에서 HTTP Method를 Mapping한 메서드의 매개변수로 `@RequestBody`는 HTTP 요청 Body의 JSON 데이터를 Dto 객체로 변환하고, `@Valid`는 변환된 객체에 선언된 @NotBlank, @Size, @NotNull 등의 Constraint를 검사한다.

검증에 실패하면 Controller의 비즈니스 로직이 실행되기 전에 검증 예외가 발생하므로, Controller에서 입력값을 직접 검사하는 코드를 반복해서 작성할 필요가 없다.

따라서 Bean Validation을 사용하면 외부에서 들어오는 잘못된 입력을 비즈니스 로직에 전달하기 전에 검증하고, 입력값 검증에 대한 코드를 간결하게 유지할 수 있다.

---
### Lv 6. 보상 카드 선택과 진행 저장
`ResponseEntity`는 Spring Controller에서 HTTP 응답 전체를 명시적으로 표현하기 위한 클래스다.

HTTP 응답은 크게 다음 세 가지로 구성된다.

- `Status`: HTTP 상태 코드
- `Headers`: 응답 헤더
- `Body`: 클라이언트에게 전달할 데이터

ResponseEntity를 사용하면 이 세 가지를 Spring 코드에서 직접 지정할 수 있다.

Controller에서 `ResponseEntity`를 사용하지 않고 객체를 반환하면, Spring은 반환된 객체를 JSON 등의 형태로 변환하여 HTTP Body에 넣고 기본적으로 200 OK 응답을 생성한다.

`ResponseEntity`를 사용하는 경우 GameController의 `ResponseEntity<GameDetailResponse>` 반환은 HTTP 응답의 Body가 `GameDetailResponse`인 `ResponseEntity`라고 생각하면 된다.

자주 사용하는 Status가 200인 경우 `ResponseEntity.ok(bodyType)` 처럼 사용 가능하다.

반환할 데이터가 필요하지 않다면 Body가 없는 `ResponseEntity.noContent().build();`로 반환할 수 있다. 반환형은 `ResponseEntity<Void>`를 사용한다.
여기서 `noContent()`는 응답 상태를 204로 설정하는 메서드이고 `build()`는 지금까지 설정한 내용을 가지고 최종적인 ResponseEntity 객체를 생성하는 메서드다.

`ResponseEntity<T>`에서 T는 Body의 타입을 말한다.

`ResponseEntity<?>`에서 `?`는 Java의 와일드 카드로 Body의 타입이 무엇인지는 특정하지 않지만, 어떤 타입이든 들어올 수 있다는 의미이다.

---
### Lv 7. 저장된 여정 이어하기













