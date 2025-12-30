# Last.fm データソース設計

Deprecated: Last.fm 連携はタグ取得率が低いため一時停止中。

## 1. 概要

Spotify の再生履歴に含まれる楽曲・アーティスト情報を **Last.fm API** と照合し、付加的なメタデータを補充する（エンリッチメント）。これにより、Spotify API だけでは取得しにくい「ジャンルタグ」や「グローバルな再生数」を利用した分析を可能にする。

- **Cloudflare R2**: 取得したメタデータを Parquet 形式で保存。
- **DuckDB**: R2 上の Parquet を読み取り、Spotify の履歴とジョインして分析に利用。

---

## 2. データ構造 (Input)

### 2.1 取得する情報
`pylast` ライブラリを介して Last.fm API から以下を取得：

#### Track Metadata
- `track_name`, `artist_name` (名寄せ後の正規化された名称)
- `album_name`
- `playcount` (Last.fm 全ユーザーの総再生数)
- `listeners` (総リスナー数)
- `duration_ms`
- `tags` (上位のジャンルタグ・属性タグのリスト)
- `url`, `mbid` (MusicBrainz ID)

#### Artist Metadata
- `artist_name`
- `playcount`, `listeners`
- `tags`
- `bio_summary` (アーティストの紹介文の要約)
- `url`, `mbid`

---

## 3. Storage Design (R2 / DuckDB)

### 3.1 Cloudflare R2 への保存レイアウト
増分取得および月次パーティショニングを行う。

- **Tracks**: `master/lastfm/tracks/year={yyyy}/month={mm}/{uuid}.parquet`
- **Artists**: `master/lastfm/artists/year={yyyy}/month={mm}/{uuid}.parquet`

### 3.2 DuckDB での参照 (Mart 層)
`read_parquet()` を使用して R2 上のデータをビューとして定義する。
※ `mart` は論理的なビューであり、R2 上の `mart/` フォルダを指すものではない。

**スキーマ例 (`mart.lastfm_tracks`)**:
| Column | Type | 備考 |
|---|---|---|
| `track_name` | VARCHAR | |
| `artist_name` | VARCHAR | |
| `playcount` | BIGINT | |
| `tags` | VARCHAR[] | 音楽ジャンル等のタグ配列 |
| `fetched_at` | TIMESTAMP | 取得日時 |

---

## 4. エンリッチメント・プロセス (GitHub Actions)

1.  **Extract**: Spotify の再生履歴 (`mart.spotify_plays`) から、まだ Last.fm メタデータを取得していないユニークな (楽曲, アーティスト) ペアを抽出。
2.  **Transform & Fetch**: 
    - `LastFmCollector` を使用して API を呼び出し。
    - 直接ヒットしない場合は検索 API によるフォールバックを実行。
3.  **Load**: 取得結果を Parquet に変換し、`LastFmStorage` 経由で R2 に保存。

---

## 5. 分析ユースケース

- **タグベースの集計**: 
  - 「今週はどのジャンルの曲を一番聴いたか？」
  - `mart.spotify_plays` と `mart.lastfm_tracks` をジョインし、`tags` を展開（UNNEST）して集計。
- **アーティストの深掘り**:
  - LLM が再生傾向を要約する際、`bio_summary` や `tags` をコンテキストとして注入。

---

## 6. 考慮事項

- **API レート制限**: `pylast` のレート制限を有効化し、負荷を調整。
- **再試行ロジック**: `tenacity` を使用し、ネットワークエラー等に対する指数バックオフ再試行を実装。
- **名寄せ (Normalization)**: Last.fm 側で名称が修正される場合があるため、取得後の `track_name`, `artist_name` を正文として扱う。
