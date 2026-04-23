package com.sample.mcp.server.tool;

import com.sample.mcp.server.service.SampleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * Mẫu MCP Tool sử dụng annotation @Tool.
 *
 * Convention:
 * - 1 file = 1 domain (vd: AccountTool, TransferTool, CardTool)
 * - Method name = tool name (snake_case tự động)
 * - Return type = String (text content) hoặc Record/POJO (auto-serialize JSON)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SampleTool {

    private final SampleService sampleService;

    @Tool(description = "Look up entity information by ID. Returns entity details including name and status.")
    public SampleResponse lookupEntity(
            @ToolParam(description = "Unique entity identifier (UUID format)", required = true)
            String entityId
    ) {
        log.info("MCP Tool invoked: lookupEntity(entityId={})", entityId);
        return sampleService.findById(entityId);
    }

    @Tool(description = "Create a new entity with the given name and optional tags. Returns the created entity with generated ID.")
    public SampleResponse createEntity(
            @ToolParam(description = "Display name for the entity", required = true)
            String name,
            @ToolParam(description = "Comma-separated tags for categorization", required = false)
            String tags
    ) {
        log.info("MCP Tool invoked: createEntity(name={}, tags={})", name, tags);
        return sampleService.create(name, tags);
    }

    public record SampleResponse(
            String id,
            String name,
            String status,
            String tags
    ) {}
}
