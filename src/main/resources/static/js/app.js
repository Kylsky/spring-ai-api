// 配置Marked.js
marked.setOptions({
    gfm: true, // 启用GitHub风格的Markdown
    breaks: true, // 启用换行符转换为<br>
    headerIds: false, // 禁用给标题添加ID
    sanitize: false, // 不使用内置的sanitize功能（因为已被弃用）
    mangle: false // 禁用给标题添加链接
});

// 通过DOMPurify保障Markdown渲染安全性
// 注意：如果想要使用此功能，需要在index.html中添加DOMPurify库

const app = Vue.createApp({
    data() {
        return {
            model: 'gpt-4o',
            userMessage: '',
            streamMode: true, // 默认使用流式响应且不允许更改
            loading: false,
            statusMessage: '',
            isError: false,
            reconnectAttempts: 0,
            maxReconnectAttempts: 3,
            chatHistory: [],
            isTyping: false,
            lastResponseStartTime: null
        }
    },
    mounted() {
        // 初始化页面时聚焦输入框
        this.$nextTick(() => {
            const messageInput = document.querySelector('textarea');
            if (messageInput) {
                messageInput.focus();
            }
        });
    },
    watch: {
        chatHistory: {
            deep: true,
            handler() {
                this.$nextTick(() => {
                    this.scrollToBottom();
                });
            }
        }
    },
    methods: {
        scrollToBottom() {
            const chatBody = document.querySelector('.chat-body');
            if (chatBody) {
                // 使用平滑滚动
                chatBody.scrollTo({
                    top: chatBody.scrollHeight,
                    behavior: 'smooth'
                });
            }
        },
        async sendRequest() {
            if (!this.userMessage.trim()) {
                return;
            }
            
            // 添加用户消息到聊天历史
            this.chatHistory.push({
                role: 'user',
                content: this.userMessage
            });
            
            // 准备发送请求
            this.userMessage = '';
            this.loading = true;
            this.statusMessage = '';
            this.isError = false;
            this.reconnectAttempts = 0;
            this.isTyping = true;
            this.lastResponseStartTime = Date.now();
            
            const payload = {
                model: this.model,
                messages: this.chatHistory.map(msg => ({
                    role: msg.role,
                    content: msg.content
                })),
                stream: true // 始终使用流式响应
            };
            
            // 准备接收回复的对象
            const aiResponseObject = {
                role: 'assistant',
                content: ''
            };
            
            // 添加到历史
            this.chatHistory.push(aiResponseObject);
            
            // 始终使用流式请求
            this.streamRequest(payload, aiResponseObject);
            
            // 自动聚焦输入框
            this.$nextTick(() => {
                const messageInput = document.querySelector('textarea');
                if (messageInput) {
                    messageInput.focus();
                }
            });
        },
        
        streamRequest(payload, responseObject) {
            try {
                this.statusMessage = '正在处理...';
                
                fetch('/v1/chat/completions', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                })
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`HTTP error! status: ${response.status}`);
                    }
                    this.statusMessage = '';
                    return this.processStream(response, responseObject);
                })
                .catch(error => {
                    console.error('流式请求错误:', error);
                    // 将错误消息显示在对话中
                    responseObject.content = `出错了：${error.message}`;
                    this.loading = false;
                    this.isTyping = false;
                    
                    // 如果是网络错误，尝试重连
                    if (error.name === 'TypeError' && this.reconnectAttempts < this.maxReconnectAttempts) {
                        this.reconnectAttempts++;
                        // 只在状态栏显示重连信息
                        this.statusMessage = `连接失败，正在尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`;
                        setTimeout(() => this.streamRequest(payload, responseObject), 1000);
                    }
                });
            } catch (error) {
                console.error('流式请求初始化错误:', error);
                // 将错误消息显示在对话中
                responseObject.content = `出错了：${error.message}`;
                this.loading = false;
                this.isTyping = false;
            }
        },
        
        async processStream(response, responseObject) {
            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = '';
            let lastUpdateTime = Date.now();
            const updateInterval = 100; // 至少100ms更新一次状态，保证流式感
            
            try {
                while (true) {
                    const { done, value } = await reader.read();
                    
                    const currentTime = Date.now();
                    const timeSinceStart = currentTime - this.lastResponseStartTime;
                    
                    // 处理首次接收数据的延迟
                    if (timeSinceStart > 500 && responseObject.content === '') {
                        this.isTyping = true;
                    }
                    
                    if (done) {
                        // 确保最后一次更新至少在500ms后关闭isTyping
                        const timeSinceLastUpdate = currentTime - lastUpdateTime;
                        if (timeSinceLastUpdate < 500) {
                            await new Promise(resolve => setTimeout(resolve, 500 - timeSinceLastUpdate));
                        }
                        
                        this.statusMessage = '';
                        this.loading = false;
                        this.isTyping = false;
                        break;
                    }
                    
                    // 解码收到的数据
                    const chunk = decoder.decode(value, { stream: true });
                    buffer += chunk;
                    
                    // 处理接收到的数据并清空已处理内容
                    const lines = buffer.split('\n');
                    let processedIndex = -1;
                    let contentUpdated = false;
                    
                    for (let i = 0; i < lines.length; i++) {
                        const line = lines[i];
                        if (line.startsWith('data:') && line.length > 5) {
                            try {
                                const jsonStr = line.substring(5).trim();
                                
                                // 忽略空行或[DONE]标记
                                if (!jsonStr || jsonStr === '[DONE]') {
                                    processedIndex = i;
                                    continue;
                                }
                                
                                const data = JSON.parse(jsonStr);
                                if (data.choices && data.choices.length > 0) {
                                    const delta = data.choices[0].delta;
                                    if (delta && delta.content) {
                                        responseObject.content += delta.content;
                                        contentUpdated = true;
                                    }
                                }
                                processedIndex = i;
                            } catch (e) {
                                console.error('解析流数据出错:', e, line);
                            }
                        }
                    }
                    
                    // 更新UI状态，控制更新频率，确保平滑的流式体验
                    if (contentUpdated && (currentTime - lastUpdateTime > updateInterval)) {
                        lastUpdateTime = currentTime;
                        this.isTyping = true; // 保持打字状态
                        // 这里会触发watch ChatHistory，自动滚动
                    }
                    
                    // 只保留未处理完成的行
                    if (processedIndex >= 0 && processedIndex < lines.length - 1) {
                        buffer = lines.slice(processedIndex + 1).join('\n');
                    } else if (processedIndex === lines.length - 1) {
                        buffer = '';
                    }
                }
            } catch (error) {
                console.error('处理流数据错误:', error);
                // 将错误消息作为AI助手消息添加到聊天历史
                responseObject.content = `出错了：${error.message}`;
                this.loading = false;
                this.isTyping = false;
            }
        }
    }
});

// 挂载到DOM后初始化Vue应用
document.addEventListener('DOMContentLoaded', () => {
    app.mount('#app');
}); 