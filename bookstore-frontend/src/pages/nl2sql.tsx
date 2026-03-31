import { CONFIG } from 'src/config-global';

import { Nl2SqlChatView } from 'src/sections/nl2sql/view';

// ----------------------------------------------------------------------

export default function Page() {
  return (
    <>
      <title>{`NL2SQL - ${CONFIG.appName}`}</title>

      <Nl2SqlChatView />
    </>
  );
}
