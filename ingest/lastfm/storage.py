"""Last.fm データの R2 ストレージハンドラ。"""

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


class LastFmStorage:
    """Last.fm データの保存・管理クラス (R2 対応)。"""

    def __init__(
        self,
        endpoint_url: str,
        access_key_id: str,
        secret_access_key: str,
        bucket_name: str,
        raw_path: str = "raw/",
        events_path: str = "events/",
        master_path: str = "master/",
    ):
        """ストレージを初期化します。"""
        self.bucket_name = bucket_name
        self.raw_path = raw_path.rstrip("/") + "/"
        self.events_path = events_path.rstrip("/") + "/"
        self.master_path = master_path.rstrip("/") + "/"

        self.s3 = boto3.client(
            "s3",
            endpoint_url=endpoint_url,
            aws_access_key_id=access_key_id,
            aws_secret_access_key=secret_access_key,
            region_name="auto",
        )
        logger.info(f"LastFmStorage initialized for bucket: {bucket_name}")

    def save_parquet(
        self,
        data: list[dict[str, Any]],
        year: int,
        month: int,
        prefix: str = "lastfm/tracks",
    ) -> str | None:
        """データを Parquet 形式で R2 に保存します。"""
        if not data:
            return None

        unique_id = str(uuid.uuid4())
        key = (
            f"{self.master_path}{prefix}/"
            f"year={year}/month={month:02d}/{unique_id}.parquet"
        )

        try:
            df = pd.DataFrame(data)
            # フェッチ時刻を追加
            if "fetched_at" not in df.columns:
                df["fetched_at"] = datetime.now(timezone.utc)

            buffer = BytesIO()
            df.to_parquet(buffer, index=False, engine="pyarrow")
            buffer.seek(0)

            self.s3.put_object(
                Bucket=self.bucket_name,
                Key=key,
                Body=buffer.getvalue(),
                ContentType="application/octet-stream",
            )
            logger.info(f"Saved Last.fm Parquet to {key}")
            return key
        except Exception:
            logger.exception(f"Failed to save Parquet to {key}")
            return None

    def get_ingest_state(
        self, key: str = "state/lastfm_ingest_state.json"
    ) -> dict[str, Any] | None:
        """インジェスト状態を取得します。"""
        try:
            response = self.s3.get_object(Bucket=self.bucket_name, Key=key)
            return json.loads(response["Body"].read().decode("utf-8"))
        except ClientError as e:
            if e.response["Error"]["Code"] == "NoSuchKey":
                return None
            logger.exception("Failed to get ingest state")
            return None

    def save_ingest_state(
        self, state: dict[str, Any], key: str = "state/lastfm_ingest_state.json"
    ):
        """インジェスト状態を保存します。"""
        try:
            self.s3.put_object(
                Bucket=self.bucket_name,
                Key=key,
                Body=json.dumps(state),
                ContentType="application/json",
            )
        except Exception:
            logger.exception("Failed to save ingest state")

    def list_parquet_files(self, prefix: str) -> list[str]:
        """指定されたプレフィックス配下の Parquet ファイル一覧を取得します。"""
        keys = []
        paginator = self.s3.get_paginator("list_objects_v2")
        for page in paginator.paginate(
            Bucket=self.bucket_name, Prefix=f"{self.master_path}{prefix}"
        ):
            if "Contents" in page:
                for obj in page["Contents"]:
                    if obj["Key"].endswith(".parquet"):
                        keys.append(obj["Key"])
        return keys
