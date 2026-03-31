import type { RouteObject } from 'react-router';

import { lazy, Suspense } from 'react';
import { Outlet } from 'react-router-dom';
import { varAlpha } from 'minimal-shared/utils';

import Box from '@mui/material/Box';
import LinearProgress, { linearProgressClasses } from '@mui/material/LinearProgress';

import { AuthLayout } from 'src/layouts/auth';
import { DashboardLayout } from 'src/layouts/dashboard';

// ----------------------------------------------------------------------

export const DashboardPage = lazy(() => import('src/pages/dashboard'));
export const UserPage = lazy(() => import('src/pages/user'));
export const SystemPage = lazy(() => import('src/pages/system'));
export const SystemDbPage = lazy(() => import('src/pages/system-db'));
export const SystemJvmPage = lazy(() => import('src/pages/system-jvm'));
export const Nl2SqlPage = lazy(() => import('src/pages/nl2sql'));
export const SignInPage = lazy(() => import('src/pages/sign-in'));
export const SignUpPage = lazy(() => import('src/pages/sign-up'));
export const ProductsPage = lazy(() => import('src/pages/products'));
export const Page404 = lazy(() => import('src/pages/page-not-found'));

const renderFallback = () => (
  <Box
    sx={{
      display: 'flex',
      flex: '1 1 auto',
      alignItems: 'center',
      justifyContent: 'center',
    }}
  >
    <LinearProgress
      sx={{
        width: 1,
        maxWidth: 320,
        bgcolor: (theme) => varAlpha(theme.vars.palette.text.primaryChannel, 0.16),
        [`& .${linearProgressClasses.bar}`]: { bgcolor: 'text.primary' },
      }}
    />
  </Box>
);

export const routesSection: RouteObject[] = [
  {
    element: (
      <DashboardLayout>
        <Suspense fallback={renderFallback()}>
          <Outlet />
        </Suspense>
      </DashboardLayout>
    ),
    children: [
      { index: true, element: <DashboardPage /> },
      { path: 'user', element: <UserPage /> },
      { path: 'system', element: <SystemPage /> },
      { path: 'system/db', element: <SystemDbPage /> },
      { path: 'system/jvm', element: <SystemJvmPage /> },
      { path: 'nl2sql', element: <Nl2SqlPage /> },
      { path: 'products', element: <ProductsPage /> },
    ],
  },
  {
    path: 'sign-in',
    element: (
      <AuthLayout>
        <SignInPage />
      </AuthLayout>
    ),
  },
  {
    path: 'sign-up',
    element: (
      <AuthLayout>
        <SignUpPage />
      </AuthLayout>
    ),
  },
  {
    path: '404',
    element: <Page404 />,
  },
  { path: '*', element: <Page404 /> },
];
