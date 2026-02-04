# Team Java Backend Style Guide (무명소)

이 문서는 **무명소 Java 백엔드 팀의 코드 스타일 및 설계 가이드**입니다.  
Gemini AI 코드 리뷰어는 이 문서를 기준으로 코드 품질과 일관성을 평가합니다.

코드는 단순히 “동작하는 코드”를 넘어  
**유지보수성 · 일관성 · 확장성 · 운영 안정성**을 함께 고려해야 합니다.

리뷰는 이해를 돕기 위해 **한글**로 작성하며,  
아래 가이드를 기본으로 하되 더 나은 방향이 있다면 합리적으로 판단하여 제안합니다.

---

## 1. 패키지 구조 및 계층 분리

다음과 같은 계층형 구조를 사용합니다.

- `controller` : HTTP 요청/응답 처리
- `service` : 비즈니스 로직 처리
- `facade` : 여러 Service를 조합하여 유스케이스 단위의 흐름을 제어하는 계층(패턴)
- `repository` : 데이터베이스 접근
- `dto` : API 입출력 객체
- `domain` / `entity` : 도메인 모델, JPA Entity, Mongo Document

### 기본 원칙
- 각 계층은 자신의 역할에만 집중합니다.
- Controller는 로직을 직접 구현하지 않고 Service 또는 Facade에 위임합니다.
- Facade는 비즈니스 규칙을 담지 않고, 트랜잭션 경계 및 흐름 제어에 집중합니다.
- 하위 계층이 상위 계층에 의존하지 않도록 합니다.

---

## 2. 네이밍 컨벤션 (Naming Conventions)

- 클래스: `UpperCamelCase`
  - 예: `UserController`, `PostService`
- 메서드 / 변수: `lowerCamelCase`
  - 예: `getPostList`, `userId`
- boolean 변수: `is` / `has` / `can` 접두어 사용
  - 예: `isDeleted`, `hasPermission`
- 상수: `UPPER_CASE_WITH_UNDERSCORES`
- 패키지: `lowercase.with.dots`
- DTO: `Request`, `Response` 접미어 명시
  - 예: `LoginRequest`, `PostPreviewResponse`

---

## 3. DTO와 Entity 분리

- Entity / Mongo Document는 API 응답에 직접 노출하지 않습니다.
- Request / Response DTO는 명확히 분리합니다.
- DTO 변환 책임은 Service 또는 전용 Mapper에 둡니다.
- Request DTO에는 검증 어노테이션을 명시합니다.
  - `@NotNull`, `@Size`, `@Valid` 등

---

## 4. 트랜잭션 관리 규칙 (DB별 구분)

### JPA / RDB
- 조회 메서드: `@Transactional(readOnly = true)` 권장
- 쓰기 메서드: `@Transactional` 명시
- dirty checking을 적극 활용합니다.
- 트랜잭션 경계는 Service 또는 Facade에서만 정의합니다.

### MongoDB
- MongoDB는 dirty checking이 기본적으로 동작하지 않습니다.
- `MongoTemplate` / `MongoRepository` 기반 조회 로직에서  
  `@Transactional(readOnly = true)`는 성능 최적화 효과가 없습니다.
- MongoDB 조회 메서드에 트랜잭션 어노테이션을 습관적으로 사용하지 않습니다.
- 트랜잭션은 **multi-document 정합성이 필요한 경우에만** 명시적으로 사용합니다.

---

## 5. MongoDB 사용 규칙

- 정렬/검색이 포함된 조회는 반드시 인덱스 존재 여부를 확인합니다.
- 조회 로직은 **무제한 결과를 반환하지 않도록** 설계합니다.
- 목록성 조회는 페이지네이션을 사용합니다.
- 개수 또는 범위가 명확한 조회는 limit 또는 조건으로 결과 크기를 제한합니다.
- Aggregation 사용 시 다음 순서를 권장합니다.
  - `$match → $project / $sort → $limit`
- `$sample`은 비용이 크므로:
  - 사용 사유와 호출 빈도를 명확히 합니다.
  - 리뷰 시 성능 영향을 반드시 검토합니다.

---

## 6. 예외 처리 및 로깅

