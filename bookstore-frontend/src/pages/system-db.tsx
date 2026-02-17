import { CONFIG } from 'src/config-global';

import { SystemDbView } from 'src/sections/system/view';

// ----------------------------------------------------------------------

export default function Page() {
  return (
    <>
      <title>{`System DB Metrics - ${CONFIG.appName}`}</title>

      <SystemDbView />
    </>
  );
}

