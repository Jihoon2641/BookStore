import { CONFIG } from 'src/config-global';

import { SystemJvmView } from 'src/sections/system/view';

// ----------------------------------------------------------------------

export default function Page() {
  return (
    <>
      <title>{`System JVM Metrics - ${CONFIG.appName}`}</title>

      <SystemJvmView />
    </>
  );
}

