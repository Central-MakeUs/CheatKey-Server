# 기술적 시퀀스 다이어그램

## 1. 사기 탐지 시퀀스

### 1.1 텍스트 기반 사기 탐지

```mermaid
sequenceDiagram
    participant U as 사용자
    participant App as Mobile App
    participant Server as CheatKey Server
    participant Embedding as Embedding Server
    participant Qdrant as Qdrant Vector DB
    participant DB as MySQL DB
    
    U->>App: 텍스트 입력
    App->>Server: POST /v1/api/detection/case
    Note over App,Server: {text: "사기 텍스트", type: "CASE"}
    
    Server->>Server: 텍스트 전처리
    Server->>Embedding: POST /v1/embed
    Note over Server,Embedding: {text: "전처리된 텍스트"}
    
    Embedding->>Embedding: 벡터 임베딩 생성
    Embedding->>Server: 벡터 반환
    Note over Embedding,Server: {vector: [0.1, 0.2, ...]}
    
    Server->>Qdrant: 벡터 유사도 검색
    Qdrant->>Server: 유사 사례 목록 반환
    Note over Qdrant,Server: {results: [{id: "case1", score: 0.95}, ...]}
    
    Server->>Server: 결과 분석 및 점수 계산
    Server->>DB: 탐지 기록 저장
    DB->>Server: 저장 완료
    
    Server->>App: 탐지 결과 반환
    Note over Server,App: {status: "SUSPICIOUS", score: 0.95, matchedCase: {...}}
    App->>U: 결과 표시
```

### 1.2 URL 기반 사기 탐지

```mermaid
sequenceDiagram
    participant U as 사용자
    participant App as Mobile App
    participant Server as CheatKey Server
    participant Google as Google Safe Browsing
    participant DB as MySQL DB
    
    U->>App: URL 입력
    App->>Server: POST /v1/api/detection/url
    Note over App,Server: {url: "https://suspicious-site.com"}
    
    Server->>Server: URL 검증 및 정규화
    Server->>Google: Safe Browsing API 호출
    Note over Server,Google: {url: "https://suspicious-site.com"}
    
    Google->>Server: 위험도 정보 반환
    Note over Google,Server: {threatType: "MALWARE", threatLevel: "HIGH"}
    
    Server->>DB: 탐지 기록 저장
    DB->>Server: 저장 완료
    
    Server->>App: URL 탐지 결과 반환
    Note over Server,App: {status: "DANGEROUS", threatType: "MALWARE"}
    App->>U: 경고 메시지 표시
```

## 2. 커뮤니티 시퀀스

### 2.1 게시글 작성 및 조회

```mermaid
sequenceDiagram
    participant U as 사용자
    participant App as Mobile App
    participant Server as CheatKey Server
    participant S3 as AWS S3
    participant DB as MySQL DB
    
    U->>App: 게시글 작성
    App->>Server: POST /v1/api/community/posts
    Note over App,Server: {title: "...", content: "...", files: [...]}
    
    Server->>Server: JWT 토큰 검증
    Server->>Server: 내용 검증 (XSS 방지)
    
    alt 파일 첨부 있음
        Server->>S3: 파일 업로드
        S3->>Server: 업로드 완료
        Server->>DB: 파일 정보 저장
    end
    
    Server->>DB: 게시글 저장
    DB->>Server: 저장 완료
    
    Server->>App: 게시글 ID 반환
    App->>U: 작성 완료
    
    U->>App: 게시글 목록 조회
    App->>Server: GET /v1/api/community/posts?page=1&size=10
    
    Server->>DB: 게시글 목록 조회
    DB->>Server: 게시글 목록 반환
    
    Server->>App: 게시글 목록 반환
    App->>U: 목록 표시
```

### 2.2 댓글 작성 및 계층형 구조

