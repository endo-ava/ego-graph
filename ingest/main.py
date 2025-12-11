"""EgoGraph Spotify MVPのメインエントリーポイント。

完全なデータパイプラインを調整します:
1. Spotify APIからデータを収集
2. 統一スキーマへの変換
3. LlamaIndex ETLによる処理
4. ローカル埋め込みモデルによる埋め込み生成
5. Qdrant Cloudへの保存
"""

import logging
import sys
from datetime import datetime, timezone

from shared.config import Config
from shared.utils import log_execution_time
from ingest.spotify.collector import SpotifyCollector
from ingest.spotify.transformer import SpotifyTransformer
from ingest.pipeline.etl import ETLPipeline
from ingest.pipeline.embeddings import LocalEmbedder
from ingest.pipeline.storage import QdrantStorage

logger = logging.getLogger(__name__)


@log_execution_time
def main():
    """メインパイプラインの実行。"""
    logger.info("=" * 60)
    logger.info("EgoGraph Spotify MVP Pipeline")
    logger.info(f"Started at: {datetime.now(timezone.utc).isoformat()}")
    logger.info("=" * 60)

    try:
        # 設定のロード
        logger.info("Loading configuration...")
        config = Config.from_env()
        config.validate_all()

        # Step 1: Spotifyからデータを収集
        logger.info("\n[Step 1/5] Collecting data from Spotify API...")
        collector = SpotifyCollector(
            client_id=config.spotify.client_id,
            client_secret=config.spotify.client_secret,
            refresh_token=config.spotify.refresh_token,
            redirect_uri=config.spotify.redirect_uri,
            scope=config.spotify.scope,
        )

        # 最近再生したトラックを取得
        logger.info("Fetching recently played tracks...")
        recently_played = collector.get_recently_played()
        logger.info(f"✓ Collected {len(recently_played)} recently played tracks")

        # トラック付きプレイリストを取得
        logger.info("Fetching user playlists...")
        playlists = collector.get_playlists_with_tracks()
        logger.info(f"✓ Collected {len(playlists)} playlists")

        # Step 2: 統一スキーマへの変換
        logger.info("\n[Step 2/5] Transforming data to unified schema...")
        transformer = SpotifyTransformer()
        unified_data = transformer.transform_all(recently_played, playlists)
        logger.info(f"✓ Transformed {len(unified_data)} items to unified schema")

        if not unified_data:
            logger.warning("No data to process. Exiting.")
            return

        # Step 3: LlamaIndex ETLで処理
        logger.info("\n[Step 3/5] Processing with LlamaIndex ETL pipeline...")
        etl = ETLPipeline(
            chunk_size=512,
            chunk_overlap=50,
        )
        nodes = etl.process_documents(unified_data)
        stats = etl.get_stats(nodes)
        logger.info(
            f"✓ Created {stats['total_nodes']} nodes "
            f"(avg length: {stats['avg_text_length']:.0f} chars)"
        )

        if not nodes:
            logger.warning("No nodes created. Exiting.")
            return

        # Step 4: ローカルモデルで埋め込みを生成
        logger.info("\n[Step 4/5] Generating embeddings with Local Model...")
        embedder = LocalEmbedder(
            model_name=config.embedding.model_name,
            batch_size=config.embedding.batch_size,
            device=config.embedding.device,
            expected_dimension=config.embedding.expected_dimension,
        )

        texts = [node.text for node in nodes]
        embeddings = embedder.embed_texts(texts)
        logger.info(
            f"✓ Generated {len(embeddings)} embeddings "
            f"({embedder.get_embedding_dimension()}-dimensional)"
        )

        # Step 5: Qdrant Cloudへ保存
        logger.info("\n[Step 5/5] Storing vectors in Qdrant Cloud...")
        storage = QdrantStorage(
            url=config.qdrant.url,
            api_key=config.qdrant.api_key,
            collection_name=config.qdrant.collection_name,
            vector_size=config.qdrant.vector_size,
            batch_size=config.qdrant.batch_size,
        )

        upserted_count = storage.upsert_vectors(nodes, embeddings)
        logger.info(f"✓ Upserted {upserted_count} vectors to Qdrant")

        # コレクション情報の取得
        collection_info = storage.get_collection_info()
        logger.info(f"Collection info: {collection_info}")

        # サマリー
        logger.info("\n" + "=" * 60)
        logger.info("Pipeline completed successfully!")
        logger.info(f"Processed: {len(unified_data)} items")
        logger.info(f"Created: {len(nodes)} nodes")
        logger.info(f"Embedded: {len(embeddings)} vectors")
        logger.info(f"Stored: {upserted_count} vectors")
        logger.info(f"Completed at: {datetime.now(timezone.utc).isoformat()}")
        logger.info("=" * 60)

    except Exception as e:
        logger.error(f"\n❌ Pipeline failed: {e}", exc_info=True)
        sys.exit(1)


if __name__ == "__main__":
    main()
