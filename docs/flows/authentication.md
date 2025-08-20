# 인증 시스템 구조도

## 1. 전체 인증 아키텍처

```mermaid
graph TB
    subgraph "Client (Mobile/Web)"
        A[사용자] --> B[Native SDK]
        B --> C[소셜 로그인]
    end
    
    subgraph "CheatKey Backend"
        D[AuthController] --> E[AuthSignInService]
        E --> F[KakaoSignInService]
        E --> G[AppleSignInService]
        F --> H[AuthService]
        G --> H
        H --> I[JwtProvider]
        I --> J[RefreshTokenService]
    end
    
    subgraph "External Services"
        K[Kakao API]
        L[Apple API]
    end
    
    subgraph "Security Layer"
        N[JwtAuthenticationFilter]
        O[SecurityConfig]
        P[GlobalExceptionHandler]
    end
    
    subgraph "Database"
        Q[MySQL - Auth Table]
        R[MySQL - Refresh Token Table]
    end
    
    C --> D
    F --> K
    G --> L
    N --> O
    H --> Q
    J --> R
```

## 2. 소셜 로그인 흐름도

### 2.1 Kakao 로그인 흐름

```mermaid
sequenceDiagram
    participant U as 사용자
    participant App as Mobile App
    participant Kakao as Kakao SDK
    participant Server as CheatKey Server
    participant DB as Database
    
    U->>App: 앱 실행
    App->>Kakao: Kakao SDK 초기화
    U->>App: 로그인 버튼 클릭
    App->>Kakao: 로그인 요청
    Kakao->>U: 카카오 로그인 화면
    U->>Kakao: 로그인 정보 입력
    Kakao->>App: ID Token + Access Token 반환
    App->>Server: POST /v1/api/auth/login
    Note over App,Server: {provider: "kakao", idToken: "...", accessToken: "..."}
    
    Server->>Server: AuthSignInService.signIn()
    Server->>Kakao: ID Token 검증
    Kakao->>Server: 사용자 정보 반환
    Server->>DB: 사용자 정보 조회/생성
    DB->>Server: Auth 엔티티 반환
    
    Server->>Server: JWT 토큰 생성
    Note over Server: Access Token (1시간)<br/>Refresh Token (14일)
    
    Server->>DB: Refresh Token 저장
    Server->>App: JWT 토큰 반환
    App->>U: 로그인 완료
```

### 2.2 Apple 로그인 흐름

```mermaid
sequenceDiagram
    participant U as 사용자
    participant App as Mobile App
    participant Apple as Apple SDK
    participant Server as CheatKey Server
    participant DB as Database
    
    U->>App: 앱 실행
    App->>Apple: Apple SDK 초기화
    U->>App: 로그인 버튼 클릭
    App->>Apple: Apple 로그인 화면
    Apple->>U: Apple 로그인 화면
    U->>Apple: Apple 로그인 정보 입력
    Apple->>App: ID Token 반환
    App->>Server: POST /v1/api/auth/login
    Note over App,Server: {provider: "apple", idToken: "..."}
    
    Server->>Server: AuthSignInService.signIn()
    Server->>Apple: ID Token 검증
    Apple->>Server: 사용자 정보 반환
    Server->>DB: 사용자 정보 조회/생성
    DB->>Server: Auth 엔티티 반환
    
    Server->>Server: JWT 토큰 생성
    Server->>DB: Refresh Token 저장
    Server->>App: JWT 토큰 반환
    App->>U: 로그인 완료
```

## 3. JWT 토큰 구조 및 흐름

### 3.1 JWT 토큰 구조

#### Access Token
```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "123",           // 사용자 ID
    "provider": "KAKAO",    // 소셜 제공자
    "role": "ROLE_USER",    // 사용자 권한
    "iat": 1640995200,      // 발급 시간
    "exp": 1640998800       // 만료 시간 (1시간)
  },
  "signature": "HMACSHA256(...)"
}
```

