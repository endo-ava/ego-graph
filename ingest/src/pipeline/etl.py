"""LlamaIndexを使用したETLパイプライン。

統一データモデルを処理し、埋め込みと保存の準備をします。
チャンキング、メタデータエンリッチメント、テキスト正規化を処理します。
"""

import logging
from typing import List

from llama_index.core import Document
from llama_index.core.node_parser import SentenceSplitter
from llama_index.core.schema import TextNode

from egograph.models import UnifiedDataModel

logger = logging.getLogger(__name__)


class ETLPipeline:
    """統一データモデルを処理するためのETLパイプライン。

    LlamaIndexを使用して以下を行います:
    - データをDocumentオブジェクトに変換
    - チャンクに分割（長いテキスト用）
    - メタデータのエンリッチメント
    - 埋め込み用テキストの正規化
    """

    def __init__(
        self,
        chunk_size: int = 512,
        chunk_overlap: int = 50,
    ):
        """ETLパイプラインを初期化します。

        Args:
            chunk_size: 最大トークン数
            chunk_overlap: トークン単位でのチャンクのオーバーラップ
        """
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap

        # sentence splitterの初期化
        self.splitter = SentenceSplitter(
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
        )

        logger.info(
            f"ETL pipeline initialized (chunk_size={chunk_size}, "
            f"overlap={chunk_overlap})"
        )

    def process_documents(
        self,
        unified_data: List[UnifiedDataModel]
    ) -> List[TextNode]:
        """統一データモデルをテキストノードに処理します。

        Args:
            unified_data: 統一データモデルのリスト

        Returns:
            埋め込み準備完了のテキストノードのリスト
        """
        logger.info(f"Processing {len(unified_data)} unified data models")

        # LlamaIndex Documentに変換
        documents = self._create_documents(unified_data)

        # テキストノードにパース（チャンキング含む）
        nodes = self.splitter.get_nodes_from_documents(documents)

        logger.info(
            f"Created {len(nodes)} nodes from {len(documents)} documents "
            f"(avg {len(nodes)/len(documents):.1f} nodes per document)"
        )

        return nodes

    def _create_documents(
        self,
        unified_data: List[UnifiedDataModel]
    ) -> List[Document]:
        """統一データモデルをLlamaIndex Documentに変換します。

        Args:
            unified_data: 統一データモデルのリスト

        Returns:
            LlamaIndex Documentオブジェクトのリスト
        """
        documents = []

        for data in unified_data:
            # 関連する全フィールドを持つメタデータ辞書を作成
            metadata = {
                "id": str(data.id),
                "source": data.source,
                "type": data.type,
                "timestamp": data.timestamp.isoformat(),
                "sensitivity": data.sensitivity,
                "nsfw": data.nsfw,
            }

            # ソース固有のメタデータを含める
            metadata.update(data.metadata)

            # Documentの作成
            doc = Document(
                text=data.raw_text,
                metadata=metadata,
                excluded_llm_metadata_keys=["embedding"],  # 埋め込みをLLMに送信しない
                excluded_embed_metadata_keys=[],  # 全メタデータを埋め込みに含める
            )

            documents.append(doc)

        return documents

    def get_stats(self, nodes: List[TextNode]) -> dict:
        """処理されたノードに関する統計を取得します。

        Args:
            nodes: テキストノードのリスト

        Returns:
            統計情報を含む辞書
        """
        if not nodes:
            return {
                "total_nodes": 0,
                "avg_text_length": 0,
                "total_characters": 0,
            }

        total_chars = sum(len(node.text) for node in nodes)
        avg_length = total_chars / len(nodes)

        return {
            "total_nodes": len(nodes),
            "avg_text_length": avg_length,
            "total_characters": total_chars,
        }
