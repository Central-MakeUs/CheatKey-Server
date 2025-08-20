# 비즈니스 플로우 차트

## 1. 사용자 온보딩 플로우

### 1.1 소셜 로그인 및 회원가입 플로우

```mermaid
flowchart TD
    A[사용자 앱 실행] --> B{이미 로그인된 상태?}
    B -->|Yes| C[홈 화면으로 이동]
    B -->|No| D[로그인 화면 표시]
    
    D --> E[소셜 로그인 선택]
    E --> F{Kakao vs Apple?}
    F -->|Kakao| G[Kakao SDK 로그인]
    F -->|Apple| H[Apple SDK 로그인]
    
    G --> I[ID Token + Access Token 획득]
    H --> I
    
    I --> J[서버에 토큰 전송]
    J --> K{사용자 정보 검증}
    K -->|실패| L[에러 메시지 표시]
    K -->|성공| M{신규 사용자?}
    
    M -->|Yes| N[회원가입 화면 표시]
    M -->|No| O[JWT 토큰 발급]
    
    N --> P[닉네임 입력]
    P --> Q{닉네임 중복 체크}
    Q -->|중복| R[다른 닉네임 입력]
    Q -->|사용 가능| S[약관 동의]
    
    S --> T{필수 약관 동의?}
    T -->|No| U[동의 요청]
    T -->|Yes| V[회원가입 완료]
    
    V --> O
    O --> W[홈 화면으로 이동]
    C --> W
```

### 1.2 약관 동의 플로우

```mermaid
flowchart TD
    A[약관 동의 화면] --> B[약관 목록 조회]
    B --> C{필수 약관 존재?}
    C -->|No| D[선택적 약관만 표시]
    C -->|Yes| E[필수 약관 강조 표시]
    
    D --> F[약관 내용 확인]
    E --> F
    
    F --> G{약관 동의 체크}
    G -->|필수 약관 미동의| H[동의 요청 메시지]
    G -->|모든 약관 동의| I[동의 완료]
    
    H --> F
    I --> J[동의 기록 저장]
    J --> K[회원가입 완료]
```

## 2. 사기 탐지 의사결정 플로우

### 2.1 텍스트 기반 사기 탐지 플로우 (Graph-based State Machine)

```mermaid
flowchart TD
    A[사용자 텍스트 입력] --> B[1단계: 기본 입력 검증]
    B --> C[입력 개선 및 전처리]
    C --> D[2단계: 벡터 검색 KoSimCSE Qdrant]
    D --> E[Top-K 유사 사례 조회]
    
    E --> F{유사도 점수 충분?}
    F -->|No| G[3단계: OpenAI 검증 조건부]
    F -->|Yes| H[4단계: 품질 평가]
    
    G --> I{OpenAI 사용 가능?}
    I -->|No| H
    I -->|Yes| J[OpenAI API 호출]
    J --> K[AI 검증 결과 분석]
    K --> H
    
    H --> L[5단계: 결과 분석 및 의사결정]
    L --> M{최종 위험도 판정}
    M -->|DANGER| N[높은 위험도 경고]
    M -->|WARNING| O[중간 위험도 경고]
    M -->|SAFE| P[안전 메시지]
    
    N --> Q[탐지 기록 저장]
    O --> Q
    P --> Q
    Q --> R[결과 반환]
```

### 2.2 URL 탐지 플로우

```mermaid
flowchart TD
    A[URL 입력] --> B[URL 형식 검증]
    B --> C{유효한 URL?}
    C -->|No| D[URL 형식 오류]
    C -->|Yes| E[URL 정규화]
    
    E --> F[Google Safe Browsing API 호출]
    F --> G{API 응답 확인}
    G -->|실패| H[네트워크 오류]
    G -->|성공| I{위험도 분석}
    
    I --> J{위험 사이트?}
    J -->|Yes| K[위험한 사이트]
    J -->|No| L[안전한 사이트]
    
    K --> M[위험 경고]
    L --> N[안전 메시지]
    D --> U[입력 오류 메시지]
    H --> V[서비스 일시 오류]
    
    M --> O[탐지 기록 저장]
    N --> O
    U --> W
    V --> W
    W --> X[결과 반환]
```

## 3. 커뮤니티 모더레이션 플로우

### 3.1 게시글 작성 및 검증 플로우

```mermaid
flowchart TD
    A[게시글 작성] --> B[사용자 인증 확인]
    B --> C{인증 상태?}
    C -->|미인증| D[로그인 요청]
    C -->|인증됨| E[내용 검증]
    
    E --> F{내용 길이 적절?}
    F -->|Too Short| G[최소 길이 요청]
    F -->|Too Long| H[최대 길이 초과]
    F -->|적절함| I{파일 첨부?}
    
    I -->|Yes| J[파일 업로드]
    I -->|No| K[게시글 저장]
    
    J --> L{파일 검증}
    L -->|실패| M[파일 오류]
    L -->|성공| K
    
    K --> N[게시글 ID 반환]
    N --> O[작성 완료]
    
    D --> P[로그인 화면]
    G --> Q[내용 보완 요청]
    H --> R[내용 축약 요청]
    M --> S[파일 재업로드]
```

### 3.2 신고 처리 플로우

```mermaid
flowchart TD
    A[게시글 신고] --> B[신고 사유 선택]
    B --> C[신고 내용 입력]
    C --> D[신고 기록 저장]
    
    D --> E[해당 게시글 신고 횟수 조회]
    E --> F{신고 횟수 >= 임계값?}
    F -->|No| G[신고 완료]
    F -->|Yes| H[게시글 상태 변경]
    
    H --> I[게시글 상태: REPORTED]
    I --> J[관리자 알림 생성]
    J --> K[신고 완료]
    
    G --> L[사용자에게 신고 완료 알림]
    K --> L
```

