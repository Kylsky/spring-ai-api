app.component('chat-controls', {
    props: ['loading', 'userMessage', 'statusMessage', 'isError'],
    emits: ['update:userMessage', 'send-request'],
    data() {
        return {
            isComposing: false
        }
    },
    methods: {
        updateUserMessage(e) {
            this.$emit('update:userMessage', e.target.value);
            this.adjustTextareaHeight(e.target);
        },
        handleKeydown(e) {
            if (e.key === 'Enter' && !e.shiftKey && !this.isComposing) {
                e.preventDefault();
                this.sendRequest();
            }
        },
        handleCompositionStart() {
            this.isComposing = true;
        },
        handleCompositionEnd() {
            this.isComposing = false;
        },
        sendRequest() {
            if (this.userMessage.trim() && !this.loading) {
                this.$emit('send-request');
            }
        },
        adjustTextareaHeight(element) {
            element.style.height = 'auto';
            const newHeight = Math.min(Math.max(element.scrollHeight, 38), 150);
            element.style.height = `${newHeight}px`;
        }
    },
    computed: {
        placeholderText() {
            // 只有在不是错误状态下才显示状态消息
            if (this.statusMessage && !this.isError) {
                return this.statusMessage;
            }
            return '输入您的问题...';
        }
    },
    mounted() {
        this.$nextTick(() => {
            const textarea = this.$refs.messageInput;
            if (textarea) {
                this.adjustTextareaHeight(textarea);
            }
        });
    },
    template: `
    <div class="chat-controls">
        <div class="form-group">
            <div class="input-group">
                <div class="textarea-wrapper">
                    <textarea 
                        :value="userMessage"
                        @input="updateUserMessage"
                        @keydown="handleKeydown"
                        @compositionstart="handleCompositionStart"
                        @compositionend="handleCompositionEnd"
                        rows="1" 
                        :placeholder="placeholderText" 
                        ref="messageInput">
                    </textarea>
                </div>
                <button @click="sendRequest" class="btn" :disabled="loading || !userMessage.trim()">
                    <span class="btn-text">发送</span>
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="send-icon">
                        <line x1="22" y1="2" x2="11" y2="13"></line>
                        <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
                    </svg>
                </button>
            </div>
        </div>
    </div>
    `
}); 