"""Qdrant Cloudストレージ統合。

Qdrant Cloudを使用したベクトルの保存と検索を処理します。
"""

import logging
from typing import Any, Dict, List
from uuid import uuid4

from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams, PointStruct
from llama_index.core.schema import TextNode

from egograph.utils import batch_items

logger = logging.getLogger(__name__)


class QdrantStorage:
    """Qdrant Cloudストレージクライアント。

    Qdrant Cloudでのベクトル保存（コレクション作成、ベクトルのアップサート、クエリ）を管理します。
    """

    def __init__(
        self,
        url: str,
        api_key: str,
        collection_name: str = "egograph_spotify",
        vector_size: int = 768,
        batch_size: int = 1000,
    ):
        """Qdrantストレージを初期化します。

        Args:
            url: QdrantクラスターのURL
            api_key: Qdrant APIキー
            collection_name: コレクション名
            vector_size: ベクトルの次元数（Nomicは768）
            batch_size: バッチごとのアップサート数
        """
        self.collection_name = collection_name
        self.vector_size = vector_size
        self.batch_size = batch_size

        # Qdrantクライアントの初期化
        self.client = QdrantClient(url=url, api_key=api_key)

        logger.info(
            f"Qdrant storage initialized (collection={collection_name}, "
            f"vector_size={vector_size})"
        )

        # コレクションの存在確認
        self._ensure_collection()

    def _ensure_collection(self) -> None:
        """コレクションが存在することを確認し、なければ作成します。"""
        try:
            # コレクションが存在するか確認
            collections = self.client.get_collections()
            collection_names = [c.name for c in collections.collections]

            if self.collection_name in collection_names:
                logger.info(f"Collection '{self.collection_name}' already exists")
            else:
                # コレクションを作成
                self.client.create_collection(
                    collection_name=self.collection_name,
                    vectors_config=VectorParams(
                        size=self.vector_size,
                        distance=Distance.COSINE,
                    ),
                )
                logger.info(f"Created collection '{self.collection_name}'")

        except Exception as e:
            logger.error(f"Failed to ensure collection exists: {e}")
            raise

    def upsert_vectors(
        self,
        nodes: List[TextNode],
        embeddings: List[List[float]],
        show_progress: bool = True,
    ) -> int:
        """メタデータ付きベクトルをQdrantにアップサートします。

        Args:
            nodes: メタデータを含むテキストノードのリスト
            embeddings: 埋め込みベクトルのリスト（ノードと同じ順序）
            show_progress: 進捗をログ出力するかどうか

        Returns:
            正常にアップサートされたベクトルの数

        Raises:
            ValueError: ノードと埋め込みの長さが異なる場合
        """
        if len(nodes) != len(embeddings):
            raise ValueError(
                f"Nodes and embeddings must have same length "
                f"(got {len(nodes)} nodes, {len(embeddings)} embeddings)"
            )

        if not nodes:
            logger.warning("No vectors to upsert")
            return 0

        logger.info(f"Upserting {len(nodes)} vectors to Qdrant")

        # ポイントの作成
        points = []
        for node, embedding in zip(nodes, embeddings):
            # 埋め込みが無効（すべてゼロ）の場合はスキップ
            if all(v == 0.0 for v in embedding):
                logger.warning(f"Skipping node with zero embedding: {node.node_id}")
                continue

            point = PointStruct(
                id=str(uuid4()),  # Qdrant用に一意なIDを生成
                vector=embedding,
                payload={
                    "text": node.text,
                    "node_id": node.node_id,
                    "metadata": node.metadata,
                },
            )
            points.append(point)

        # バッチアップサート
        batches = batch_items(points, self.batch_size)
        total_upserted = 0

        for i, batch in enumerate(batches, 1):
            if show_progress and len(batches) > 1:
                logger.info(f"Upserting batch {i}/{len(batches)}")

            try:
                self.client.upsert(
                    collection_name=self.collection_name,
                    points=batch,
                )
                total_upserted += len(batch)
            except Exception as e:
                logger.error(f"Failed to upsert batch {i}: {e}")

        logger.info(
            f"Successfully upserted {total_upserted}/{len(points)} vectors"
        )

        return total_upserted

    def recreate_collection(self) -> None:
        """コレクションを再作成します（既存データは削除されます）。
        
        Embeddingモデルの変更や次元数の変更時に使用します。
        既存のコレクションがある場合は削除し、現在の設定（vector_size）で新規作成します。
        """
        try:
            # 既存コレクションの削除を試みる
            collections = self.client.get_collections()
            collection_names = [c.name for c in collections.collections]
            
            if self.collection_name in collection_names:
                logger.warning(f"Deleting existing collection '{self.collection_name}' for recreation...")
                self.client.delete_collection(self.collection_name)
            
            # コレクションの新規作成
            logger.info(f"Creating new collection '{self.collection_name}' with size={self.vector_size}")
            self.client.create_collection(
                collection_name=self.collection_name,
                vectors_config=VectorParams(
                    size=self.vector_size,
                    distance=Distance.COSINE,
                ),
            )
            logger.info("Collection recreated successfully.")
            
        except Exception as e:
            logger.error(f"Failed to recreate collection: {e}")
            raise

    def get_collection_info(self) -> Dict[str, Any]:
        """コレクションに関する情報を取得します。

        Returns:
            コレクション情報を含む辞書
        """
        try:
            info = self.client.get_collection(self.collection_name)
            return {
                "name": self.collection_name,  # info.name might be missing in some clients
                "vectors_count": info.vectors_count,
                "points_count": info.points_count,
                "status": str(info.status),
            }
        except Exception as e:
            logger.error(f"Failed to get collection info: {e}")
            return {}

    def delete_collection(self) -> None:
        """コレクションを削除します。

        警告: これによりコレクション内の全てのデータが削除されます！
        """
        try:
            self.client.delete_collection(self.collection_name)
            logger.info(f"Deleted collection '{self.collection_name}'")
        except Exception as e:
            logger.error(f"Failed to delete collection: {e}")
            raise
