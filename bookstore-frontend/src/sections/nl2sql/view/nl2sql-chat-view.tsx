import { useRef, useState, useEffect } from 'react';

import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import Chip from '@mui/material/Chip';
import Grid from '@mui/material/Grid';
import Alert from '@mui/material/Alert';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import Divider from '@mui/material/Divider';
import TextField from '@mui/material/TextField';
import CardHeader from '@mui/material/CardHeader';
import Typography from '@mui/material/Typography';
import CardContent from '@mui/material/CardContent';

import { DashboardContent } from 'src/layouts/dashboard';
import {
  type Nl2SqlChatResponse,
  type Nl2SqlSocketClient,
  createNl2SqlSocketClient,
} from 'src/services/nl2sql-websocket';

type ConnectionStatus = 'CONNECTING' | 'OPEN' | 'CLOSED';

type TimelineItem = {
  id: number;
  role: 'USER' | 'ASSISTANT' | 'SYSTEM';
  message: string;
};

function toConnectionColor(status: ConnectionStatus): 'default' | 'success' | 'warning' {
  if (status === 'OPEN') {
    return 'success';
  }
  if (status === 'CONNECTING') {
    return 'warning';
  }
  return 'default';
}

function toConnectionLabel(status: ConnectionStatus): string {
  if (status === 'OPEN') {
    return 'Connected';
  }
  if (status === 'CONNECTING') {
    return 'Connecting';
  }
  return 'Disconnected';
}

function toTimelineRoleLabel(role: TimelineItem['role']): string {
  if (role === 'USER') {
    return 'User';
  }
  if (role === 'ASSISTANT') {
    return 'Assistant';
  }
  return 'System';
}

