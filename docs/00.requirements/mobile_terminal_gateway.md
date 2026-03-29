# 要件定義: モバイル端末からの LXC tmux 接続（Terminal Gateway）

## Status

この要件定義は **EgoGraph repo では現在の実装対象ではない**。
terminal/runtime 機能は Plexus 側へ移管済みのため、本ドキュメントは移管前の要件メモとして保持する。

## Current Ownership

- terminal session access
- WebSocket terminal transport
- push / webhook notification runtime
- 将来の orchestration runtime

上記は 2026-03 時点で **Plexus** が所有する。

## Note

- `frontend/**` 側の terminal UI はこの repo に残っているが、本ドキュメントのサーバ側責務は対象外
- EgoGraph で terminal/runtime を再実装する前提では読まないこと
