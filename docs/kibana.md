# Kibana Dashboard Sketch (PII Guard)

## Index Patterns
- `ppi-logs-*` (log events)
- `ppi-metrics-*` (metric events)

## Visualizations

### 1) PII Count Over Time
- **Data view**: `ppi-metrics-*`
- **Filter**: `name: "pii_count"`
- **X-axis**: Date histogram on `timestamp` (1m / 5m)
- **Y-axis**: Sum of `value`
- **Split series** (optional): Terms on `tags.success`

### 2) Success Rate
- **Data view**: `ppi-metrics-*`
- **Filter**: `name: "processing_ms"`
- **Metric**: Formula
  - `count(kql='tags.success: "true"') / count()`
- **Display**: Gauge (0–100%) or single number

### 3) Runtime (p50/p95)
- **Data view**: `ppi-metrics-*`
- **Filter**: `name: "processing_ms"`
- **Metric**: Percentiles of `value` (50th, 95th)
- **X-axis**: Date histogram on `timestamp`

## Alerts

### High PII Volume Alert
- **Rule type**: Threshold / ES query
- **Index**: `ppi-metrics-*`
- **Query**: `name:"pii_count"`
- **Aggregation**: Sum of `value`
- **Window**: 5 minutes
- **Condition**: sum(value) > 100 (tune for environment)
- **Actions**: Email/Slack/Teams + link to dashboard

### Low Success Rate Alert (optional)
- **Rule type**: Threshold / ES query
- **Index**: `ppi-metrics-*`
- **Query**: `name:"processing_ms" AND tags.success:"false"`
- **Aggregation**: Count
- **Window**: 5 minutes
- **Condition**: count > 0
