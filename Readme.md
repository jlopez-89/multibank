System Characteristics

1. Performance
   High-frequency event handling
   Kafka absorbs large message volumes with low latency and guarantees per-symbol ordering, allowing producers and consumers to scale independently without losing events.

Low-latency candle generation & history queries
-	Efficient database indexing
-	In-memory caching (Caffeine for single instance, Redis for distributed)
This ensures fast access to both real-time and historical candle data.

2. Scalability
   Multiple symbols + intervals without blocking
   Optimistic locking enables safe concurrent updates to the same candle without serializing all operations.

Time-aligned candle generation
Each event is mapped to its correct time bucket using the event timestamp—even late/out-of-order events update the proper candle.

3. Reliability
   Thread-safe aggregation and storage
   Concurrent maps + DB locking ensure correctness under high load.

    Safe startup, shutdown & replay : Manual ACKs ensure offsets are committed only after persistence succeeds.
    For replay or backfill, configure Kafka with: auto-offset-reset=earliest.

4. Maintainability
   -	Clean, modular service boundaries
   -	Clear separation of concerns: ingestion (Kafka), aggregation (time buckets), persistence, history queries
   -	Comprehensive unit + integration tests validating time-bucket behavior and Kafka ingestion.

5. Observability
   -	Structured logs for bid/ask events and candle lifecycle
   -	Spring Actuator health endpoints (DB, Kafka, disk, etc.)
   -	Metrics suitable for dashboards (latency, candle creation rate, consumer lag)

6. Extensibility
   -	Add new symbols or timeframes through configuration
   -	Candle logic is decoupled from ingestion, enabling support for Kafka, schedulers, replay jobs, WebSockets, etc.

 - To execute the test go to the root directory and execute 
   - mvn test
 - To run to go to main/resources and execute
   - docker-compose up
 - To see if the service is working we can execute :
   - http://localhost:8080/swagger-ui/index.html#/Candles/getHistory
   - curl "http://localhost:8080/api/v1/candles/history?symbol=BTC-USD&interval=1m&from=$(($(date +%s)-300))&to=$(date +%s)" | jq
     - $(date +%s) calculate the current timestamp in seconds
     - $(date +%s)-300 calculate the current timestamp in seconds minus 5 seconds


Test Strategy Overview - 95% coverage

-	Unit Tests
  - Aggregation logic (CandleAggregationOperation): bucket calculation, OHLC updates, volume increments, out-of-order events, multiple timeframes.
  - Read logic (GetHistoryOperation): range filtering, ordering, and validation (from < to).
-	Repository / Persistence Tests
  - JPA mappings and queries by symbol/timeframe/range, including optimistic locking behavior.
-	Integration Tests
  - CandleAggregationOperation working together with the database.
  - /history controller returning correct JSON and proper validation errors.
-	Kafka Integration Tests
  - Full ingestion pipeline: Producer → Topic → Listener → Aggregation → Database (using Testcontainers + Awaitility).


