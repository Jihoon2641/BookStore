import type { FormEvent, ChangeEvent } from 'react';

import { useState, useCallback } from 'react';

import Box from '@mui/material/Box';
import Link from '@mui/material/Link';
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import IconButton from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import InputAdornment from '@mui/material/InputAdornment';

import { useRouter } from 'src/routes/hooks';
import { RouterLink } from 'src/routes/components';

import { signInAdmin, signUpAdmin } from 'src/services/admin-auth';

import { Iconify } from 'src/components/iconify';

// ----------------------------------------------------------------------

export function SignUpView() {
  const router = useRouter();

  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [adminId, setAdminId] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleAdminIdChange = useCallback((event: ChangeEvent<HTMLInputElement>) => {
    setAdminId(event.target.value);
  }, []);

  const handlePasswordChange = useCallback((event: ChangeEvent<HTMLInputElement>) => {
    setPassword(event.target.value);
  }, []);

  const handleConfirmPasswordChange = useCallback((event: ChangeEvent<HTMLInputElement>) => {
    setConfirmPassword(event.target.value);
  }, []);

  const handleSignUp = useCallback(
    async (event: FormEvent<HTMLFormElement>) => {
      event.preventDefault();

      const trimmedAdminId = adminId.trim();

      if (!trimmedAdminId || !password || !confirmPassword) {
        setErrorMessage('모든 항목을 입력해 주세요.');
        return;
      }

      if (trimmedAdminId.length < 4 || trimmedAdminId.length > 30) {
        setErrorMessage('관리자 아이디는 4자 이상 30자 이하여야 합니다.');
        return;
      }

      if (password.length < 8 || password.length > 15) {
        setErrorMessage('관리자 비밀번호는 8자 이상 15자 이하여야 합니다.');
        return;
      }

      if (password !== confirmPassword) {
        setErrorMessage('비밀번호 확인 값이 일치하지 않습니다.');
        return;
      }

      setErrorMessage('');
      setIsSubmitting(true);

      try {
        await signUpAdmin(trimmedAdminId, password);
        await signInAdmin(trimmedAdminId, password);
        router.replace('/');
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : '회원가입 처리 중 오류가 발생했습니다.');
      } finally {
        setIsSubmitting(false);
      }
    },
    [adminId, confirmPassword, password, router]
  );

  const renderForm = (
    <Box
      component="form"
      onSubmit={handleSignUp}
      sx={{
        display: 'flex',
        alignItems: 'flex-end',
        flexDirection: 'column',
      }}
    >
      {!!errorMessage && (
        <Alert severity="error" sx={{ width: 1, mb: 3 }}>
          {errorMessage}
        </Alert>
      )}

      <TextField
        fullWidth
        name="adminId"
        label="Admin ID"
        value={adminId}
        onChange={handleAdminIdChange}
        autoComplete="username"
        sx={{ mb: 3 }}
        slotProps={{
          inputLabel: { shrink: true },
        }}
      />

      <TextField
        fullWidth
        name="password"
        label="Password"
        value={password}
        onChange={handlePasswordChange}
        type={showPassword ? 'text' : 'password'}
        autoComplete="new-password"
        sx={{ mb: 3 }}
        slotProps={{
          inputLabel: { shrink: true },
          input: {
            endAdornment: (
              <InputAdornment position="end">
                <IconButton onClick={() => setShowPassword(!showPassword)} edge="end">
                  <Iconify icon={showPassword ? 'solar:eye-bold' : 'solar:eye-closed-bold'} />
                </IconButton>
              </InputAdornment>
            ),
          },
        }}
      />

      <TextField
        fullWidth
        name="confirmPassword"
        label="Confirm password"
        value={confirmPassword}
        onChange={handleConfirmPasswordChange}
        type={showConfirmPassword ? 'text' : 'password'}
        autoComplete="new-password"
        sx={{ mb: 3 }}
        slotProps={{
          inputLabel: { shrink: true },
          input: {
            endAdornment: (
              <InputAdornment position="end">
                <IconButton
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  edge="end"
                >
                  <Iconify
                    icon={showConfirmPassword ? 'solar:eye-bold' : 'solar:eye-closed-bold'}
                  />
                </IconButton>
              </InputAdornment>
            ),
          },
        }}
      />

      <Button fullWidth size="large" type="submit" color="inherit" variant="contained" disabled={isSubmitting}>
        {isSubmitting ? 'Signing up...' : 'Sign up'}
      </Button>
    </Box>
  );

  return (
    <>
      <Box
        sx={{
          gap: 1.5,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          mb: 5,
        }}
      >
        <Typography variant="h5">Admin Sign up</Typography>
        <Typography
          variant="body2"
          sx={{
            color: 'text.secondary',
          }}
        >
          Already have an account?
          <Link component={RouterLink} href="/sign-in" variant="subtitle2" sx={{ ml: 0.5 }}>
            Sign in
          </Link>
        </Typography>
      </Box>
      {renderForm}
    </>
  );
}