#### Refresh Token
```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "123",           // 사용자 ID
    "role": "ROLE_USER",    // 사용자 권한 (provider 포함 안됨)
    "iat": 1640995200,      // 발급 시간
    "exp": 1641600000       // 만료 시간 (14일)
  },
  "signature": "HMACSHA256(...)"
}
```

### 3.2 JWT 인증 필터 흐름

```mermaid
flowchart TD
    A[HTTP Request] --> B{Authorization Header 존재?}
    B -->|No| C[401 Unauthorized]
    B -->|Yes| D{Bearer 토큰 형식?}
    D -->|No| C
    D -->|Yes| E[토큰 추출]
    E --> F{JWT 토큰 검증}
    F -->|Invalid| C
    F -->|Valid| G[Claims 추출]
    G --> H[Authentication 객체 생성]
    H --> I[SecurityContext 설정]
    I --> J[요청 처리 계속]
```

### 3.3 토큰 갱신 흐름

```mermaid
sequenceDiagram
    participant Client as Client
    participant Server as CheatKey Server
    participant DB as Database
    
    Client->>Server: POST /v1/api/auth/refresh
    Note over Client,Server: {refreshToken: "..."}
    
    Server->>Server: JWT 토큰 검증
    Server->>DB: Refresh Token 조회
    DB->>Server: 토큰 정보 반환
    
    alt 토큰 유효
        Server->>Server: 새로운 Access Token 생성
        Server->>Server: 새로운 Refresh Token 생성
        Server->>DB: 새로운 Refresh Token 저장
        Server->>Client: 새로운 토큰 반환
    else 토큰 무효
        Server->>Client: 401 Unauthorized
    end
```

## 4. 보안 설정 구조

### 4.1 Spring Security 설정

```mermaid
graph TB
    subgraph "Security Configuration"
        A[SecurityConfig] --> B[SecurityFilterChain]
        B --> C[JwtAuthenticationFilter]
        B --> D[JwtExceptionFilter]
        B --> E[CORS Configuration]
    end
    
    subgraph "Filter Chain"
        F[HTTP Request] --> G[CORS Filter]
        G --> H[JwtExceptionFilter]
        H --> I[JwtAuthenticationFilter]
        I --> J[Authorization]
        J --> K[Controller]
    end
    
    subgraph "Exception Handling"
        L[JwtAuthenticationException]
        M[CustomException]
        N[GlobalExceptionHandler]
    end
    
    C --> L
    L --> N
    M --> N
```

### 4.2 인증/인가 흐름

```mermaid
flowchart TD
    A[API 요청] --> B{인증 필요?}
    B -->|No| C[요청 처리]
    B -->|Yes| D{JWT 토큰 존재?}
    D -->|No| E[401 Unauthorized]
    D -->|Yes| F{토큰 유효?}
    F -->|No| E
    F -->|Yes| G{권한 확인}
    G -->|No| H[403 Forbidden]
    G -->|Yes| C
```

## 5. 에러 처리 구조

### 5.1 인증 관련 에러 코드

| 에러 코드 | 설명 | HTTP 상태 |
|-----------|------|-----------|
| `INVALID_TOKEN` | 유효하지 않은 토큰 | 401 |
| `TOKEN_EXPIRED` | 토큰이 만료 되었습니다 | 401 |
| `INVALID_PROVIDER` | 잘못된 소셜 제공자 | 400 |
| `AUTH_UNAUTHORIZED` | 인증 실패 | 401 |
| `AUTH_DUPLICATE_NICKNAME` | 중복된 닉네임 | 409 |
| `REFRESH_TOKEN_NOT_FOUND` | Refresh Token을 찾을 수 없습니다 | 404 |
| `AUTH_ALREADY_REGISTERED` | 이미 가입된 사용자입니다 | 409 |
| `AUTH_REQUIRED_TERMS_NOT_AGREED` | 필수 약관에 모두 동의해야 가입할 수 있습니다 | 400 |
| `AUTH_WITHDRAWN_RECENTLY` | 탈퇴 후 30일이 지나야 재가입이 가능합니다 | 403 |

### 5.2 예외 처리 흐름

