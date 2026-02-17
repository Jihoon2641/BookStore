import { CONFIG } from 'src/config-global';

import { SystemResourceView } from 'src/sections/system/view';

// ----------------------------------------------------------------------

export default function Page() {
  return (
    <>
      <title>{`System - ${CONFIG.appName}`}</title>

      <SystemResourceView />
    </>
  );
}

