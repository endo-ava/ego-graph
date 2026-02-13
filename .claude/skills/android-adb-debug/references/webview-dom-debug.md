# WebView DOM Debug (ADB + DevTools)

Android WebView 上で動く `terminal.html` の DOM/CSS 状態を、ADB 経由で直接確認する手順。

## 目的

- 「画面に何が見えているか」を推測ではなく DOM 実測で確認する
- `xterm` の描画崩れ・カーソル位置ずれ・要素重複を切り分ける
- WebSocket でデータが来ていないのか、描画だけ壊れているのかを判定する

## 前提

```bash
DEVICE="100.x.x.x:5559"
```

## 1. WebView DevTools ソケットを検出

```bash
adb -s "$DEVICE" shell cat /proc/net/unix | rg webview_devtools_remote
```

出力例:

```text
... @webview_devtools_remote_8294
```

## 2. ローカルにポートフォワード

```bash
adb -s "$DEVICE" forward --remove tcp:9222 >/dev/null 2>&1 || true
adb -s "$DEVICE" forward tcp:9222 localabstract:webview_devtools_remote_8294
```

## 3. デバッグ対象ページを確認

```bash
curl -sS http://127.0.0.1:9222/json/list
```

`url: "file:///android_asset/xterm/terminal.html"` の `webSocketDebuggerUrl` を使う。

## 4. Node で DOM を直接取得

### 4-1. 基本状態（行数・ステータス行・右端線・ドット）

```bash
node - <<'JS'
(async () => {
  const list = await (await fetch('http://127.0.0.1:9222/json/list')).json();
  const page = list.find(p => JSON.parse(p.description || '{}').attached) || list[0];
  const ws = new WebSocket(page.webSocketDebuggerUrl);
  let id = 1;
  const pending = new Map();
  const send = (method, params = {}) => new Promise((resolve) => {
    const rid = id++;
    pending.set(rid, resolve);
    ws.send(JSON.stringify({ id: rid, method, params }));
  });

  ws.onmessage = (event) => {
    const msg = JSON.parse(event.data);
    if (msg.id && pending.has(msg.id)) {
      pending.get(msg.id)(msg);
      pending.delete(msg.id);
    }
  };

  ws.onopen = async () => {
    const out = await send('Runtime.evaluate', {
      expression: `(() => {
        const rows = document.querySelector('.xterm-rows');
        if (!rows) return { missing: true };
        const lines = [...rows.children].map((el, i) => ({ i, t: el.innerText }));
        return {
          rows: lines.length,
          hasStatus: lines.some(x => x.t.includes('[agent-0001:')),
          rightBars: lines.filter(x => x.t.endsWith('│')).length,
          dotLines: lines.filter(x => x.t.includes('······')).length,
          sample: lines.slice(0, 12),
        };
      })()`,
      returnByValue: true,
    });
    console.log(JSON.stringify(out.result.result.value, null, 2));
    ws.close();
    setTimeout(() => process.exit(0), 80);
  };
})();
JS
```

### 4-2. カーソル座標確認

```bash
node - <<'JS'
(async () => {
  const list = await (await fetch('http://127.0.0.1:9222/json/list')).json();
  const page = list.find(p => JSON.parse(p.description || '{}').attached) || list[0];
  const ws = new WebSocket(page.webSocketDebuggerUrl);
  let id = 1;
  const pending = new Map();
  const send = (method, params = {}) => new Promise((resolve) => {
    const rid = id++;
    pending.set(rid, resolve);
    ws.send(JSON.stringify({ id: rid, method, params }));
  });
  ws.onmessage = (event) => {
    const msg = JSON.parse(event.data);
    if (msg.id && pending.has(msg.id)) {
      pending.get(msg.id)(msg);
      pending.delete(msg.id);
    }
  };
  ws.onopen = async () => {
    const out = await send('Runtime.evaluate', {
      expression: `(() => {
        const c = document.querySelector('.xterm-cursor');
        if (!c) return { cursor: null };
        const r = c.getBoundingClientRect();
        return {
          cursor: { x: r.x, y: r.y, w: r.width, h: r.height },
          line: c.parentElement ? c.parentElement.innerText : null,
        };
      })()`,
      returnByValue: true,
    });
    console.log(JSON.stringify(out.result.result.value, null, 2));
    ws.close();
    setTimeout(() => process.exit(0), 80);
  };
})();
JS
```

## 5. 画面実体の確認（スクショ）

```bash
adb -s "$DEVICE" exec-out screencap -p > /tmp/terminal_screen.png
```

DOM結果とスクリーンショットを必ずセットで見ること。

## よくある詰まりどころ

- `fetch failed` / `Empty reply from server`
  - WebView プロセスが再起動してソケット名が変わっている
  - 手順1からやり直す
- `json/list` に `terminal.html` が出ない
  - 画面が未表示、または別画面に遷移済み
- `hasStatus: true`
  - tmux の status 行が表示されている状態
- `rightBars > 0` / `dotLines > 0`
  - サイズ同期ずれ、または描画状態破綻の兆候

## 運用メモ

- この手法は「WebView の実DOM」を見るため、`uiautomator` では見えない情報を取得できる
- 端末崩れ調査は `tmux capture-pane` と本リファレンスのDOM調査を併用する
