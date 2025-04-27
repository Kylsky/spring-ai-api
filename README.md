# OpenAI 兼容 API 实现

本项目实现了兼容 OpenAI API 的接口，支持阻塞式和流式响应模式。

## 功能特点

- 兼容 OpenAI API 格式的请求和响应
- 支持阻塞式调用（传统 HTTP 请求）
- 支持流式调用（Server-Sent Events）
- 基于 Spring AI 实现，易于与现有 Spring Boot 项目集成

## 接口说明

### 聊天补全 API

```
POST /v1/chat/completions
```

请求体示例：

```json
{
  "model": "gpt-4o",
  "messages": [
    {"role": "user", "content": "你好，请介绍一下自己"}
  ],
  "stream": false
}
```

参数说明：

- `model`: 模型名称，如 gpt-4o, gpt-4, gpt-3.5-turbo 等
- `messages`: 对话消息列表
  - `role`: 角色，可选值 system、user、assistant
  - `content`: 消息内容
- `stream`: 是否使用流式响应，true 为流式，false 为阻塞式
- `temperature`: 温度参数（可选）
- `max_tokens`: 最大生成 token 数（可选）
- `n`: 生成结果数量（可选）

## 运行方式

1. 确保 Java 17+ 已安装
2. 配置 application.yml 中的 OpenAI API 密钥和地址
3. 运行应用程序

```bash
mvn spring-boot:run
```

4. 访问 http://localhost:8080 使用 Web 界面测试

## 示例代码

### Java 客户端调用示例

```java
// 阻塞式调用
ChatRequest request = ChatRequest.builder()
    .model("gpt-4o")
    .messages(List.of(new ChatRequest.Message("user", "你好，请介绍一下自己")))
    .stream(false)
    .build();

// 发送请求
ChatResponse response = restTemplate.postForObject("/v1/chat/completions", request, ChatResponse.class);
```

### JavaScript 客户端调用示例

```javascript
// 阻塞式调用
const response = await fetch('/v1/chat/completions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        model: 'gpt-4o',
        messages: [{ role: 'user', content: '你好，请介绍一下自己' }],
        stream: false
    })
});
const data = await response.json();
```

```javascript
// 流式调用
const response = await fetch('/v1/chat/completions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        model: 'gpt-4o',
        messages: [{ role: 'user', content: '你好，请介绍一下自己' }],
        stream: true
    })
});

const reader = response.body.getReader();
const decoder = new TextDecoder();

while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    
    const chunk = decoder.decode(value);
    const lines = chunk.split('\n');
    
    for (const line of lines) {
        if (line.startsWith('data:') && line.length > 5) {
            const data = JSON.parse(line.substring(5));
            if (data.choices && data.choices.length > 0) {
                const content = data.choices[0].delta.content;
                if (content) {
                    console.log(content);
                }
            }
        }
    }
}
```