### 예외 처리
- 전역 예외 처리(`@ControllerAdvice`)를 사용합니다.
- 가능한 한 **구체적인 예외 클래스**를 정의합니다.
  - 예: `PostNotFoundException`, `InsufficientCoinException`
- 비즈니스 예외(4xx)와 시스템 예외(5xx)를 명확히 구분합니다.
- 비즈니스 규칙 위반은 팀 표준 HTTP Status로 통일합니다.

### 로깅
- 로그에는 요청 식별자(traceId), userId(익명 포함), endpoint 정보를 포함합니다.
- `INFO`: 정상 흐름
- `WARN`: 복구 가능한 비즈니스/외부 연동 실패
- `ERROR`: 시스템 오류 (stack trace 포함)
- 민감 정보(비밀번호, 토큰 원문)는 로그에 출력하지 않습니다.

---

## 7. 코드 스타일 및 문법

- 들여쓰기: 4 spaces
- 한 줄 길이: 120자 이내
- `@Override`, `@Transactional`, `@Slf4j` 등 어노테이션은 명확히 작성합니다.
- early return 패턴을 권장합니다.
- 불필요한 else 블록은 지양합니다.
- 메서드는 한 가지 역할만 수행하며, 20~30라인 이내를 권장합니다.
- 단일 메서드에서만 사용되는 단순 로직은 과도하게 유틸로 분리하지 않습니다.

---

## 8. 비즈니스 로직 분리 및 멱등성 규칙

- Controller에는 비즈니스 로직을 두지 않고, 요청 위임만 담당합니다.
- 여러 Service가 엮이는 흐름은 Helper 또는 Facade로 분리하여 관리합니다.
- 상태 변경이 발생하는 비즈니스 로직은:
  - 단일 트랜잭션 경계 내에서 처리합니다.
  - 변경의 의도와 책임이 코드 구조(메서드명/도메인 객체)에서 드러나야 합니다.
- “한 번만 처리되어야 하는” 로직은:
  - 애플리케이션 레벨의 조건 분기(if)보다
  - 데이터베이스 제약(유니크 인덱스 등)을 통해 보장하는 것을 우선합니다.
- 데이터 무결성을 보장하기 위한 예외(예: 중복 키)는
  - 실패가 아닌 정상적인 제어 흐름으로 처리할 수 있습니다.

---

## 9. API 응답 포맷

- 모든 API 응답은 공통 응답 포맷(`ApiResponse`)을 사용합니다.
- 성공/실패 구조를 통일합니다.
- 에러 코드는 enum으로 관리합니다.
- HTTP Status와 에러 코드는 1:1 매핑을 원칙으로 합니다.

---

## 10. 보안 및 인증 규칙

- Controller는 인증 객체에서 **userId만 추출**하여 Service로 전달합니다.
- 토큰 원문 또는 전체 claims를 Service에 전달하지 않습니다.
- 권한 검사는 기본적으로 Service/Facade에서 수행합니다.
- 엔드포인트 단위 제어가 필요한 경우 `@PreAuthorize`를 사용합니다.
- 인증/비인증(ignore) 경로는 명시적으로 문서화합니다.

---

## 11. 테스트 원칙

- 단위 테스트는 JUnit 5를 사용합니다.
- 테스트 메서드명은 `shouldDoSomething_whenCondition` 형식을 따릅니다.
- given-when-then 패턴을 지향합니다.
- Service 단위 테스트를 우선합니다.
- MongoDB Repository 테스트는 slice test 기준으로 작성합니다.
- 스케줄러 테스트는 KST 기준 시간을 고정하여 검증합니다.
- `@Async`, 재시도 로직은 재시도 횟수와 최종 실패를 검증합니다.

---

## 12. Gemini 코드 리뷰 기준 (내재 규칙)

Gemini는 아래 기준으로 코드를 리뷰합니다.

- 계층 분리가 올바른가?
- DB 특성(JPA vs MongoDB)을 고려한 설계인가?
- 트랜잭션 사용이 합리적인가?
- 비즈니스 규칙이 일관되고 멱등적인가?
- 테스트 가능성과 유지보수성이 높은가?

### 리뷰 우선순위
1. **치명적 문제**: 정합성, 보안, 장애 가능성
2. **권장 사항**: 성능, 유지보수성, 구조 개선
3. **제안 사항**: 스타일, 가독성