```mermaid
flowchart TD
    A[Exception 발생] --> B{Exception 타입}
    B -->|JwtAuthenticationException| C[401 응답]
    B -->|CustomException| D[ErrorCode 기반 응답]
    B -->|기타 Exception| E[500 Internal Server Error]
    
    C --> F[에러 로그 기록]
    D --> F
    E --> F
```

## 6. 데이터베이스 스키마

### 6.1 인증 관련 테이블

#### Auth 테이블
```sql
CREATE TABLE t_auth (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider VARCHAR(20) NOT NULL,           -- KAKAO, APPLE
    provider_id VARCHAR(255) NOT NULL,       -- 소셜 제공자 ID
    email VARCHAR(255),                      -- 이메일
    nickname VARCHAR(50),                    -- 닉네임
    age_code VARCHAR(200),                    -- 연령대 코드
    gender_code VARCHAR(200),                 -- 성별 코드
    trade_method_code VARCHAR(200),           -- 거래 방식 코드
    trade_item_code VARCHAR(200),             -- 거래 품목 코드
    profile_image_id BIGINT,                 -- 프로필 이미지 ID
    login_count INT,                         -- 로그인 횟수
    level INT,                               -- 사용자 레벨
    last_login_at DATETIME,                  -- 마지막 로그인 시간
    total_visit_count INT,                   -- 총 방문 횟수
    last_visit_date DATETIME,                -- 마지막 방문 날짜
    status VARCHAR(20) DEFAULT 'PENDING',    -- PENDING, ACTIVE, WITHDRAWN
    role VARCHAR(20) DEFAULT 'USER',         -- USER, ADMIN, MANAGER
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    withdrawn_at DATETIME,                   -- 탈퇴 시점
    withdrawal_reason VARCHAR(500)           -- 탈퇴 사유
);
```

#### AuthActivity 테이블
```sql
CREATE TABLE t_auth_activity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    auth_id BIGINT NOT NULL,
    activity_type VARCHAR(50) NOT NULL,      -- SOCIAL_LOGIN, TOKEN_REFRESH, HOME_VISIT, MYPAGE_VISIT, REACTIVATION_ATTEMPT, USER_WITHDRAWAL
    ip_address VARCHAR(45),                  -- IP 주소
    user_agent TEXT,                         -- User Agent
    success BOOLEAN DEFAULT TRUE,            -- 성공 여부
    fail_reason TEXT,                        -- 실패 사유
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### RefreshToken 테이블
```sql
CREATE TABLE t_auth_refresh_token (
    user_id BIGINT PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 7. 모니터링 및 로깅

### 7.1 로그 레벨 설정
```yaml
logging:
  level:
    org.springframework.security: DEBUG
    com.cheatkey.common.jwt: DEBUG
    com.cheatkey.module.auth: DEBUG
```

### 7.2 주요 로그 포인트
- 소셜 로그인 시도/성공/실패
- JWT 토큰 생성/검증
- 토큰 갱신 시도/성공/실패
- 인증 실패 (401, 403)
- 사용자 활동 기록
- 방문 기록 (HOME_VISIT, MYPAGE_VISIT)

## 8. 성능 최적화

### 8.1 토큰 검증 최적화
- JWT 토큰 검증을 필터 레벨에서 처리
- MySQL을 통한 Refresh Token 관리
- 토큰 순환 패턴으로 보안성 향상

## 9. 보안 고려사항

### 9.1 토큰 보안
- JWT Secret Key 안전한 관리
- 토큰 만료 시간 적절히 설정 (Access Token: 1시간, Refresh Token: 14일)
- Refresh Token Rotation 구현 (토큰 갱신 시 새로운 토큰 발급)
- 로그아웃/탈퇴 시 Refresh Token 무효화

### 9.2 입력 검증
- 소셜 토큰 검증
- 사용자 입력 데이터 검증
- XSS 방지 (Jsoup.clean() 사용)
- SQL Injection 방지 (JPA 사용)

### 9.3 네트워크 보안
- HTTPS 사용
- CORS 설정
- Spring Security 기반 인증/인가 