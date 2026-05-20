# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## 프로젝트 개요

한국(KOSPI/KOSDAQ) + 미국(NASDAQ) 모의 주식 거래 웹앱. 사용자는 KRW 시드머니를 받고 USD로 환전하여 미국 주식을 거래할 수 있다. 실시간 시세는 Finnhub WebSocket(미국) / Yahoo Finance polling(한국) → Kafka → STOMP/WebSocket → Vue 프론트로 흐른다.

---

## 개발 워크플로우 하네스

### 작업 시작 전 체크리스트 (반드시 확인)

1. **인프라 확인** — 백엔드보다 먼저 실행되어야 함
   ```bash
   docker-compose ps   # oracle, kafka, zookeeper, redis 모두 Up 상태인지 확인
   ```
   Down 상태라면:
   ```bash
   docker-compose up -d
   # Oracle은 최초 기동 시 ~2분 소요. healthy 상태 확인 후 백엔드 실행
   ```

2. **백엔드 컴파일 가능 상태 확인**
   ```bash
   cd backend && ./gradlew compileJava
   ```

3. **프론트엔드 의존성 확인**
   ```bash
   cd frontend && npm install
   ```

---

### 백엔드 변경 시 워크플로우

백엔드 Java 파일을 수정할 때마다 아래 순서를 따른다:

```
1. 코드 수정
2. cd backend && ./gradlew compileJava   ← 컴파일 에러 즉시 확인
3. cd backend && ./gradlew test          ← 전체 테스트
4. 필요 시: cd backend && ./gradlew bootRun  ← 실행 확인
```

단일 테스트 실행:

```bash
cd backend && ./gradlew test --tests "com.chartanalysis.패키지.클래스명"
```

**백엔드 변경 후 필수 점검 항목:**
- [ ] 새 엔드포인트 추가 시 → `SecurityConfig`의 permitAll/authenticated 규칙 확인
- [ ] 새 도메인 추가 시 → `DataInitializer`에 초기 데이터 필요 여부 확인
- [ ] Kafka 메시지 구조 변경 시 → `StockPriceMessage` DTO와 Consumer/Producer 양쪽 동시 수정
- [ ] DB 엔티티 변경 시 → `ddl-auto: create-drop`이므로 재시작하면 **전체 데이터 소멸** 주의

---

### 프론트엔드 변경 시 워크플로우

```
1. cd frontend && npm run dev   ← 개발 서버 실행 (포트 5173)
2. 코드 수정
3. cd frontend && npm run build ← 빌드 에러 확인
```

**프론트엔드 변경 후 필수 점검 항목:**
- [ ] 새 API 호출 추가 시 → `src/utils/api.js`의 Axios 인터셉터(JWT 자동 첨부) 사용 여부 확인
- [ ] WebSocket 구독 변경 시 → `stock` store의 `connectWebSocket()` → `subscribeToAllStocks()` 순서 유지
- [ ] 새 페이지/라우트 추가 시 → `src/router/index.js`에 route 등록 및 인증 가드 설정 확인
- [ ] 새 Pinia store 추가 시 → `main.js`에 등록 여부 확인

---

### 기능 추가 시 전체 워크플로우

새 기능(예: 새 종목 추가, 새 거래 유형 등)은 반드시 아래 순서로 진행:

```
백엔드 도메인 모델 → Repository → Service → Controller
         ↓
  DataInitializer 초기 데이터
         ↓
  프론트엔드 API 연동 (api.js or store)
         ↓
  Vue 컴포넌트/뷰
         ↓
  전체 빌드 + 실행 확인
```

---

## 도메인 규칙 (반드시 준수)

### 통화 구분
- **NASDAQ 종목** → `portfolio.availableUsd` (USD 지갑) 사용
- **KOSPI / KOSDAQ 종목** → `portfolio.availableCash` (KRW 지갑) 사용
- `TradeService.isUsdMarket(stock)`으로 판별 (`stock.market == Market.NASDAQ`)
- KRW ↔ USD 환전은 반드시 `ExchangeService.exchangeCurrency()`를 통해서만

### 종목 심볼 규칙
- DB에는 `.KS`/`.KQ` 접미사 없이 저장 (예: `005930`)
- Yahoo Finance API 호출 시에는 접미사 포함 (예: `005930.KS`)
- `FinnhubWebSocketClient`가 구독하는 미국 종목은 `application.yml`의 `finnhub.us-symbols`에서 관리

### 실시간 데이터 파이프라인
- 실시간 가격은 반드시 Kafka 파이프라인을 통해 흘러야 함
- `FinnhubWebSocketClient`는 3초마다 최신가를 Kafka로 flush (매 tick이 아님)
- `StockDataConsumer`가 Kafka에서 소비 → Oracle 업데이트 + STOMP broadcast + 캔들 저장

### 인증/보안
- 공개 엔드포인트: `/api/auth/**`, `GET /api/stocks/**`, `/ws/**`, `/api/exchange/rate`
- 나머지 전체 → JWT 필수 (`Authorization: Bearer <token>`)
- 프론트엔드 `api.js` Axios 인스턴스가 localStorage JWT를 자동 첨부

---

## 아키텍처 참조

### 실시간 가격 흐름

```
Finnhub WSS ──► FinnhubWebSocketClient (3초 flush)
                         │
Yahoo Finance ──► YahooFinanceScheduler (polling)
                         │
               StockDataProducer ──► Kafka "stock-prices"
                         │
               StockDataConsumer ──► Oracle(Stock 현재가 갱신)
                                 ──► Oracle(StockPrice 1m 캔들 저장)
                                 ──► STOMP /topic/stocks/{symbol}
                         │
               Vue stock store ──► CandlestickChart 실시간 업데이트
```

### 환율 흐름

```
exchangerate-api.com ──► ExchangeRateScheduler ──► Redis "exchange:USD:KRW"
                                                        │
                                          ExchangeService.getCurrentRate()
                                          (Redis miss 시 API 직접 호출, 둘 다 실패 시 1380.0 fallback)
```

### 포트폴리오 구조
- `Portfolio` — 유저당 1개, `availableCash`(KRW) + `availableUsd`(USD)
- `Holding` — 보유 종목별, 가중평균 단가(`avgPrice`) + 수량
- `Trade` — 거래 내역 (BUY/SELL 로그)
- 월 1일 00:00 → 전체 포트폴리오에 1,000,000 KRW 자동 지급

### 핵심 설정값 (application.yml)
| 항목 | 값 |
|------|-----|
| 백엔드 포트 | 8080 |
| 프론트엔드 포트 | 5173 |
| DB | Oracle `FREEPDB1` / chartuser:chartpass |
| JWT 만료 | 24시간 |
| ddl-auto | `create-drop` (재시작 시 데이터 전체 초기화) |
| 시드머니 | 1,000,000 KRW / 월 |

---

## 명령어 빠른 참조

```bash
# 인프라
docker-compose up -d
docker-compose ps
docker-compose down

# 백엔드
cd backend && ./gradlew bootRun
cd backend && ./gradlew build
cd backend && ./gradlew test
cd backend && ./gradlew test --tests "com.chartanalysis.SomeTest"
cd backend && ./gradlew compileJava   # 컴파일만

# 프론트엔드
cd frontend && npm run dev
cd frontend && npm run build
cd frontend && npm run preview
```
