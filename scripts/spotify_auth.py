# SPOTIFY_REFRESH_TOKEN を取得するためのScript
# python3 scripts/spotify_auth.py で実行

# spotify_auth.py
from spotipy.oauth2 import SpotifyOAuth

# Client IDとClient Secretを入力
CLIENT_ID = "あなたのClient ID"
CLIENT_SECRET = "あなたのClient Secret"
REDIRECT_URI = "http://127.0.0.1:8888/callback"

# 必要なスコープ
SCOPE = "user-read-recently-played playlist-read-private playlist-read-collaborative"

# OAuth認証
sp_oauth = SpotifyOAuth(
    client_id=CLIENT_ID,
    client_secret=CLIENT_SECRET,
    redirect_uri=REDIRECT_URI,
    scope=SCOPE,
)

# 認証URLを取得
auth_url = sp_oauth.get_authorize_url()
print(f"以下のURLをブラウザで開いてください:\n{auth_url}\n")

# リダイレクトされたURLを入力
response_url = input("リダイレクトされたURL全体を貼り付けてください: ")

# トークンを取得
code = sp_oauth.parse_response_code(response_url)
token_info = sp_oauth.get_access_token(code)

print(f"\nRefresh Token: {token_info['refresh_token']}")
print("\nこのRefresh Tokenを安全に保管してください！")
