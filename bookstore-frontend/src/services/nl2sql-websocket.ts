const DEFAULT_NL2SQL_WS_URL = import.meta.env.VITE_NL2SQL_WS_URL ?? 'ws://localhost:8001/v1/chat/ws';

export type Nl2SqlStatus =
  | 'AWAITING_CONFIRMATION'
  | 'AWAITING_FEEDBACK_REASON'
  | 'DONE'
  | 'MAX_ATTEMPTS_EXCEEDED';

export type Nl2SqlRepairResult = {
  success: boolean;
  issue_type: string;
  issue_reason: string;
  corrected_sql?: string | null;
  change_summary?: string | null;
  intent_alignment_check?: string | null;
  attempts: number;
  validator_error?: string | null;
  validator_suggestions: string[];
  saved_to_few_shot: boolean;
  example_id?: string | null;
};

export type Nl2SqlChatResponse = {
  type: 'chat.response';
  session_id: string;
  status: Nl2SqlStatus;
  message: string;
  query?: string | null;
  sql?: string | null;
  explanation?: string | null;
  validation_passed?: boolean | null;
  repair?: Nl2SqlRepairResult | null;
  attempt_count?: number | null;
  max_attempts?: number | null;
};

type Nl2SqlErrorResponse = {
  type: 'chat.error';
  code?: string;
  message: string;
};

type Nl2SqlAskRequest = {
  action: 'ASK';
  query: string;
  session_id?: string;
};

type Nl2SqlConfirmRequest = {
  action: 'CONFIRM';
  session_id: string;
  satisfied: boolean;
};

type Nl2SqlFeedbackRequest = {
  action: 'FEEDBACK';
  session_id: string;
  feedback_text: string;
};

type Nl2SqlRequest = Nl2SqlAskRequest | Nl2SqlConfirmRequest | Nl2SqlFeedbackRequest;

export type Nl2SqlSocketHandlers = {
  onOpen?: () => void;
  onClose?: () => void;
  onResponse?: (response: Nl2SqlChatResponse) => void;
  onError?: (message: string) => void;
};

export type Nl2SqlSocketClient = {
  sendAsk: (query: string, sessionId?: string) => void;
  sendConfirm: (sessionId: string, satisfied: boolean) => void;
  sendFeedback: (sessionId: string, feedbackText: string) => void;
  close: () => void;
  readyState: () => number;
};

function parseIncomingMessage(raw: unknown): Nl2SqlChatResponse | Nl2SqlErrorResponse | null {
  if (!raw || typeof raw !== 'object') {
    return null;
  }

  const payload = raw as { type?: string; message?: string };
  if (payload.type !== 'chat.response' && payload.type !== 'chat.error') {
    return null;
  }

  if (payload.type === 'chat.error') {
    return {
      type: 'chat.error',
      code: (raw as { code?: string }).code,
      message: payload.message ?? '알 수 없는 오류가 발생했습니다.',
    };
  }

  return raw as Nl2SqlChatResponse;
}

export function createNl2SqlSocketClient(
  handlers: Nl2SqlSocketHandlers,
  url = DEFAULT_NL2SQL_WS_URL
): Nl2SqlSocketClient {
  const socket = new WebSocket(url);

  socket.onopen = () => {
    handlers.onOpen?.();
  };

  socket.onclose = () => {
    handlers.onClose?.();
  };

  socket.onerror = () => {
    handlers.onError?.('NL2SQL WebSocket 연결 중 오류가 발생했습니다.');
  };

  socket.onmessage = (event) => {
    try {
      const parsed = JSON.parse(event.data);
      const message = parseIncomingMessage(parsed);

      if (!message) {
        handlers.onError?.('알 수 없는 메시지 형식입니다.');
        return;
      }

      if (message.type === 'chat.error') {
        handlers.onError?.(message.message);
        return;
      }

      handlers.onResponse?.(message);
    } catch {
      handlers.onError?.('WebSocket 메시지 파싱에 실패했습니다.');
    }
  };

  const send = (payload: Nl2SqlRequest) => {
    if (socket.readyState !== WebSocket.OPEN) {
      handlers.onError?.('WebSocket 연결이 열려 있지 않습니다.');
      return;
    }
    socket.send(JSON.stringify(payload));
  };

  return {
    sendAsk: (query, sessionId) => {
      send({
        action: 'ASK',
        query,
        session_id: sessionId,
      });
    },
    sendConfirm: (sessionId, satisfied) => {
      send({
        action: 'CONFIRM',
        session_id: sessionId,
        satisfied,
      });
    },
    sendFeedback: (sessionId, feedbackText) => {
      send({
        action: 'FEEDBACK',
        session_id: sessionId,
        feedback_text: feedbackText,
      });
    },
    close: () => {
      socket.close();
    },
    readyState: () => socket.readyState,
  };
}
