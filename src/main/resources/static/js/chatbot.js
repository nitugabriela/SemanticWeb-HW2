document.addEventListener("DOMContentLoaded", () => {
    const chatWidget = document.getElementById("chat-widget");
    const openBtn = document.getElementById("chat-open-btn");
    const closeBtn = document.getElementById("chat-close-btn");
    const sendBtn = document.getElementById("chat-send-btn");
    const userInput = document.getElementById("chat-user-input");
    const messagesArea = document.getElementById("chat-messages");
    const startersArea = document.getElementById("chat-starters");

    openBtn.addEventListener("click", () => {
        chatWidget.style.display = "flex";
        openBtn.style.display = "none";
        loadStarters();
    });

    closeBtn.addEventListener("click", () => {
        chatWidget.style.display = "none";
        openBtn.style.display = "block";
    });

    function loadStarters() {
        startersArea.innerHTML = "";
        const currentUrl = window.location.pathname;
        let starters = [];

        if (currentUrl.includes("/book/")) {
            starters = [
                "What is the theme of this book?",
                "Who is the author of this book?",
                "Is this book suitable for a Beginner reader?"
            ];
        } else {
            starters = [
                "What is a book that I am most likely to enjoy from this list?",
                "Can you recommend a Science Fiction book?",
                "What books are suitable for Advanced readers?"
            ];
        }

        starters.forEach(text => {
            const btn = document.createElement("button");
            btn.className = "starter-btn";
            btn.innerText = text;
            btn.onclick = () => {
                userInput.value = text;
                sendMessage();
                startersArea.style.display = "none";
            };
            startersArea.appendChild(btn);
        });
        startersArea.style.display = "flex";
    }

    async function sendMessage() {
        const text = userInput.value.trim();
        if (!text) return;

        appendMessage("user", text);
        userInput.value = "";
        startersArea.style.display = "none";

        try {
            const response = await fetch("/api/chat", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ message: text })
            });

            const data = await response.json();
            appendMessage("bot", data.reply);
        } catch (error) {
            appendMessage("bot", "Sorry, an error occurred while connecting to the AI.");
            console.error(error);
        }
    }

    function appendMessage(sender, text) {
        const msgDiv = document.createElement("div");
        msgDiv.className = `chat-message ${sender}`;
        msgDiv.innerText = text;
        messagesArea.appendChild(msgDiv);
        messagesArea.scrollTop = messagesArea.scrollHeight; // Auto-scroll to bottom
    }

    sendBtn.addEventListener("click", sendMessage);
    userInput.addEventListener("keypress", (e) => {
        if (e.key === "Enter") sendMessage();
    });
});