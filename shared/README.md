# EgoGraph Shared Package

Common data models, configuration, and utilities shared across all EgoGraph services.

## Installation

Development mode (for working on the package):
```bash
pip install -e .
```

With dev dependencies:
```bash
pip install -e ".[dev]"
```

## Usage

```python
from shared.models import UnifiedDataModel, DataSource, DataType
from shared.config import Config, SpotifyConfig
from shared.utils import batch_items

# Load configuration (service側で設定を組み立てる)
config = Config(
    spotify=SpotifyConfig(
        client_id="client-id",
        client_secret="secret",
        refresh_token="refresh",
    )
)

# Create unified data model
model = UnifiedDataModel(
    source=DataSource.SPOTIFY,
    type=DataType.MUSIC,
    timestamp=datetime.utcnow(),
    raw_text="Sample track",
)
```

## Testing

```bash
pytest
```
