// Chatbot functionality
class PortfolioChatbot {
    constructor() {
        this.isOpen = false;
        this.messages = [];
        this.init();
    }

    init() {
        this.createChatbotUI();
        this.attachEventListeners();
        this.loadPredefinedQuestions();
    }

    createChatbotUI() {
        const chatbotHTML = `
            <div class="chatbot-container">
                <button class="chatbot-toggle" id="chatbotToggle" title="Chat with Portfolio Assistant">
                    💬
                </button>
                
                <div class="chatbot-window" id="chatbotWindow">
                    <div class="chatbot-header">
                        <h3>🤖 Portfolio Assistant</h3>
                        <button class="chatbot-close" id="chatbotClose">&times;</button>
                    </div>
                    
                    <div class="chatbot-messages" id="chatbotMessages">
                        <div class="welcome-message">
                            <h4>👋 Welcome!</h4>
                            <p>I'm your portfolio assistant. Choose a question below!</p>
                        </div>
                    </div>
                    
                    <div class="chatbot-typing" id="chatbotTyping">
                        <div class="typing-dot"></div>
                        <div class="typing-dot"></div>
                        <div class="typing-dot"></div>
                    </div>
                    
                    <div class="chatbot-options" id="chatbotOptions">
                        <div class="chatbot-options-title">Quick Questions:</div>
                        <div class="option-buttons" id="optionButtons">
                            <!-- Options will be loaded dynamically -->
                        </div>
                    </div>

                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', chatbotHTML);
    }

    attachEventListeners() {
        const toggle = document.getElementById('chatbotToggle');
        const close = document.getElementById('chatbotClose');

        toggle.addEventListener('click', () => this.toggleChatbot());
        close.addEventListener('click', () => this.toggleChatbot());
    }

    toggleChatbot() {
        this.isOpen = !this.isOpen;
        const window = document.getElementById('chatbotWindow');
        const toggle = document.getElementById('chatbotToggle');

        if (this.isOpen) {
            window.classList.add('active');
            toggle.classList.add('active');
            toggle.textContent = '✕';
        } else {
            window.classList.remove('active');
            toggle.classList.remove('active');
            toggle.textContent = '💬';
        }
    }

    async loadPredefinedQuestions() {
        try {
            const response = await fetch(`${API_BASE_URL}/chatbot/questions`);
            if (response.ok) {
                const questions = await response.json();
                this.renderOptions(questions);
            }
        } catch (error) {
            console.error('Error loading questions:', error);
            // Fallback questions
            const fallbackQuestions = [
                "How is my portfolio performing today?",
                "What is my total portfolio value?",
                "How much profit or loss am I currently in?",
                "Show my portfolio summary",
                "What assets do I currently hold?"
            ];
            this.renderOptions(fallbackQuestions);
        }
    }

    renderOptions(questions) {
        const optionButtons = document.getElementById('optionButtons');
        optionButtons.innerHTML = '';

        const icons = ['📊', '💰', '📈', '🧾', '📋'];

        questions.forEach((question, index) => {
            const button = document.createElement('button');
            button.className = 'option-btn';
            button.innerHTML = `<span>${icons[index] || '❓'}</span> ${question}`;
            button.addEventListener('click', () => {
                this.sendPredefinedMessage(question);
            });
            optionButtons.appendChild(button);
        });
    }

    sendPredefinedMessage(message) {
        this.addMessage(message, 'user');
        this.processQuery(message);
    }

    async sendMessage() {
        const input = document.getElementById('chatbotInput');
        if (!input) return;

        const message = input.value.trim();

        if (!message) return;

        this.addMessage(message, 'user');
        input.value = '';

        await this.processQuery(message);
    }

    async processQuery(query) {
        this.showTyping();

        try {
            const response = await fetch(`${API_BASE_URL}/chatbot/query`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ query })
            });

            if (!response.ok) {
                throw new Error('Failed to get response from chatbot');
            }

            const data = await response.json();
            this.hideTyping();

            if (data.success) {
                this.addMessage(data.response, 'bot');
            } else {
                this.addMessage('Sorry, I encountered an error processing your request. Please try again.', 'bot');
            }
        } catch (error) {
            console.error('Chatbot error:', error);
            this.hideTyping();
            this.addMessage('I\'m having trouble connecting right now. Please check if the server is running and try again.', 'bot');
        }
    }

    addMessage(text, sender) {
        const messagesContainer = document.getElementById('chatbotMessages');
        const messageDiv = document.createElement('div');
        messageDiv.className = `chatbot-message ${sender}`;

        const now = new Date();
        const timeString = now.toLocaleTimeString('en-US', {
            hour: '2-digit',
            minute: '2-digit'
        });

        messageDiv.innerHTML = `
            <div class="message-bubble">
                ${this.formatMessage(text)}
                <div class="message-timestamp">${timeString}</div>
            </div>
        `;

        messagesContainer.appendChild(messageDiv);
        this.scrollToBottom();

        this.messages.push({ text, sender, timestamp: now });
    }

    formatMessage(text) {
        // Convert markdown-style formatting to HTML
        let formatted = text;

        // Bold text
        formatted = formatted.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');

        // Line breaks
        formatted = formatted.replace(/\n/g, '<br>');

        return formatted;
    }

    showTyping() {
        const typing = document.getElementById('chatbotTyping');
        typing.classList.add('active');
        this.scrollToBottom();
    }

    hideTyping() {
        const typing = document.getElementById('chatbotTyping');
        typing.classList.remove('active');
    }

    scrollToBottom() {
        const messagesContainer = document.getElementById('chatbotMessages');
        setTimeout(() => {
            messagesContainer.scrollTop = messagesContainer.scrollHeight;
        }, 100);
    }

    clearMessages() {
        const messagesContainer = document.getElementById('chatbotMessages');
        messagesContainer.innerHTML = `
            <div class="welcome-message">
                <h4>👋 Welcome!</h4>
                <p>I'm your portfolio assistant. Choose a question below!</p>
            </div>
        `;
        this.messages = [];
    }
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        window.portfolioChatbot = new PortfolioChatbot();
    });
} else {
    window.portfolioChatbot = new PortfolioChatbot();
}