```mermaid
sequenceDiagram
    participant U as 사용자
    participant App as Mobile App
    participant Server as CheatKey Server
    participant DB as MySQL DB
    
    U->>App: 댓글 작성
    App->>Server: POST /v1/api/community/posts/{postId}/comments
    Note over App,Server: {content: "댓글 내용", parentId: null}
    
    Server->>Server: JWT 토큰 검증
    Server->>DB: 댓글 저장
    DB->>Server: 저장 완료
    
    Server->>App: 댓글 정보 반환
    App->>U: 댓글 표시
    
    U->>App: 대댓글 작성
    App->>Server: POST /v1/api/community/posts/{postId}/comments
    Note over App,Server: {content: "대댓글", parentId: "commentId"}
    
    Server->>DB: 대댓글 저장 (parent_id 설정)
    DB->>Server: 저장 완료
    
    Server->>App: 대댓글 정보 반환
    App->>U: 계층형 댓글 표시
```

### 2.3 게시글 신고 처리

```mermaid
sequenceDiagram
    participant U as 사용자
    participant App as Mobile App
    participant Server as CheatKey Server
    participant DB as MySQL DB
    
    U->>App: 게시글 신고
    App->>Server: POST /v1/api/community/posts/{postId}/report
    Note over App,Server: {reasonCode: "SPAM"}
    
    Server->>Server: JWT 토큰 검증
    Server->>DB: 신고 기록 저장
    DB->>Server: 저장 완료
    
    Server->>DB: 신고 횟수 확인
    DB->>Server: 신고 횟수 반환
    
    alt 신고 횟수 임계값 초과
        Server->>DB: 게시글 상태 변경 (REPORTED)
        DB->>Server: 상태 변경 완료
    end
    
    Server->>App: 신고 처리 완료
    App->>U: 신고 완료 메시지
```

## 3. 파일 업로드 시퀀스

### 3.1 파일 업로드 프로세스

```mermaid
sequenceDiagram
    participant U as 사용자
    participant App as Mobile App
    participant Server as CheatKey Server
    participant S3 as AWS S3
    participant DB as MySQL DB
    
    U->>App: 파일 선택
    App->>Server: POST /v1/api/files/upload
    Note over App,Server: {file: MultipartFile, folder: "COMMUNITY"}
    
    Server->>Server: JWT 토큰 검증
    Server->>Server: 파일 검증 (크기, 확장자)
    
    alt 파일 검증 실패
        Server->>App: 에러 응답
        App->>U: 에러 메시지 표시
    else 파일 검증 성공
        Server->>Server: 파일명 생성 (UUID)
        Server->>S3: 파일 업로드
        S3->>Server: 업로드 완료
        
        Server->>DB: 파일 정보 저장
        DB->>Server: 저장 완료
        
        Server->>App: 파일 정보 반환
        Note over Server,App: {fileId: 123, s3Key: "uploads/...", url: "..."}
        App->>U: 업로드 완료
    end
```

### 3.2 파일 다운로드 프로세스

```mermaid
sequenceDiagram
    participant U as 사용자
    participant App as Mobile App
    participant Server as CheatKey Server
    participant S3 as AWS S3
    participant DB as MySQL DB
    
    U->>App: 파일 다운로드 요청
    App->>Server: GET /v1/api/files/{fileId}/download
    
    Server->>Server: JWT 토큰 검증
    Server->>DB: 파일 정보 조회
    DB->>Server: 파일 정보 반환
    
    Server->>S3: Presigned URL 생성
    S3->>Server: Presigned URL 반환
    
    Server->>App: Presigned URL 반환
    App->>S3: 파일 다운로드
    S3->>App: 파일 스트림
    App->>U: 파일 저장
```

## 4. 사용자 활동 추적 시퀀스

### 4.1 홈 대시보드 방문

```mermaid
sequenceDiagram
    participant U as 사용자
    participant App as Mobile App
    participant Server as CheatKey Server
    participant DB as MySQL DB
    
    U->>App: 앱 실행
    App->>Server: GET /v1/api/home/dashboard
    Note over App,Server: Authorization: Bearer {token}
    
    Server->>Server: JWT 토큰 검증
    Server->>DB: 사용자 정보 조회
    DB->>Server: 사용자 정보 반환
    
    Server->>DB: 방문 기록 저장
    DB->>Server: 저장 완료
    
    Server->>DB: 통계 정보 조회
    DB->>Server: 통계 정보 반환
    
    Server->>App: 대시보드 정보 반환
    Note over Server,App: {userInfo: {...}, stats: {...}, recentDetections: [...]}
    App->>U: 대시보드 표시
```