export function Nl2SqlChatView() {
  const socketRef = useRef<Nl2SqlSocketClient | null>(null);
  const timelineIdRef = useRef(1);

  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('CONNECTING');
  const [socketVersion, setSocketVersion] = useState(0);
  const [queryInput, setQueryInput] = useState('');
  const [feedbackInput, setFeedbackInput] = useState('');
  const [latest, setLatest] = useState<Nl2SqlChatResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState('');
  const [timeline, setTimeline] = useState<TimelineItem[]>([]);

  useEffect(() => {
    setConnectionStatus('CONNECTING');
    setErrorMessage('');

    const socketClient = createNl2SqlSocketClient({
      onOpen: () => {
        setConnectionStatus('OPEN');
        setErrorMessage('');
        setTimeline((previous) => [
          ...previous,
          {
            id: timelineIdRef.current++,
            role: 'SYSTEM',
            message: 'NL2SQL WebSocket 연결이 열렸습니다.',
          },
        ]);
      },
      onClose: () => {
        setConnectionStatus('CLOSED');
      },
      onError: (message) => {
        setErrorMessage(message);
      },
      onResponse: (response) => {
        setLatest(response);
        setErrorMessage('');
        setTimeline((previous) => [
          ...previous,
          {
            id: timelineIdRef.current++,
            role: 'ASSISTANT',
            message: response.message,
          },
        ]);
      },
    });

    socketRef.current = socketClient;

    return () => {
      socketClient.close();
      socketRef.current = null;
    };
  }, [socketVersion]);

  const handleAsk = () => {
    const query = queryInput.trim();
    if (!query) {
      setErrorMessage('질문을 입력해 주세요.');
      return;
    }

    socketRef.current?.sendAsk(query);
    setTimeline((previous) => [
      ...previous,
      {
        id: timelineIdRef.current++,
        role: 'USER',
        message: query,
      },
    ]);
    setLatest(null);
    setFeedbackInput('');
  };

  const handleConfirm = (satisfied: boolean) => {
    if (!latest?.session_id) {
      setErrorMessage('세션이 없어 응답을 보낼 수 없습니다.');
      return;
    }
    socketRef.current?.sendConfirm(latest.session_id, satisfied);
  };

  const handleFeedback = () => {
    if (!latest?.session_id) {
      setErrorMessage('세션이 없어 피드백을 보낼 수 없습니다.');
      return;
    }

    const feedbackText = feedbackInput.trim();
    if (!feedbackText) {
      setErrorMessage('불만족 이유를 입력해 주세요.');
      return;
    }

    socketRef.current?.sendFeedback(latest.session_id, feedbackText);
    setTimeline((previous) => [
      ...previous,
      {
        id: timelineIdRef.current++,
        role: 'USER',
        message: `피드백: ${feedbackText}`,
      },
    ]);
    setFeedbackInput('');
  };

  const canSubmitQuery = connectionStatus === 'OPEN';
  const awaitingConfirmation = latest?.status === 'AWAITING_CONFIRMATION';
  const awaitingFeedbackReason = latest?.status === 'AWAITING_FEEDBACK_REASON';

  return (
    <DashboardContent maxWidth="lg">
      <Stack direction={{ xs: 'column', md: 'row' }} justifyContent="space-between" sx={{ mb: 3 }}>
        <Box>
          <Typography variant="h4">NL2SQL WebSocket</Typography>
          <Typography variant="body2" sx={{ mt: 0.5, color: 'text.secondary' }}>
            질문 → SQL 생성/검증 → yes/no 피드백 기반 교정 흐름
          </Typography>
        </Box>

        <Stack direction="row" spacing={1} alignItems="center">
          <Chip
            label={toConnectionLabel(connectionStatus)}
            color={toConnectionColor(connectionStatus)}
            variant="outlined"
          />
          <Button
            variant="contained"
            color="inherit"
            onClick={() => {
              setSocketVersion((previous) => previous + 1);
            }}
          >
            Reconnect
          </Button>
        </Stack>
      </Stack>

      {!!errorMessage && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {errorMessage}
        </Alert>
      )}

      <Grid container spacing={3}>
        <Grid size={{ xs: 12 }}>
          <Card>
            <CardHeader title="1) 사용자 질의" subheader="질문 입력 후 SQL 생성" />
            <CardContent>
              <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                <TextField
                  fullWidth
                  value={queryInput}
                  label="질문"
                  placeholder="예: 이번 달 가장 많이 주문한 사용자는?"
                  onChange={(event) => {
                    setQueryInput(event.target.value);
                  }}
                />
                <Button
                  variant="contained"
                  disabled={!canSubmitQuery}
                  onClick={handleAsk}
                  sx={{ minWidth: 160 }}
                >
                  SQL 생성 요청
                </Button>
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, md: 7 }}>
          <Card sx={{ height: 1 }}>
            <CardHeader
              title="2~3) 생성/검증 결과"
              subheader="Retriever + Prompt 기반 SQL 생성, Syntax 검증/수정"
            />
            <CardContent>
              {!latest && (
                <Typography variant="body2" color="text.secondary">
                  아직 응답이 없습니다.
                </Typography>
              )}

              {latest && (
                <Stack spacing={2}>
                  <Stack direction="row" spacing={1} flexWrap="wrap">
                    <Chip label={`Status: ${latest.status}`} size="small" />
                    <Chip
                      label={`Attempts: ${latest.attempt_count ?? 0}/${latest.max_attempts ?? 3}`}
                      size="small"
                      variant="outlined"
                    />
                    {typeof latest.validation_passed === 'boolean' && (
                      <Chip
                        size="small"
                        color={latest.validation_passed ? 'success' : 'warning'}
                        label={latest.validation_passed ? 'Syntax Valid' : 'Syntax Repaired'}
                      />
                    )}
                  </Stack>

                  <Typography variant="body2" color="text.secondary">
                    {latest.message}
                  </Typography>

                  {!!latest.query && (
                    <Typography variant="body2">
                      <strong>질문:</strong> {latest.query}
                    </Typography>
                  )}

                  {!!latest.sql && (
                    <Box
                      component="pre"
                      sx={{
                        m: 0,
                        p: 2,
                        overflowX: 'auto',
                        borderRadius: 1,
                        typography: 'body2',
                        bgcolor: 'grey.900',
                        color: 'common.white',
                        fontFamily:
                          'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, Liberation Mono, monospace',
                      }}
                    >
                      {latest.sql}
                    </Box>
                  )}

                  {!!latest.explanation && (
                    <Typography variant="body2" color="text.secondary">
                      {latest.explanation}
                    </Typography>
                  )}

                  {latest.repair?.change_summary && (
                    <Alert severity="info">수정 요약: {latest.repair.change_summary}</Alert>
                  )}
                </Stack>
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, md: 5 }}>
          <Card sx={{ height: 1 }}>
            <CardHeader title="4~6) 만족도 피드백" subheader="yes/no 및 재생성(최대 3회)" />
            <CardContent>
              <Stack spacing={2}>
                <Stack direction="row" spacing={1}>
                  <Button
                    variant="contained"
                    color="success"
                    disabled={!awaitingConfirmation}
                    onClick={() => {
                      handleConfirm(true);
                    }}
                  >
                    Yes
                  </Button>
                  <Button
                    variant="contained"
                    color="warning"
                    disabled={!awaitingConfirmation}
                    onClick={() => {
                      handleConfirm(false);
                    }}
                  >
                    No
                  </Button>
                </Stack>

                <Divider />

                <TextField
                  multiline
                  minRows={3}
                  fullWidth
                  label="불만족 이유"
                  placeholder="예: 결과 컬럼이 달라요, 기간 조건이 잘못됐어요"
                  value={feedbackInput}
                  onChange={(event) => {
                    setFeedbackInput(event.target.value);
                  }}
                  disabled={!awaitingFeedbackReason}
                />

                <Button
                  variant="contained"
                  disabled={!awaitingFeedbackReason}
                  onClick={handleFeedback}
                >
                  이유 제출 후 재생성
                </Button>

                {latest?.status === 'MAX_ATTEMPTS_EXCEEDED' && (
                  <Alert severity="warning">
                    최대 3회 교정 시도 이후에도 불만족으로 처리되어 로그 롤링 저장 상태입니다.
                  </Alert>
                )}
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12 }}>
          <Card>
            <CardHeader title="대화 로그" />
            <CardContent>
              {timeline.length === 0 ? (
                <Typography variant="body2" color="text.secondary">
                  로그가 없습니다.
                </Typography>
              ) : (
                <Stack spacing={1.5}>
                  {timeline.map((item) => (
                    <Box key={item.id}>
                      <Typography variant="caption" color="text.secondary">
                        {toTimelineRoleLabel(item.role)}
                      </Typography>
                      <Typography variant="body2">{item.message}</Typography>
                    </Box>
                  ))}
                </Stack>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </DashboardContent>
  );
}
