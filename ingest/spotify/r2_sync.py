"""DuckDB 永続化のための Cloudflare R2 同期。"""

import logging
from pathlib import Path
from typing import Any

import boto3
from botocore.exceptions import ClientError

logger = logging.getLogger(__name__)


class R2Sync:
    """Cloudflare R2 との DuckDB ファイル同期を管理する。"""

    def __init__(
        self,
        endpoint_url: str,
        access_key_id: str,
        secret_access_key: str,
        bucket_name: str,
        key_prefix: str = "duckdb/",
    ):
        """R2 同期を初期化する。

        Args:
            endpoint_url: R2 エンドポイント URL
            access_key_id: R2 アクセスキー ID
            secret_access_key: R2 シークレットアクセスキー
            bucket_name: R2 バケット名
            key_prefix: バケット内のキー prefix（デフォルト: "duckdb/"）
        """
        self.bucket_name = bucket_name
        self.key_prefix = key_prefix.rstrip("/") + "/"

        # R2 用の S3 互換クライアント
        self.s3 = boto3.client(
            "s3",
            endpoint_url=endpoint_url,
            aws_access_key_id=access_key_id,
            aws_secret_access_key=secret_access_key,
            region_name="auto",
        )

        logger.info(f"R2 sync initialized for bucket: {bucket_name}")

    def download_db(self, local_path: str, db_name: str = "analytics.duckdb") -> bool:
        """R2 から DuckDB ファイルをダウンロードする。

        Args:
            local_path: 保存先のローカルファイルパス
            db_name: R2 内のデータベースファイル名（デフォルト: "analytics.duckdb"）

        Returns:
            ダウンロード成功時は True、見つからない場合は False
        """
        r2_key = f"{self.key_prefix}{db_name}"

        try:
            logger.info(f"Downloading {r2_key} to {local_path}")
            Path(local_path).parent.mkdir(parents=True, exist_ok=True)

            self.s3.download_file(self.bucket_name, r2_key, local_path)
            logger.info("Download successful")
            return True

        except ClientError as e:
            if e.response["Error"]["Code"] == "404":
                logger.warning(f"DB not found in R2: {r2_key} (first run?)")
                return False
            else:
                logger.error(f"Failed to download from R2: {e}")
                raise

    def upload_db(self, local_path: str, db_name: str = "analytics.duckdb"):
        """DuckDB ファイルを R2 にアップロードする。

        Args:
            local_path: アップロードするローカルファイルパス
            db_name: R2 内のデータベースファイル名（デフォルト: "analytics.duckdb"）
        """
        r2_key = f"{self.key_prefix}{db_name}"

        logger.info(f"Uploading {local_path} to {r2_key}")

        self.s3.upload_file(
            local_path,
            self.bucket_name,
            r2_key,
            ExtraArgs={"ContentType": "application/octet-stream"},
        )

        logger.info("Upload successful")

    def get_db_metadata(
        self, db_name: str = "analytics.duckdb"
    ) -> dict[str, Any] | None:
        """保存されている DB ファイルのメタデータを取得する。

        Args:
            db_name: R2 内のデータベースファイル名（デフォルト: "analytics.duckdb"）

        Returns:
            size, last_modified 等を含む辞書、見つからない場合は None
        """
        r2_key = f"{self.key_prefix}{db_name}"

        try:
            response = self.s3.head_object(Bucket=self.bucket_name, Key=r2_key)
            return {
                "size_bytes": response["ContentLength"],
                "last_modified": response["LastModified"],
                "etag": response["ETag"],
            }
        except ClientError as e:
            if e.response["Error"]["Code"] == "404":
                return None
            raise
