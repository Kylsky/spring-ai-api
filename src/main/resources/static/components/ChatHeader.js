app.component('chat-header', {
    props: ['model'],
    emits: ['update:model'],
    methods: {
        updateModel(e) {
            this.$emit('update:model', e.target.value);
        }
    },
    template: `
    <div class="chat-header">
        <div class="header-brand">
            <div class="brand-logo">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
                    <polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline>
                    <line x1="12" y1="22.08" x2="12" y2="12"></line>
                </svg>
            </div>
            <h1>AI 智能助手</h1>
        </div>
        <div class="model-selector">
            <span>模型:</span>
            <select :value="model" @change="updateModel">
                <option value="gpt-4o">GPT-4o</option>
                <option value="gpt-4">GPT-4</option>
                <option value="gpt-3.5-turbo">GPT-3.5 Turbo</option>
            </select>
        </div>
    </div>
    `
}); 