# BE
Dentruth Backend Server

## 1. 프로젝트 개요
외국인 대상의 치과 관련 서비스의 백엔드 API 서버입니다.
도메인 로직 보호와 유지보수성을 최우선으로 고려한 레이어드 아키텍처를 기반으로 설계되었습니다.

## 2. 아키텍처
패키지 구조
```
src/main/java/com/Dentruth/{domain}
│
├── presentation (controller)
│   ├── controller
│   └── dto (Request)
│
├── application (service)
│   ├── service
│   └── dto (Request, Response)
│
├── domain
│   ├── entity
│   └── repository
│
└── infra
    └── external api 구현체
```

핵심 원칙
의존성 방향: presentation → application → domain
도메인 로직은 외부(DB, API, 프레임워크)로부터 완전히 분리
순환 참조 방지

## 3. 설계 원칙
### 1) 도메인 중심 설계
핵심 비즈니스 로직은 Entity에 위치
Service는 트랜잭션 관리 및 흐름 제어 역할만 수행
### 2) DTO 전략
Controller Request / Service Request 분리
Response는 Service 계층에서 관리
### 3) 트랜잭션 관리
트랜잭션 범위 최소화
외부 API 호출은 트랜잭션 외부에서 수행
### 4) Infra 분리
외부 API는 interface(application) + 구현(infra) 구조

## 4. 공통 구조
```
common/
 ├── exception
 ├── response
 └── aop
config/
 └── 설정 클래스
```
 
## 5. 인증 / 인가
JWT 기반 인증
Spring Security 적용 예정

## 6. 예외 처리
커스텀 예외 코드 정의
글로벌 예외 핸들러 적용

## 7. API 문서
Swagger
Notion 병행 관리

## 8. 인프라
배포
AWS EC2
HTTPS 적용 예정
CI/CD
추후 구축 예정
로깅 / 모니터링
로깅: @Slf4j
모니터링: Grafana + Prometheus + Loki


## 9. 데이터 설계
간접 참조 방식
Soft Delete 정책 (법적 요구사항 기반 확정 예정)


## 10. 개발 규칙
- 브랜치 전략
 : 기능 단위 브랜치 생성
 : 이슈 기반 작업
- 코드 리뷰
 : CodeRabbit 사용
- 로그 정책
 : CUD 작업 중심 기록
 : 사용자 행위 추적 로그 남김

## 11. 커밋 타입
| Type     | 설명              | 예시            |
| -------- | --------------- | ------------- |
| feat     | 기능 추가           | 회원 가입 API 추가  |
| fix      | 버그 수정           | 로그인 토큰 오류 수정  |
| refactor | 리팩토링 (동작 변경 없음) | 서비스 로직 구조 개선  |
| docs     | 문서 수정           | README 업데이트   |
| test     | 테스트 코드 추가/수정    | 회원 서비스 테스트 추가 |
| chore    | 빌드, 설정 등 기타 작업  | gradle 설정 변경  |
| style    | 코드 스타일 변경 (비기능) | 공백, 포맷팅 수정    |


## 12. 테스트
단위 테스트 + 통합 테스트 지향

