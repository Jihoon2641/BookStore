import { CONFIG } from 'src/config-global';

import { SignUpView } from 'src/sections/auth';

// ----------------------------------------------------------------------

export default function Page() {
  return (
    <>
      <title>{`Admin sign up - ${CONFIG.appName}`}</title>

      <SignUpView />
    </>
  );
}

