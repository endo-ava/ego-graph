"""埋め込み用のローカルモデル統合。

Hugging Faceの `sentence-transformers` 対応モデルを使用してベクトル埋め込みを生成します。
これはローカルで実行されるため、APIキーは不要で、GitHub Actions上でも動作します。
推奨モデル:
- `cl-nagoya/ruri-v3-310m` (日本語SOTA)
- `intfloat/multilingual-e5-small` (軽量多言語)
"""

import logging
from typing import List, Optional

from sentence_transformers import SentenceTransformer
import torch

from shared.utils import batch_items

logger = logging.getLogger(__name__)


class LocalEmbedder:
    """埋め込み生成用のローカルモデルクライアント。

    任意のHugging Faceモデルを使用して埋め込みを生成します。
    デフォルトは `cl-nagoya/ruri-v3-310m` です。
    """

    def __init__(
        self,
        model_name: str = "cl-nagoya/ruri-v3-310m",
        batch_size: int = 32,
        device: Optional[str] = None,
        expected_dimension: Optional[int] = None,
    ):
        """Embedderを初期化します。

        Args:
            model_name: Hugging Faceのモデル名 (default: cl-nagoya/ruri-v3-310m)
            batch_size: 埋め込みの一括処理数。CPUでは小さめが推奨されます。
            device: 実行デバイス ('cpu', 'cuda', 'mps' 等)。Noneの場合は自動検出。
            expected_dimension: 想定する次元数。指定した場合、ロード後に整合性をチェックします。
        """
        self.model_name = model_name
        self.batch_size = batch_size
        
        if device is None:
            if torch.cuda.is_available():
                self.device = "cuda"
            elif torch.backends.mps.is_available():
                self.device = "mps"
            else:
                self.device = "cpu"
        else:
            self.device = device

        logger.info(f"Loading local embedding model: {model_name} on {self.device}...")
        
        # モデルのロード (初回はダウンロードが発生します)
        try:
            self.model = SentenceTransformer(model_name, device=self.device)
            actual_dim = self.get_embedding_dimension()
            
            logger.info(f"Model loaded successfully. Embedding dimension: {actual_dim}")
            
            if expected_dimension and actual_dim != expected_dimension:
                logger.warning(
                    f"Dimension mismatch! Expected {expected_dimension}, but model {model_name} produces {actual_dim}. "
                    "Re-indexing may be required."
                )
                
        except Exception:
            logger.exception(f"Failed to load model {model_name}")
            raise

    def _embed_batch(
        self,
        texts: List[str],
    ) -> List[List[float]]:
        """テキストのバッチを埋め込みます。

        Args:
            texts: 埋め込むテキストのリスト

        Returns:
            埋め込みベクトルのリスト
        """
        # sentence-transformers は内部でバッチ処理を行えますが、
        # ここでは明示的に呼び出す形に合わせています。
        embeddings = self.model.encode(
            texts, 
            batch_size=self.batch_size, 
            show_progress_bar=False, 
            convert_to_numpy=True,
            normalize_embeddings=True # 正規化されたベクトルを返す
        )
        return embeddings.tolist()

    def embed_texts(
        self,
        texts: List[str],
        show_progress: bool = True,
    ) -> List[List[float]]:
        """複数のテキストを埋め込みます。

        Args:
            texts: 埋め込むテキストのリスト
            show_progress: 進捗をログ出力するかどうか

        Returns:
            埋め込みベクトルのリスト(入力と同じ順序)
        """
        if not texts:
            logger.warning("No texts to embed")
            return []

        logger.info(f"Embedding {len(texts)} texts with {self.model_name} (batch_size={self.batch_size})")

        all_embeddings = []
        batches = batch_items(texts, self.batch_size)

        for i, batch in enumerate(batches, 1):
            if show_progress and len(batches) > 1:
                logger.debug(f"Processing batch {i}/{len(batches)}")

            try:
                embeddings = self._embed_batch(batch)
                all_embeddings.extend(embeddings)
            except Exception:
                logger.exception(f"Failed to embed batch {i}")
                # 失敗時は例外を投げるか、空を埋めるか。ローカル実行なので基本は例外で落とす方が安全。
                raise

        logger.info(
            f"Successfully embedded {len(all_embeddings)}/{len(texts)} texts"
        )

        return all_embeddings

    def get_embedding_dimension(self) -> int:
        """このモデルによって生成される埋め込みの次元を取得します。

        Returns:
            埋め込み次元
        """
        return self.model.get_sentence_embedding_dimension()
