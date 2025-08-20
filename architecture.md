# CheatKey 시스템 아키텍처

## 1. 전체 시스템 아키텍처 개요

### 1.1 시스템 아키텍처 다이어그램

![system-architecture-diagram.png](docs/diagrams/system-architecture-diagram.png)
> Lucidchart로 작성된 AWS 클라우드 아키텍처 다이어그램.  
> VPC, EC2 인스턴스, RDS, CI/CD 파이프라인을 포함한 전체 시스템 구조를 시각적으로 보여줍니다.

---

### 1.2 주요 구성 요소

#### 클라이언트 접근
- **사용자**: Android/iOS 앱을 통한 접근
- **DNS & CDN**: 가비아 DNS + Amazon Route 53 + CloudFront
- **정적 리소스**: Amazon S3

#### AWS 클라우드 인프라
- **VPC**: `10.0.0.0/16`
- **가용영역 (AZ)**: AZ A, AZ B
- **인터넷 게이트웨이**: 외부 트래픽 진입점

#### 네트워크 구성
- **Public Subnet 1 (AZ A)**: EC2: WAS 서버 (Nginx + Spring Boot)
- **Private Subnet 1 (AZ A)**: EC2: Vector 서버 (Phishing AI)
- **Private Subnet 2 (AZ B)**: Amazon RDS (MySQL)
- **Private Subnet 3 (AZ B)**: EC2: LangGraph Agent 서버 (CS Bot) - 향후 구현 예정

---

## 2. 서버 구성 상세

### 2.1 EC2: WAS 서버 (Public Subnet 1)
- **역할**: Nginx Reverse Proxy + Spring Boot
- **포트**: 80, 443
- **기능**:
  - 클라이언트 요청 처리
  - API 라우팅
  - AWS CodeDeploy Agent 설치
- **통신**:
  - Vector 서버 (Port 6333)
  - RDS (Port 3306)

---

### 2.2 EC2: Vector 서버 (Private Subnet 1)
- **역할**: 피싱 사례 AI 분석
- **구성**:
  - Docker 컨테이너 환경
    - Qdrant DB (for Phishing Analysis)
    - Embedding Server (KoSimCSE)
- **비고**:
  - RDS와 직접 통신하지 않음
  - 현재 운영 중인 핵심 AI 분석 서버

---

### 2.3 Amazon RDS (Private Subnet 2)
- **역할**: 관계형 데이터베이스 (MySQL)
- **접근**: WAS 서버에서 직접 접근
- **포트**: 3306
- **구성**: Single-AZ (현재 부하로 충분)

---

### 2.4 EC2: LangGraph Agent 서버 (Private Subnet 3) - 향후 구현
- **역할**: CS Bot 처리 서버
- **포트**: 8080 (내부 호출 전용)
- **구성**:
  - Qdrant DB (for LangGraph)
  - GPT API 연동
- **통신**:
  - RDS (Port 3306)
  - 외부 GPT API (HTTPS)
- **상태**: 현재 미구현, CS 자동화 고도화 시 적용 예정

---

## 3. 외부 API 통합

### 3.1 OpenAI API (HTTPS)
- **요청 주체**: WAS 서버 (Spring Boot)
- **용도**: AI 검증 및 품질 평가 (Graph-based state machine 3단계)
- **통신 방식**: Outbound HTTPS via IGW

---

## 4. CI/CD 파이프라인

### 4.1 배포 프로세스
- **개발자**: 코드 수정 및 GitHub 푸시
- **GitHub**: 소스 코드 저장소
- **GitHub Actions**: 자동 빌드 및 테스트
- **AWS CodeDeploy**: 배포 서비스
- **EC2 WAS 서버**: CodeDeploy Agent 설치됨

### 4.2 배포 아티팩트
- **Amazon S3**: CodeDeploy 배포 파일 저장소

---

## 5. 보안 구성

### 5.1 네트워크 보안
- **VPC**: 프라이빗 네트워크
- **Security Groups**: 포트별 접근 제어
- **Private Subnets**: AI 서버 및 DB 보호

### 5.2 애플리케이션 보안
- **JWT 토큰**: API 기반 사용자 인증
- **Native SDK**: Kakao, Apple 소셜 로그인
- **HTTPS**: 모든 외부 통신 암호화

---

## 6. 데이터 플로우

### 6.1 일반 요청 흐름
- Client → 가비아 DNS → Route 53 → CloudFront → S3
  - 정적 콘텐츠: Client → Route 53/CloudFront → S3
  - 동적 API: Client → Route 53 (또는 도메인) → EC2 WAS (HTTPS)
