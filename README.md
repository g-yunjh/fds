본 명세서는 바이낸스(Binance) 실시간 시세 데이터를 활용하여 0.1초 내에 이상 거래를 탐지하는 하이브리드 고성능 플랫폼의 기술 규격 및 구조를 정의함.

---

## 1. 아키텍처 및 파이프라인 구조도

데이터의 실시간 흐름과 홈 서버 및 클라우드 간의 인프라 배치 구조임.

```text
[ 데이터 소스: Binance WebSocket ]
        │
        ▼ (Real-time Stream)
┌────────────────────────────────────────────────────────────────────────────┐
│                          홈 서버 (Samsung Laptop / Ubuntu)                 │
│    "데이터 처리 엔진 및 로컬 저장소 (Core Engine & Storage Layer)"               │
│                                                                            │
│   ┌──────────────────────┐          ┌────────────────────────────────────┐  │
│   │  Collector Service   │          │      Analyzer (SCS / Java 25)      │  │
│   │ (Virtual Threads)    │          │ - 실시간 Z-Score 기반 이상 탐지      │  │
│   └──────────┬───────────┘          └──────────────────┬─────────────────┘  │
│              │ (Kafka Produce)                         │                    │
│              ▼                                         ▼                    │
│   ┌──────────────────────┐          ┌────────────────────────────────────┐  │
│   │    Apache Kafka      │◀─────────┤       Redis (Stats Cache)          │  │
│   │  (raw-price topic)   │          │ (실시간 탐지용 통계 지표 저장)       │  │
│   └──────────┬───────────┘          └──────────────────▲─────────────────┘  │
│              │                                         │                    │
│              ▼ (L)                  ┌──────────────────┴─────────────────┐  │
│   ┌──────────────────────┐ (E)      │        Apache Spark (Batch)        │  │
│   │    MinIO (Data Lake) ├──────────▶ - 대규모 시계열 정제 및 ETL          │  │
│   │ (Bronze/Silver Layer)│          │ - 주기적인 통계 모델(μ, σ) 업데이트  │  │
│   └──────────┬───────────┘          └────────────────────────────────────┘  │
│              │                                                              │
│              ▼ (Insert)             ┌────────────────────────────────────┐  │
│   ┌──────────────────────┐ (Fetch)  │       Spring Batch (Operator)      │  │
│   │      TimescaleDB     │◀─────────┤ - 일일 결산 및 지표 요약              │  │
│   │ (Detailed Logs/Gold) │          │ - Supabase로 최종 데이터 전송 (ETL)   │  │
│   └──────────────────────┘          └──────────────────┬─────────────────┘  │
└─────────────────────┬─────────────────────────────────┼────────────────────┘
                      │                                 │
       [ Tailscale VPN (Metric Pull) ]                  │ (Business Data Push)
                      │                                 ▼
┌─────────────────────┴────────────────┐    ┌──────────────────────────────┐
│        OCI Instance (1GB RAM)        │    │      Supabase (Free Tier)    │
│    "통합 관제 센터 (Control Tower)"    │    │    "최종 데이터 (Gold Tier)"   │
│                                      │    │                              │
│   ┌────────────────────────────────┐ │    │  ┌────────────────────────┐  │
│   │        Grafana (Server)        │ │    │  │   Managed PostgreSQL   │  │
│   │--------------------------------│ │    │  │ (일일 통계/최종 리포트)   │  │
│   │ 📁 [System-Monitor]            │◀─┼────┼─▶│ - Grafana Business Source│  │
│   │ 📁 [Business-Analysis]         │ │    │  └────────────────────────┘  │
│   └────────────────▲───────────────┘ │    └──────────────────────────────┘
│                    │                 │
│   ┌────────────────┴───────────────┐ │
│   │            Prometheus          │ │
│   │ (Scraping Home Server Metrics) │ │
│   └────────────────────────────────┘ │
└──────────────────────────────────────┘
```

---

## 2. 데이터 파이프라인 상세 단계

* **Ingestion (수집)**: Java 25 가상 스레드 및 리액티브 스택 활용, Binance API 데이터 비동기 수집 및 Kafka 전송.
* **Speed Layer (실시간 탐지)**: Kafka 데이터 소비, Redis 통계 지표 대조, 0.1초 내 이상 거래 판단 및 분석 결과 TimescaleDB 적재.
* **Data Lake (저장)**: 모든 원천 데이터의 MinIO 저장 및 향후 재분석용 데이터 확보.
* **Batch Layer (분석)**: Apache Spark 기반 MinIO 전수 조사, 정밀 통계 모델($\mu, \sigma$) 생성 및 Redis 캐시 갱신.
* **Serving Layer (전시)**: Spring Batch 기반 데이터 요약 및 클라우드(Supabase) 최종 데이터 전송.
* **Observability (관제)**: OCI Grafana 인스턴스 기반 시스템 상태 및 비즈니스 지표 실시간 시각화.

---

## 3. 기술 스택 요약

* **Runtime**: Java 25 (LTS) 기반 툴체인 적용.
* **Backend**: Spring Boot 4.0.2, Spring Cloud Stream, Spring Batch 구성.
* **Infrastructure**: Apache Kafka (KRaft), Redis, TimescaleDB, MinIO 컨테이너 환경.
* **Cloud & Network**: OCI (관제), Supabase (데이터 서빙), Tailscale (보안 터널링) 활용.

---

## 4. 현재 구현 현황 및 진행 상황

* **멀티 모듈 구조**: 루트 `fds-platform` 및 하위 `fds-core` 모듈 체계 구축 완료.
* **실시간 수집기**: Reactor Netty 기반 `BinanceWebSocketCollector` 및 자동 재연결 로직 구현 완료.
* **메시지 중계**: 수집 데이터의 Kafka `binance-trades` 토픽 전송 로직 완료.
* **인프라 자동화**: Docker Compose 기반 Kafka, Redis, TimescaleDB, MinIO 통합 환경 설정 완료.

---

## 5. 핵심 가치 및 알고리즘

* **Z-Score 탐지**: 실시간 가격($x$)과 Redis 캐시 통계값($\mu, \sigma$) 대조를 통한 이상치 판단 ($|Z| > 3$ 시 트리거).
* **하이브리드 전략**: 대량 연산의 온프레미스 집중 및 관제의 클라우드 분산을 통한 효율 극대화.
* **확장성**: 람다 아키텍처 적용으로 실시간 탐지와 정밀 분석의 상호 보완 구조 확보.