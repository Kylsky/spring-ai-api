package com.yeelovo.ai.service;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class WeatherService {
    //@Tool(description = "Get weather information by city name")
    //public String getWeather(String cityName) {
    //    //var transport = new WebFluxSseClientTransport(WebClient.builder().baseUrl("https://mcp.amap.com/sse?key=1320a184ce92c4ee5d7fa3785781cc65"));
    //    //var client = McpClient.sync(transport).build();
    //    //client.callTool(new McpSchema.CallToolRequest("ai.user.input"))
    //    return "";
    //}
}
