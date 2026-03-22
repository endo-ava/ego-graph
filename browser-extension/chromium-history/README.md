# Chromium History Extension

Chromium MV3 extension for syncing Edge / Brave / Chrome history to the EgoGraph backend.

## Setup

1. `npm install`
2. `npm run build`
3. Load `dist/` as an unpacked extension
4. Open the options page and set:
   - `server_url`
   - `x_api_key`
   - `browser_id`
   - `device_id`
   - `profile`

## Behavior

- Sync runs on browser startup
- `Sync now` uses the same background sync path
- Cursor advances only after a successful `200 OK` response