### 4.2 마이페이지 조회

```mermaid
sequenceDiagram
    participant U as 사용자
    participant App as Mobile App
    participant Server as CheatKey Server
    participant DB as MySQL DB
    
    U->>App: 마이페이지 접근
    App->>Server: GET /v1/api/mypage/profile
    Note over App,Server: Authorization: Bearer {token}
    
    Server->>Server: JWT 토큰 검증
    Server->>DB: 사용자 상세 정보 조회
    DB->>Server: 사용자 정보 반환
    
    Server->>DB: 활동 기록 조회
    DB->>Server: 활동 기록 반환
    
    Server->>DB: 탐지 기록 조회
    DB->>Server: 탐지 기록 반환
    
    Server->>App: 마이페이지 정보 반환
    Note over Server,App: {profile: {...}, activities: [...], detections: [...]}
    App->>U: 마이페이지 표시
```

## 5. 에러 처리 시퀀스

### 5.1 토큰 만료 처리

```mermaid
sequenceDiagram
    participant U as 사용자
    participant App as Mobile App
    participant Server as CheatKey Server
    participant DB as MySQL DB
    
    U->>App: API 요청
    App->>Server: GET /v1/api/community/posts
    Note over App,Server: Authorization: Bearer {expired_token}
    
    Server->>Server: JWT 토큰 검증
    Server->>App: 401 Unauthorized
    Note over Server,App: {error: "EXPIRED_TOKEN"}
    
    App->>Server: POST /v1/api/auth/refresh
    Note over App,Server: {refreshToken: "..."}
    
    Server->>DB: Refresh Token 검증
    DB->>Server: 토큰 정보 반환
    
    Server->>Server: 새로운 Access Token 생성
    Server->>DB: 새로운 Refresh Token 저장
    DB->>Server: 저장 완료
    
    Server->>App: 새로운 토큰 반환
    App->>Server: 원래 요청 재시도
    Server->>App: 정상 응답
    App->>U: 결과 표시
```

## 6. 성능 최적화 시퀀스

### 6.1 페이징을 활용한 게시글 조회

```mermaid
sequenceDiagram
    participant U as 사용자
    participant App as Mobile App
    participant Server as CheatKey Server
    participant DB as MySQL DB
    
    U->>App: 게시글 목록 조회
    App->>Server: GET /v1/api/community/posts?page=1&size=10&category=GENERAL
    
    Server->>Server: JWT 토큰 검증
    Server->>DB: 게시글 목록 조회 (페이징)
    Note over Server,DB: LIMIT 10 OFFSET 0
    
    DB->>Server: 게시글 목록 반환
    Server->>DB: 전체 게시글 수 조회
    DB->>Server: 전체 개수 반환
    
    Server->>App: 페이징 정보와 함께 게시글 목록 반환
    Note over Server,App: {content: [...], totalElements: 150, totalPages: 15, currentPage: 1}
    App->>U: 게시글 목록 표시
```

### 6.2 인덱스를 활용한 검색 최적화

```mermaid
sequenceDiagram
    participant U as 사용자
    participant App as Mobile App
    participant Server as CheatKey Server
    participant DB as MySQL DB
    
    U->>App: 사용자 활동 기록 조회
    App->>Server: GET /v1/api/mypage/activities?page=1&size=20
    
    Server->>Server: JWT 토큰 검증
    Server->>DB: 사용자 활동 기록 조회 (인덱스 활용)
    Note over Server,DB: WHERE auth_id = ? ORDER BY created_at DESC
    
    DB->>Server: 활동 기록 반환
    Server->>App: 활동 기록 목록 반환
    App->>U: 활동 기록 표시
``` 