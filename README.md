# CheatKey 프로젝트

CheatKey는 벡터 검색 기반 AI 모델을 활용해 피싱 사기 문자를 실시간으로 분석하는 보안 서비스입니다.

현재는 피싱 탐지 기능을 중심으로 운영 중이며,LangChain을 기반으로 한 CS 자동화 기능은 AI 분석 오류 등 핵심 문의 유형을 바탕으로 추후 확장 적용을 검토하고 있습니다.

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.1-brightgreen)
![FastAPI](https://img.shields.io/badge/FastAPI-0.116.0-green)
![Qdrant](https://img.shields.io/badge/VectorDB-Qdrant-red)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

---

## 서비스 디자인

![img.png](src/main/resources/static/img.png)

<!--@TODO 실제 경로 추가하기-->
- **Android**: [Google Play Store](링크)
- **iOS**: [App Store](링크)

---

## 기술 스택

- **Java 21**, **Spring Boot 3**
- **Python 3**, **FastAPI**
- **MySQL (Amazon RDS)**, **Qdrant (Vector DB)**
- **AWS EC2 / S3 / Route 53 / CloudFront / CodeDeploy**
- **Nginx**, **Docker**
- **KoSimCSE (Embedding)**, **OpenAI GPT API**
- **GA4, Looker Studio, BigQuery** (마케팅 분석 예정)

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

### ✅ 인증 시스템
- Kakao, Apple Native SDK 로그인
- JWT 기반 사용자 인증 및 토큰 관리

### ✅ 커뮤니티 기능
- 게시글 작성, 조회, 신고 및 상태 관리
- 댓글, 좋아요, 작성자 히스토리 등 지원

### ✅ AI 기반 피싱 분석
- KoSimCSE 임베딩 → Qdrant 검색 → 유사도 기반 위협 감지
- Google Safe Browsing 연동

### ✅ CS 자동화 (LangChain 기반)
- Qdrant + GPT API 활용한 자동 답변 시스템
- 수기 문의 분석 및 자동 응답

---

## 배포 방식

- GitHub Actions + AWS CodeDeploy를 통한 무중단 배포
- Nginx Reverse Proxy를 통한 프론트/백 연결
- Route 53을 활용한 DNS 관리

---

## 테스트

- 각 모듈별 통합 테스트 포함
- 인증, 분석, 커뮤니티, 자동응답 API에 대한 시나리오 기반 테스트

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

- [CheatKey 시스템 구축기](https://your-blog-link.com) (작성 중)