### 3.3 댓글 계층 구조 플로우

```mermaid
flowchart TD
    A[댓글 작성] --> B{부모 댓글 존재?}
    B -->|No| C[일반 댓글 작성]
    B -->|Yes| D[대댓글 작성]
    
    C --> E[댓글 저장]
    D --> F[부모 댓글 ID 설정]
    F --> E
    
    E --> G[댓글 목록 조회]
    G --> H[계층 구조 정렬]
    H --> I[댓글 표시]
    
    I --> J{댓글 삭제 요청?}
    J -->|Yes| K[댓글 삭제]
    J -->|No| L[댓글 유지]
    
    K --> M[댓글 삭제 완료]
    L --> N[정상 댓글 표시]
```

## 4. 파일 관리 플로우

### 4.1 파일 업로드 검증 플로우

```mermaid
flowchart TD
    A[파일 선택] --> B[파일 크기 확인]
    B --> C{크기 <= 5MB?}
    C -->|No| D[파일 크기 초과]
    C -->|Yes| E[파일 확장자 확인]
    
    E --> F{허용된 확장자?}
    F -->|No| G[지원하지 않는 파일 형식]
    F -->|Yes| H[파일명 생성]
    
    K --> L[S3 업로드]
    L --> M{업로드 성공?}
    M -->|No| N[업로드 실패]
    M -->|Yes| O[DB에 파일 정보 저장]
    
    O --> P[업로드 완료]
    
    D --> Q[파일 크기 제한 안내]
    G --> R[지원 형식 안내]
    N --> T[재시도 안내]
```

### 4.2 파일 다운로드 플로우

```mermaid
flowchart TD
    A[파일 다운로드 요청] --> B[사용자 인증 확인]
    B --> C{인증 상태?}
    C -->|미인증| D[로그인 요청]
    C -->|인증됨| E[파일 정보 조회]
    
    E --> F{파일 존재?}
    F -->|No| G[파일 없음 오류]
    F -->|Yes| H{파일 접근 권한?}
    
    H -->|No| I[접근 권한 없음]
    H -->|Yes| J[Presigned URL 생성]
    
    J --> K[다운로드 URL 반환]
    K --> L[클라이언트 다운로드]
    L --> M[다운로드 완료]
    
    D --> N[로그인 화면]
    G --> O[파일 없음 메시지]
    I --> P[권한 오류 메시지]
```

## 5. 사용자 활동 추적 플로우

### 5.1 방문 기록 플로우

```mermaid
flowchart TD
    A[사용자 앱 접속] --> B[JWT 토큰 검증]
    B --> C{토큰 유효?}
    C -->|No| D[토큰 갱신 시도]
    C -->|Yes| E[사용자 정보 조회]
    
    E --> F[활동 타입 결정]
    F --> G{활동 타입?}
    G -->|HOME_VISIT| H[홈 방문 기록]
    G -->|MYPAGE_VISIT| I[마이페이지 방문 기록]
    G -->|SOCIAL_LOGIN| J[소셜 로그인 기록]
    G -->|TOKEN_REFRESH| K[토큰 갱신 기록]
    
    H --> L[방문 횟수 증가]
    I --> L
    J --> M[로그인 횟수 증가]
    K --> N[토큰 갱신 기록]
    
    L --> O[마지막 방문 시간 업데이트]
    M --> O
    N --> O
    O --> P[활동 기록 저장]
    P --> Q[정상 응답]
    
    D --> R{갱신 성공?}
    R -->|Yes| S[새 토큰으로 재시도]
    R -->|No| T[로그인 화면]
    S --> B
```

## 6. 에러 처리 플로우

### 6.1 토큰 만료 처리 플로우

```mermaid
flowchart TD
    A[API 요청] --> B[JWT 토큰 검증]
    B --> C{토큰 유효?}
    C -->|Yes| D[정상 처리]
    C -->|No| E{토큰 만료?}
    
    E -->|No| F[토큰 무효 오류]
    E -->|Yes| G[Refresh Token 확인]
    
    G --> H{Refresh Token 존재?}
    H -->|No| I[재로그인 요청]
    H -->|Yes| J[Refresh Token 검증]
    
    J --> K{Refresh Token 유효?}
    K -->|No| I
    K -->|Yes| L[새 Access Token 생성]
    
    L --> M[새 Refresh Token 생성]
    M --> N[토큰 정보 업데이트]
    N --> O[새 토큰 반환]
    O --> P[원래 요청 재시도]
    
    D --> Q[정상 응답]
    F --> R[401 Unauthorized]
    I --> S[로그인 화면]
    P --> Q
```

### 6.2 예외 처리 플로우

```mermaid
flowchart TD
    A[예외 발생] --> B{예외 타입 분류}
    B --> C{비즈니스 예외?}
    C -->|Yes| D[CustomException 처리]
    C -->|No| E{시스템 예외?}
    
    E -->|Yes| F[SystemException 처리]
    E -->|No| G{인증 예외?}
    
    G -->|Yes| H[JwtAuthenticationException 처리]
    G -->|No| I[일반 Exception 처리]
    
    D --> J[ErrorCode 기반 응답]
    F --> K[500 Internal Server Error]
    H --> L[401 Unauthorized]
    I --> M[500 Internal Server Error]
    
    J --> N[에러 로그 기록]
    K --> N
    L --> N
    M --> N
    N --> O[클라이언트 응답]
``` 