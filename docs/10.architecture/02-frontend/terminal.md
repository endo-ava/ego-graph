# Terminal 機能設計

このファイルは frontend 側に残っている terminal UI の設計メモである。
ただし、対応する terminal/runtime 実装は 2026-03 時点で **Plexus** 側へ移管済み。

## 現在の扱い

- `frontend/**` の terminal UI 実装はこの repo に残っている
- 接続先 runtime の詳細設計、API 契約、運用手順は Plexus 側で管理する
- このドキュメントをもとに EgoGraph 側 server runtime を追加する前提では使わない
