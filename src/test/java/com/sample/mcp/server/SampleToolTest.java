package com.sample.mcp.server;

import com.sample.mcp.server.service.SampleService;
import com.sample.mcp.server.tool.SampleTool;
import com.sample.mcp.server.tool.SampleTool.SampleResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SampleToolTest {

    @Mock
    private SampleService sampleService;

    @InjectMocks
    private SampleTool sampleTool;

    @Test
    void lookupEntity_shouldDelegateToService() {
        // given
        var entityId = "test-id-123";
        var expected = new SampleResponse(entityId, "Test", "ACTIVE", "tag1");
        when(sampleService.findById(eq(entityId))).thenReturn(expected);

        // when
        var result = sampleTool.lookupEntity(entityId);

        // then
        assertThat(result).isEqualTo(expected);
        verify(sampleService).findById(entityId);
    }

    @Test
    void createEntity_shouldDelegateToService() {
        // given
        var name = "New Entity";
        var tags = "a,b";
        var expected = new SampleResponse("gen-id", name, "CREATED", tags);
        when(sampleService.create(eq(name), eq(tags))).thenReturn(expected);

        // when
        var result = sampleTool.createEntity(name, tags);

        // then
        assertThat(result.name()).isEqualTo(name);
        assertThat(result.status()).isEqualTo("CREATED");
    }
}
