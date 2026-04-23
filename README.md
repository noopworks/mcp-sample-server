# MCP Server Template — Spring Boot + Spring AI

Minimal template để tạo MCP (Model Context Protocol) server trên Spring Boot ecosystem, sẵn sàng deploy trên Kubernetes.

## Naming Convention

```
mcp-{resource}-server
```

Ví dụ: `mcp-account-server`, `mcp-payment-server`, `mcp-card-server`.

Template mặc định dùng `mcp-sample-server` — thay `sample` bằng domain resource thực tế.

## Tech Stack

| Component          | Version   | Ghi chú                                       |
|--------------------|-----------|------------------------------------------------|
| Java               | 21        | ZGC Generational, Virtual Threads ready        |
| Spring Boot        | 3.4.4     | Stable GA                                      |
| Spring AI          | 1.1.4     | MCP Server Boot Starter (Streamable HTTP)      |
| Container          | Jib 3.4   | Không cần Docker daemon                        |
| K8s                | Kustomize | Base + overlays (dev/staging/prod)             |

## Quick Start

```bash
./mvnw spring-boot:run
```

| Endpoint | URL | Ghi chú |
|----------|-----|---------|
| MCP Streamable HTTP | `POST http://localhost:8080/mcp` | Default endpoint |
| Health | `GET http://localhost:8080/actuator/health` | Actuator |

## Sample Tools

Template đăng ký sẵn 2 tools mẫu trong `SampleTool.java`:

| Tool | Method | Mô tả |
|------|--------|-------|
| `lookupEntity` | `lookupEntity(entityId)` | Tra cứu entity theo ID |
| `createEntity` | `createEntity(name, tags)` | Tạo entity mới (name + optional tags) |

Cả hai trả về `SampleResponse(id, name, status, tags)` — dummy data, thay bằng logic thực tế khi dùng.

### Gọi thử MCP endpoint

MCP dùng JSON-RPC over HTTP. List tools:

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

Gọi tool `lookupEntity`:

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc":"2.0",
    "id":2,
    "method":"tools/call",
    "params":{"name":"lookupEntity","arguments":{"entityId":"abc-123"}}
  }'
```

## Project Structure

```
src/main/java/com/sample/mcp/server/
├── McpServerApplication.java          # Entry point
├── tool/
│   └── SampleTool.java                # @Tool annotated tools
├── service/
│   └── SampleService.java             # Business logic
├── model/                             # DTOs, domain models
└── exception/
    └── GlobalExceptionHandler.java    # ProblemDetail error responses
```

## Tạo MCP Server mới từ template

### Package convention

```
{group}.mcp.{resource}.server
```

- `{group}` — reverse domain của tổ chức (ví dụ: `io.acme`, `vn.mycompany`, `com.example`)
- `{resource}` — domain resource của server (ví dụ: `account`, `transaction`, `payment`)

| Ví dụ | Package |
|-------|---------|
| Acme Corp — account server | `io.acme.mcp.account.server` |
| Acme Corp — order server | `io.acme.mcp.order.server` |
| Example Inc — payment server | `com.example.mcp.payment.server` |

### Các bước

```bash
# 1. Copy template
cp -r mcp-sample-server mcp-account-server
cd mcp-account-server

# 2. Rename (sed hoặc IDE refactor)
#    - pom.xml: groupId → {group}, artifactId → mcp-{resource}-server
#    - Package: com.sample.mcp.server → {group}.mcp.{resource}.server
#    - application.yml: spring.application.name → mcp-{resource}-server
#    - K8s manifests: mcp-sample-server → mcp-{resource}-server

# 3. Thay SampleTool bằng domain tool
```

## Tạo MCP Tool mới

```java
@Service
public class AccountTool {

    @Tool(description = "Get account balance by account number")
    public AccountBalance getBalance(
            @ToolParam(description = "Account number (10 digits)", required = true)
            String accountNumber
    ) {
        // gọi downstream API / DB
    }
}
```

Mỗi method `@Tool` tự động:
- Đăng ký thành 1 tool trong MCP server
- Generate JSON schema cho input parameters
- Serialize return type thành structured content

## Build & Deploy

```bash
# Build container image (không cần Docker daemon)
./mvnw compile jib:build -Ddocker.registry=your-registry.com

# Hoặc dùng Dockerfile
docker build -t mcp-account-server:latest .

# Deploy K8s
kubectl apply -k k8s/overlays/dev/      # Dev
kubectl apply -k k8s/overlays/staging/   # Staging
kubectl apply -k k8s/overlays/prod/      # Production
```

## K8s Features

- **HPA**: Auto-scale 2→10 pods (CPU/Memory based)
- **PDB**: `minAvailable: 1` — zero-downtime rolling update
- **NetworkPolicy**: Restrict ingress (chỉ ingress-nginx + MCP clients)
- **Probes**: Liveness + Readiness + Startup
- **Graceful shutdown**: `preStop` sleep 5s → drain connections
- **Security context**: `runAsNonRoot`, non-privileged

## Configuration Reference

| Property                                    | Default      | Mô tả                          |
|---------------------------------------------|--------------|---------------------------------|
| `spring.ai.mcp.server.protocol`             | STREAMABLE   | Transport protocol              |
| `spring.ai.mcp.server.type`                 | SYNC         | SYNC hoặc ASYNC                 |
| `spring.ai.mcp.server.capabilities.tool`    | true         | Bật/tắt tool capability         |

## Production Checklist

Các tích hợp dưới đây đã được loại bỏ để giữ template đơn giản. Thêm lại khi chuẩn bị deploy production:

### Security (OAuth2 Resource Server)

- [ ] Thêm dependencies: `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`
- [ ] Tạo `SecurityConfig.java` — bảo vệ `/mcp/**` bằng JWT, cho phép actuator public
- [ ] Cấu hình `OAUTH2_ISSUER_URI` trong `application.yml`
- [ ] Profile `dev`: tắt security cho local development
- [ ] K8s: thêm ConfigMap cho `oauth2-issuer-uri`, Secret cho credentials
- [ ] K8s NetworkPolicy: thêm egress rule cho OAuth2 issuer (port 443)

### Observability (Micrometer + Prometheus + OpenTelemetry)

- [ ] Thêm dependencies: `micrometer-registry-prometheus`, `micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`
- [ ] Cấu hình management port riêng (8081) — không expose qua ingress
- [ ] Expose endpoints: `health,info,prometheus,metrics`
- [ ] Cấu hình tracing sampling: `TRACING_SAMPLE_RATE` (0.1 dev, 0.05 prod)
- [ ] Log pattern thêm traceId/spanId: `[%X{traceId:-}/%X{spanId:-}]`
- [ ] K8s Deployment: thêm prometheus annotations (`prometheus.io/scrape`, `/port`, `/path`)
- [ ] K8s NetworkPolicy: thêm ingress rule cho monitoring namespace (port 8081)

### Resilience (Resilience4j)

- [ ] Thêm dependency: `resilience4j-spring-boot3` (2.2.0)
- [ ] Cấu hình Circuit Breaker: `sliding-window-size`, `failure-rate-threshold`, `wait-duration-in-open-state`
- [ ] Cấu hình Retry: `max-attempts`, `wait-duration`, `exponential-backoff-multiplier`
- [ ] Cấu hình Rate Limiter: `limit-for-period`, `limit-refresh-period`
- [ ] Thêm `@CircuitBreaker`, `@Retry` annotations trên tool methods gọi downstream services

### Banking-specific

- [ ] Audit logging: AOP aspect ghi lại mọi tool invocation (who, what, when)
- [ ] mTLS: cấu hình ở ingress layer hoặc service mesh (Istio/Linkerd)
- [ ] Secret management: External Secrets Operator hoặc Vault CSI driver
