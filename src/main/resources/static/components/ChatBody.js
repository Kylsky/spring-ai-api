app.component('chat-body', {
    props: ['chatHistory', 'isTyping'],
    methods: {
        formatMessage(text) {
            if (!text) return '';
            // 使用marked将markdown转换为HTML，然后用DOMPurify进行安全处理
            const rawHtml = marked.parse(text);
            return DOMPurify.sanitize(rawHtml);
        }
    },
    template: `
    <div class="chat-body" ref="chatBody">
        <div class="chat-welcome" v-if="chatHistory.length === 0">
            <div class="welcome-icon">
                <svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"></circle>
                    <path d="M8 14s1.5 2 4 2 4-2 4-2"></path>
                    <line x1="9" y1="9" x2="9.01" y2="9"></line>
                    <line x1="15" y1="9" x2="15.01" y2="9"></line>
                </svg>
            </div>
            <div class="response welcome-message">
                <h2>您好！我是AI智能助手</h2>
                <p>有什么可以帮您解答的问题吗？我可以：</p>
                <ul>
                    <li>回答各类问题和提供信息</li>
                    <li>解释复杂的概念</li>
                    <li>提供技术支持和建议</li>
                    <li>帮助创意构思和问题解决</li>
                </ul>
            </div>
        </div>
        
        <template v-for="(message, index) in chatHistory">
            <!-- 用户消息 -->
            <div class="message-group user-group" v-if="message.role === 'user'" :key="'user-'+index">
                
                <div class="response-container user-container">
                    <div class="response user-message">
                        {{ message.content }}
                    </div>
                </div>
            </div>
            
            <!-- AI助手消息 -->
            <div class="message-group ai-group" v-else :key="'ai-'+index">
                
                <div class="response-container">
                    
                    <div class="response">
                        <div v-html="formatMessage(message.content)"></div>
                    </div>
                </div>
            </div>
        </template>
        
        <!-- 打字指示器，仅在开始新回复时显示 -->
        <div class="message-group ai-group" v-if="isTyping && (chatHistory.length === 0 || chatHistory[chatHistory.length-1].role === 'user')">
            <div class="message-avatar">
                <div class="avatar assistant-avatar">
                    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                        <line x1="3" y1="9" x2="21" y2="9"></line>
                        <line x1="9" y1="21" x2="9" y2="9"></line>
                    </svg>
                </div>
            </div>
            <div class="response-container">
                <div class="response-header">
                    <span class="response-role">AI助手</span>
                    <span class="response-time">{{ new Date().toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) }}</span>
                </div>
                <div class="response typing-response">
                    <div class="typing-indicator">
                        <span></span>
                        <span></span>
                        <span></span>
                    </div>
                </div>
            </div>
        </div>
    </div>
    `
}); 