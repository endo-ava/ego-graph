"""SpotifyデータのR2への保存を担当する。"""

import json
import logging
import uuid
from datetime import datetime, timezone
from io import BytesIO
from typing import Any

import boto3
import pandas as pd
from botocore.exceptions import ClientError

logger = logging.getLogger(__name__)


class SpotifyStorage:
    """Spotifyデータの保存・永続化を管理するクラス。"""

    def __init__(
        self,
        endpoint_url: str,
        access_key_id: str,
        secret_access_key: str,
        bucket_name: str,
        raw_path: str = "raw/",
        events_path: str = "events/",
    ):
        """Storageを初期化する。

        Args:
            endpoint_url: R2エンドポイント
            access_key_id: アクセスキーID
            secret_access_key: シークレットアクセスキー
            bucket_name: バケット名
            raw_path: 生データの保存先プレフィックス
            events_path: イベントデータの保存先プレフィックス
        """
        self.bucket_name = bucket_name
        self.raw_path = raw_path.rstrip("/") + "/"
        self.events_path = events_path.rstrip("/") + "/"

        # S3クライアントの初期化
        self.s3 = boto3.client(
            "s3",
            endpoint_url=endpoint_url,
            aws_access_key_id=access_key_id,
            aws_secret_access_key=secret_access_key,
            region_name="auto",
        )
        logger.info(f"Storage initialized for bucket: {bucket_name}")

    def save_raw_json(
        self, data: list[dict[str, Any]] | dict[str, Any], prefix: str = "spotify"
    ) -> str | None:
        """生データ(JSON)をR2に保存する。

        Path format: raw/{prefix}/YYYY/MM/DD/{timestamp}_{uuid}.json

        Args:
            data: 保存するデータ(リストまたは辞書)
            prefix: データカテゴリー(例: "spotify/recently_played")

        Returns:
            保存されたオブジェクトのキー (失敗時はNone)
        """
        now = datetime.now(timezone.utc)
        timestamp_str = now.strftime("%Y%m%d_%H%M%S")
        unique_id = str(uuid.uuid4())[:8]

        # パスの構築
        date_path = now.strftime("%Y/%m/%d")
        file_name = f"{timestamp_str}_{unique_id}.json"
        key = f"{self.raw_path}{prefix}/{date_path}/{file_name}"

        try:
            json_bytes = json.dumps(data, ensure_ascii=False).encode("utf-8")
            self.s3.put_object(
                Bucket=self.bucket_name,
                Key=key,
                Body=json_bytes,
                ContentType="application/json",
            )
            logger.info(f"Saved raw JSON to {key}")
            return key
        except ClientError:
            logger.exception("Failed to save raw JSON")
            return None

    def save_parquet(
        self,
        data: list[dict[str, Any]],
        year: int,
        month: int,
        prefix: str = "spotify/plays",
    ) -> str | None:
        """データをParquet形式で保存する。

        Path format: events/{prefix}/year={YYYY}/month={MM}/{uuid}.parquet

        Args:
            data: 保存するデータ(辞書のリスト)
            year: パーティション年
            month: パーティション月
            prefix: イベントカテゴリー

        Returns:
            保存されたオブジェクトのキー (失敗時はNone)
        """
        if not data:
            logger.warning("No data provided for Parquet save.")
            return None

        unique_id = str(uuid.uuid4())
        key = (
            f"{self.events_path}{prefix}/"
            f"year={year}/month={month:02d}/{unique_id}.parquet"
        )

        try:
            # Pandas DataFrameに変換
            df = pd.DataFrame(data)

            # DataFrameをParquetバイナリとしてメモリ書き出し
            buffer = BytesIO()
            df.to_parquet(buffer, index=False, engine="pyarrow")
            buffer.seek(0)

            self.s3.put_object(
                Bucket=self.bucket_name,
                Key=key,
                Body=buffer.getvalue(),
                ContentType="application/octet-stream",
            )
            logger.info(f"Saved Parquet to {key}")
            return key
        except ImportError:
            logger.exception("Pandas or PyArrow is required for Parquet saving")
            return None
        except Exception:
            logger.exception("Failed to save Parquet")
            return None

    def get_ingest_state(
        self, key: str = "state/ingest_state.json"
    ) -> dict[str, Any] | None:
        """インジェスト状態(カーソル)を取得する。

        Args:
            key: 状態ファイルのキー

        Returns:
            状態辞書 (存在しない場合はNone)
        """
        try:
            response = self.s3.get_object(Bucket=self.bucket_name, Key=key)
            return json.loads(response["Body"].read().decode("utf-8"))
        except ClientError as e:
            if e.response["Error"]["Code"] == "NoSuchKey":
                logger.info("No ingest state found.")
                return None
            logger.exception("Failed to get ingest state")
            return None
        except Exception:
            logger.exception("Failed to read ingest state")
            return None

    def save_ingest_state(
        self, state: dict[str, Any], key: str = "state/ingest_state.json"
    ):
        """インジェスト状態(カーソル)を保存する。

        Args:
            state: 保存する状態辞書
            key: 状態ファイルのキー
        """
        try:
            self.s3.put_object(
                Bucket=self.bucket_name,
                Key=key,
                Body=json.dumps(state),
                ContentType="application/json",
            )
            logger.info(f"Saved ingest state to {key}")
        except Exception:
            logger.exception("Failed to save ingest state")
