"""Browser history ingest API endpoint."""

import logging

from fastapi import APIRouter, Body, Depends, HTTPException
from pydantic import ValidationError

from backend.api.schemas.browser_history import (
    BrowserHistoryIngestRequest,
    BrowserHistoryIngestResponse,
)
from backend.config import BackendConfig
from backend.dependencies import get_config, verify_api_key
from backend.usecases.browser_history import (
    BrowserHistoryUseCaseError,
    ingest_browser_history,
)
from ingest.browser_history.schema import BrowserHistoryPayload

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/v1/ingest/browser-history",
    tags=["ingest", "browser_history"],
)


@router.post("", response_model=BrowserHistoryIngestResponse)
async def ingest_browser_history_endpoint(
    request: dict = Body(...),
    config: BackendConfig = Depends(get_config),
    _: None = Depends(verify_api_key),
):
    """Browser history payload を受信して保存する。"""
    try:
        validated_request = BrowserHistoryIngestRequest.model_validate(request)
        payload = BrowserHistoryPayload.model_validate(
            validated_request.model_dump(mode="python")
        )
        result = ingest_browser_history(payload, config.r2)
        return BrowserHistoryIngestResponse(
            sync_id=result.sync_id,
            accepted=result.accepted,
            raw_saved=result.raw_saved,
            events_saved=result.events_saved,
            received_at=result.received_at,
        )
    except ValidationError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except BrowserHistoryUseCaseError as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc
