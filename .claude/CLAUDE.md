# MCP Sample Server

MCP Server template sử dụng Spring Boot 3.4.4 + Spring AI 1.1.4. Đây là repo template — dùng chung cho nhiều tổ chức, không chứa business logic cụ thể.

## Quick Reference

```bash
mvn spring-boot:run             # Start local (port 8080)
mvn verify -B                   # Build + test
mvn compile jib:build -Ddocker.registry=xxx  # Container image (không cần Docker daemon)
```

| Endpoint | URL |
|----------|-----|
| MCP (Streamable HTTP) | `POST /mcp` |
| Health | `GET /actuator/health` |
| Liveness | `GET /actuator/health/liveness` |
| Readiness | `GET /actuator/health/readiness` |

## Tech Stack

- Java 21, Spring Boot 3.4.4, Spring AI 1.1.4
- MCP protocol: Streamable HTTP (SYNC), annotation-scanner auto-detect `@Tool` beans
- Build: Maven, Jib (container), Kustomize (K8s)
- CI: GitHub Actions (`.github/workflows/ci.yml`)

## Project Structure

```
src/main/java/com/sample/mcp/server/
├── McpServerApplication.java          # Entry point
├── tool/                              # @Tool annotated — 1 file = 1 domain
│   └── SampleTool.java
├── service/                           # Business logic
│   └── SampleService.java
└── exception/
    └── GlobalExceptionHandler.java    # ProblemDetail (RFC 7807)

k8s/
├── base/                              # Deployment, Service, HPA+PDB, NetworkPolicy
└── overlays/{dev,staging,prod}/       # Kustomize patches (replicas, resources, image)
```

## Conventions

### MCP Tool definition

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class DomainTool {

    @Tool(description = "Mô tả tool cho AI")
    public ResponseRecord methodName(
            @ToolParam(description = "Mô tả param", required = true)
            String param
    ) {
        log.info("MCP Tool invoked: methodName(param={})", param);
        return service.doSomething(param);
    }

    public record ResponseRecord(String field1, String field2) {}
}
```

- Annotation: `@Tool` + `@ToolParam` (package `org.springframework.ai.tool.annotation`) — **không phải** `@McpTool`
- 1 file = 1 domain (AccountTool, TransferTool, CardTool)
- Return type: Record/POJO (auto-serialize JSON) hoặc String (plain text)

### Package naming

Template dùng `com.sample.mcp.server` (generic). Khi tạo server thực tế:

```
{group}.mcp.{resource}.server
```

- `{group}`: reverse domain tổ chức (io.acme, vn.mycompany, com.example...)
- `{resource}`: domain resource (account, transaction, payment...)

### Error handling

- `GlobalExceptionHandler` trả `ProblemDetail` (RFC 7807)
- `IllegalArgumentException` → 400, `Exception` catch-all → 500
- Type URI pattern: `urn:error:{error-type}`

### Logging

- Tool invocations: **INFO** với parameters
- Service operations: **DEBUG**
- Exceptions: **WARN** (client error), **ERROR** (server error)
- Pattern: `%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n`

### Testing

- Integration test: `@SpringBootTest` + `MockMvc` — context load, actuator, MCP endpoint
- Unit test: `@ExtendWith(MockitoExtension.class)` — mock service, verify delegation
- Pattern: Given-When-Then, AssertJ assertions

## Configuration (application.yml)

| Property | Default | Ghi chú |
|----------|---------|---------|
| `spring.ai.mcp.server.protocol` | STREAMABLE | STREAMABLE / STATELESS / SSE |
| `spring.ai.mcp.server.type` | SYNC | SYNC / ASYNC |
| `spring.ai.mcp.server.capabilities.tool` | true | Auto-register @Tool beans |
| `spring.ai.mcp.server.streamable-http.max-sessions` | 100 | |
| `server.port` | 8080 | |

Profiles: `prod` (logging WARN/INFO).

## K8s Deployment (Kustomize)

| Overlay | Namespace | Replicas | HPA | CPU req/lim | Mem req/lim |
|---------|-----------|----------|-----|-------------|-------------|
| dev | mcp-dev | 1 | 1→2 | 100m/500m | 256Mi/512Mi |
| staging | mcp-staging | 2 | 2→5 | 250m/1 | 512Mi/1Gi |
| prod | mcp-prod | 3 | 3→20 | 500m/2 | 1Gi/2Gi |

Features: RollingUpdate (zero-downtime), PDB (minAvailable=1), NetworkPolicy, startup/liveness/readiness probes, preStop drain, runAsNonRoot.

## Production Checklist

Đã loại bỏ khỏi template để giữ đơn giản. Thêm lại trước production — xem README.md section "Production Checklist":

- Security (OAuth2 Resource Server)
- Observability (Micrometer + Prometheus + OpenTelemetry)
- Resilience (Resilience4j Circuit Breaker, Retry, Rate Limiter)

## Build & CI

- `mvn verify -B` chạy trong CI (`.github/workflows/ci.yml`)
- Push main → build image qua Jib → push ghcr.io
- Dockerfile (multi-stage, layered JAR) dùng cho build ngoài Jib
