# Firebase / FCM 通知アーキテクチャ

## Status

このドキュメントにある Gateway 側の通知 runtime は、2026-03 時点で **Plexus** 側へ移管済みです。
EgoGraph repo では FCM runtime を保持しません。

## Current Boundary

- `frontend/androidApp/google-services.json`:
  - Android クライアント側の設定として引き続き EgoGraph repo に存在しうる
- FCM token registration / webhook delivery / admin credentials:
  - runtime 側の責務として Plexus が所有する

## Note

- `gateway/firebase-service-account.json` を前提にした運用は EgoGraph では廃止
- このファイルは移管前の責務整理メモとしてのみ残す
