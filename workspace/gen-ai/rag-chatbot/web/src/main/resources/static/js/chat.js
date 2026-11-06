/**
 * 챗봇 모달 (F3-3 ~ F3-13).
 * - 메시지는 textContent로만 렌더링한다 (innerHTML 금지, XSS 방지).
 * - session_id와 대화 내용은 sessionStorage에 보관해 모달을 닫거나 새로고침해도 유지한다.
 */
(() => {
	'use strict';

	const API_URL = '/api/chat';
	const STORAGE_SESSION = 'seoulplan.chat.sessionId';
	const STORAGE_MESSAGES = 'seoulplan.chat.messages';
	const WELCOME = '안녕하세요! 2040 서울도시기본계획에 대해 궁금한 점을 물어보세요.';
	const GENERIC_ERROR = '일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.';
	const TIMEOUT_ERROR = '응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.';
	const REQUEST_TIMEOUT_MS = 65000; // 서버 프록시 타임아웃(60초)보다 약간 길게
	const MAX_CHARS = 1000;

	const launcher = document.getElementById('chat-launcher');
	const dialog = document.getElementById('chat-dialog');
	const closeButton = document.getElementById('chat-close');
	const list = document.getElementById('chat-messages');
	const form = document.getElementById('chat-form');
	const input = document.getElementById('chat-input');
	const sendButton = document.getElementById('chat-send');

	let pending = false;
	let typingElement = null;

	// ---------- 상태 보관 ----------

	function createUuid() {
		if (window.crypto && typeof window.crypto.randomUUID === 'function') {
			return window.crypto.randomUUID();
		}
		// randomUUID를 지원하지 않는 환경(비보안 컨텍스트 등)용 RFC 4122 v4
		const bytes = window.crypto.getRandomValues(new Uint8Array(16));
		bytes[6] = (bytes[6] & 0x0f) | 0x40;
		bytes[8] = (bytes[8] & 0x3f) | 0x80;
		const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
		return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
	}

	function getSessionId() {
		let id = sessionStorage.getItem(STORAGE_SESSION);
		if (!id) {
			id = createUuid();
			sessionStorage.setItem(STORAGE_SESSION, id);
		}
		return id;
	}

	function loadMessages() {
		try {
			const saved = JSON.parse(sessionStorage.getItem(STORAGE_MESSAGES) || '[]');
			return Array.isArray(saved) ? saved : [];
		} catch {
			return [];
		}
	}

	const messages = loadMessages();

	function saveMessages() {
		try {
			sessionStorage.setItem(STORAGE_MESSAGES, JSON.stringify(messages));
		} catch {
			// 저장 공간 부족 등: 화면 표시에는 영향 없음
		}
	}

	// ---------- 렌더링 ----------

	/** **굵게** 표기만 <strong>으로 바꾸고 나머지는 텍스트 노드로 넣는다 (innerHTML 미사용). */
	function appendFormattedText(parent, text) {
		const parts = text.split(/\*\*(.+?)\*\*/g);
		parts.forEach((part, index) => {
			if (!part) {
				return;
			}
			if (index % 2 === 1) {
				const strong = document.createElement('strong');
				strong.textContent = part;
				parent.appendChild(strong);
			} else {
				parent.appendChild(document.createTextNode(part));
			}
		});
	}

	/** 출처를 따로 표시하므로 답변 끝의 "참고: p.X" 줄은 화면에서 뺀다. */
	function stripCitationLine(text) {
		return text.replace(/\n*참고\s*:[^\n]*\s*$/, '').trimEnd();
	}

	function renderMessage(message) {
		const bubble = document.createElement('div');
		bubble.className = `chat-msg chat-msg--${message.role}`;

		const hasSources = Array.isArray(message.sources) && message.sources.length > 0;
		const text = message.role === 'bot' && hasSources ? stripCitationLine(message.text) : message.text;
		if (message.role === 'bot') {
			appendFormattedText(bubble, text);
		} else {
			bubble.textContent = text;
		}

		if (hasSources) {
			const sources = document.createElement('div');
			sources.className = 'chat-msg__sources';
			sources.textContent = `참고 페이지: ${message.sources.map((page) => `p.${page}`).join(', ')}`;
			bubble.appendChild(sources);
		}

		list.appendChild(bubble);
		scrollToBottom();
	}

	function addMessage(message) {
		messages.push(message);
		saveMessages();
		renderMessage(message);
	}

	function scrollToBottom() {
		list.scrollTop = list.scrollHeight;
	}

	function setPending(value) {
		pending = value;
		sendButton.disabled = value;
		list.setAttribute('aria-busy', String(value));

		if (value && !typingElement) {
			typingElement = document.createElement('div');
			typingElement.className = 'chat-msg chat-msg--bot chat-typing';
			typingElement.setAttribute('aria-label', '답변을 작성하고 있습니다');
			for (let i = 0; i < 3; i += 1) {
				typingElement.appendChild(document.createElement('span'));
			}
			list.appendChild(typingElement);
			scrollToBottom();
		} else if (!value && typingElement) {
			typingElement.remove();
			typingElement = null;
		}
	}

	// ---------- 서버 호출 ----------

	async function readErrorMessage(response) {
		try {
			const body = await response.json();
			if (body && typeof body.detail === 'string' && body.detail) {
				return body.detail;
			}
		} catch {
			// JSON이 아닌 오류 본문
		}
		return GENERIC_ERROR;
	}

	async function sendMessage(text) {
		addMessage({ role: 'user', text });
		setPending(true);

		const controller = new AbortController();
		const timer = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
		try {
			const response = await fetch(API_URL, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
				body: JSON.stringify({ session_id: getSessionId(), message: text }),
				signal: controller.signal,
			});
			if (!response.ok) {
				addMessage({ role: 'error', text: await readErrorMessage(response) });
				return;
			}
			const data = await response.json();
			addMessage({
				role: 'bot',
				text: data.answer,
				sources: (data.sources || []).map((source) => source.page),
			});
		} catch (error) {
			addMessage({ role: 'error', text: error.name === 'AbortError' ? TIMEOUT_ERROR : GENERIC_ERROR });
		} finally {
			clearTimeout(timer);
			setPending(false);
			input.focus();
		}
	}

	// ---------- 모달 열기/닫기 ----------

	function openChat() {
		if (!dialog.open) {
			dialog.showModal();
		}
		if (messages.length === 0) {
			addMessage({ role: 'bot', text: WELCOME }); // F3-6
		}
		scrollToBottom();
		input.focus();
	}

	launcher.addEventListener('click', (event) => {
		event.preventDefault();
		openChat();
	});

	closeButton.addEventListener('click', () => dialog.close());

	// 모달 바깥(배경) 클릭 시 닫기. ESC는 <dialog> 기본 동작으로 닫힌다.
	dialog.addEventListener('click', (event) => {
		if (event.target === dialog) {
			dialog.close();
		}
	});

	dialog.addEventListener('close', () => launcher.focus());

	// ---------- 입력 ----------

	form.addEventListener('submit', (event) => {
		event.preventDefault();
		const text = input.value.trim();
		if (!text || pending) {
			return; // F3-7: 빈 메시지는 전송하지 않음
		}
		if (text.length > MAX_CHARS) {
			addMessage({ role: 'error', text: '질문은 1,000자 이내로 입력해 주세요.' });
			return;
		}
		input.value = '';
		sendMessage(text);
	});

	input.addEventListener('keydown', (event) => {
		// Enter: 전송, Shift+Enter: 줄바꿈. 한글 조합 중 Enter는 무시 (IME)
		if (event.key === 'Enter' && !event.shiftKey && !event.isComposing && event.keyCode !== 229) {
			event.preventDefault();
			form.requestSubmit();
		}
	});

	// 저장된 대화 복원 (F3-10)
	messages.forEach(renderMessage);
})();
