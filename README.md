# CheatKey 프로젝트

CheatKey는 **벡터 검색 기반 AI 모델**을 활용해 피싱 사기 문자를 실시간으로 분석하는 보안 서비스입니다.

현재는 **피싱 탐지 기능**을 중심으로 운영 중이며, LangGraph을 가반으로 한 **CS 자동화 기능**은 AI 분석 오류 등 핵심 문의 유형을 바탕으로 추후 확장 적용을 검토하고 있습니다.

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.1-brightgreen)
![FastAPI](https://img.shields.io/badge/FastAPI-0.116.0-green)
![Qdrant](https://img.shields.io/badge/VectorDB-Qdrant-red)
![AWS](https://img.shields.io/badge/AWS-Cloud%20Infrastructure-orange)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

---

## 서비스 디자인

![img.png](src/main/resources/static/img.png)

- **Android**: [Google Play Store](https://play.google.com/store/apps/details?id=com.cheatkey.app&pcampaignid=web_share)
- **iOS**: [App Store](https://apps.apple.com/kr/app/%EC%B9%98%ED%8A%B8%ED%82%A4-ai-%EC%82%AC%EA%B8%B0%ED%83%90%EC%A7%80-%ED%94%8C%EB%9E%AB%ED%8F%BC/id6749635626)

---

## 기술 스택

### Backend
- **Java 21**, **Spring Boot 3.3.1** (메인 서버)
- **Python 3**, **FastAPI** (AI 임베딩 서버)
- **MySQL** (Amazon RDS), **Qdrant (Vector DB)** (Vector DB)

### AI/ML
- **KoSimCSE (Embedding)**, **OpenAI API**

### Infrastructure
- **Nginx**, **Docker**
- **AWS EC2 / S3 / Route 53 / CloudFront / CodeDeploy**

---

## 프로젝트 구조

본 프로젝트는 **DDD 레이어드 아키텍처**를 기반으로 설계되었습니다.

```
cheatkey/
├── src/main/java/com/cheatkey/
│ ├── common/                   # 공통 모듈 (보안, 예외처리, 설정)
│ ├── module/       
│ │ ├── auth/                   # 인증 도메인 (소셜 로그인, JWT)
│ │ ├── community/              # 커뮤니티 도메인
│ │ ├── detection/              # 피싱 탐지 도메인
│ │ ├── file/                   # 파일 업로드 도메인
│ │ ├── home/                   # 홈 대시보드 도메인
│ │ ├── mypage/                 # 마이페이지 도메인
│ │ └── terms/                  # 약관 관리 도메인
│ └── CheatkeyApplication.java
├── embedding-server/           # AI 임베딩 서버 (FastAPI)
└── docs/                       # 프로젝트 문서
```

---

## 주요 기능

### ✅ AI 기반 피싱 탐지
- **5단계 지능형 워크플로우**:
    - 입력검증 → KoSimCSE 임베딩 → Qdrant 벡터검색 → AI검증 → 품질평가 → 결과분석
- **3단계 위험도 판정**: DANGER(0.7+), WARNING(0.3~0.7), SAFE(0.3 미만)
- **Google Safe Browsing 연동**

### ✅ 인증 시스템
- Kakao, Apple Native SDK 로그인
- JWT 기반 사용자 인증 및 토큰 관리

### ✅ 커뮤니티 기능
- 게시글 작성부터 조회, 댓글/대댓글, 신고/차단까지 완전한 커뮤니티 시스템
- S3 연동 다중 파일 첨부 및 게시글 상태 관리

### ✅ CS 자동화 (LangGraph 기반 - 고도화 진행 중)
- Qdrant + OpenAI API 활용한 자동 답변 시스템
- 수기 문의 분석 및 자동 응답

---

## 배포 방식

- GitHub Actions + AWS CodeDeploy를 통한 무중단 배포
- Nginx Reverse Proxy를 통한 프론트/백 연결
- 가비아 DNS + AWS Route 53을 활용한 도메인 및 라우팅 관리

---

## 테스트

- 각 모듈별 통합 테스트 포함
- 인증, 분석, 커뮤니티 API에 대한 시나리오 기반 테스트

---

## 문서화

- **[시스템 아키텍처](architecture.md)**: VPC, EC2, RDS 포함 전체 구성
- **[인증 흐름](docs/flows/authentication.md)**: Native SDK + JWT 구조
- **[ERD](docs/models/erd.md)**: 전체 도메인 설계
- **[시퀀스 다이어그램](docs/flows/sequence.md)**: 주요 기능 동작 흐름
- **[비즈니스 플로우 차트](docs/flows/business.md)**: 비즈니스 로직 의사결정 플로우

> 위 문서들은 `/docs` 디렉터리에 포함되어 있으며, 상세 기능별 이해를 돕기 위해 별도 작성되었습니다.

---

## 라이선스

본 프로젝트는 MIT 라이선스를 따릅니다.

---

## 기술 블로그

- CheatKey 시스템 구축기 (작성 중)