- WAS Server → Vector Server (Port 6333) - AI 분석
- WAS Server → RDS (Port 3306) - 데이터 저장/조회

### 6.2 AI 분석 워크플로우 (현재 운영 중)
- 사용자 입력 → WAS Server → Vector Server
- Vector Server → KoSimCSE 임베딩 → Qdrant 벡터검색
- **Graph-based state machine (LangGraph-inspired)**: 5단계 상태 기반 분석 파이프라인
  - 입력검증 → 벡터검색 → AI검증 → 품질평가 → 결과분석
- 필요 시 OpenAI API 호출 (품질 평가) - WAS 서버에서 직접 호출
- 결과 분석 및 위험도 판정

### 6.3 CS Bot 요청 흐름 (향후 구현)
- Client → IGW → LangGraph Agent Server (Port 8080)
- LangGraph Agent → RDS (Port 3306)
- LangGraph Agent → Qdrant (Local)
- LangGraph Agent → OpenAI API (HTTPS)

---

## 7. 확장성 및 고가용성

### 7.1 현재 구현된 구성
- **AZ A**: WAS 서버, Vector 서버
- **AZ B**: RDS (MySQL)
- **RDS**: Single-AZ 설정 (현재 부하로 충분)
- **다중 AZ**: 기본적인 가용성 보장

### 7.2 향후 확장 계획 (필요 시)
- **Auto Scaling Groups**: 트래픽 증가 시 자동 확장
- **RDS Read Replicas**: 읽기 부하 분산 및 가용성 향상
- **Application Load Balancer (ALB)**: 다중 서버 환경 대응
- **Multi-AZ RDS**: 고가용성 강화

---

## 8. 모니터링 및 운영

### 8.1 현재 모니터링 (자체 로그 기반)
- **Spring Boot 내장 로깅**: 애플리케이션 로그
- **파일 기반 로그 관리**: 로그 파일 저장 및 관리
- **기본 모니터링**: 서버 상태 및 애플리케이션 동작 확인

### 8.2 향후 확장 계획 (CloudWatch 연동)
- **CloudWatch Logs**: Spring Boot 로그와 연동 가능
- **CloudWatch Metrics**: 시스템 지표 수집
- **CloudWatch Alarms**: 이상 징후 알림
- **확장성**: Spring Boot Actuator 기반으로 CloudWatch 연동 준비됨

### 8.3 운영 도구 (향후 CS 자동화 시 적용 예정)
- LangGraph, MCP, Jira : CS 자동화 워크플로우 엔진 (구현 예정)

---

## 9. 기술 스택

### 9.1 백엔드
- **Spring Boot 3.3.1**: 메인 애플리케이션 서버
- **FastAPI**: AI 임베딩 서버 (KoSimCSE)
- **Nginx**: Reverse Proxy

### 9.2 데이터베이스
- **MySQL 8.0**: Amazon RDS (메인 운영 DB)
- **Qdrant**: Vector DB (피싱 사례 임베딩)

### 9.3 AI/ML
- **KoSimCSE**: 한국어 특화 문장 임베딩 모델
- **OpenAI GPT API**: AI 검증 및 품질 평가
- **Graph-based state machine**: LangGraph 영감을 받은 상태 기반 워크플로우 (현재 AI 분석에 적용)

### 9.4 인프라
- **AWS**: EC2, RDS, S3, CloudFront, Route 53
- **Docker**: 컨테이너화 환경
- **CI/CD**: GitHub Actions + AWS CodeDeploy

---

## 📌 요약

이 아키텍처는 **현재 운영 중인 피싱 탐지 AI 분석 시스템**을 기반으로 하며,  
**향후 CS 자동화 기능 확장**을 고려한 확장 가능한 AWS 기반 클라우드 시스템입니다.

### 현재 상태
- ✅ **AI 기반 피싱 탐지**: Graph-based state machine (LangGraph-inspired) 5단계 워크플로우로 운영 중
- ✅ **기본 인프라**: VPC, EC2, RDS, S3 구성 완료
- ✅ **CI/CD 파이프라인**: GitHub Actions + CodeDeploy 구축

### 향후 계획
- 🔄 **CS 자동화**: LangGraph 기반 고도화 진행 중
- 🔄 **모니터링 강화**: CloudWatch 연동 검토 중
- 🔄 **확장성 개선**: Auto Scaling, Read Replicas 등 필요 시 적용
