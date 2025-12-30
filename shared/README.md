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
from shared.config import Config
from shared.utils import batch_items

# Load configuration
config = Config.from_env()

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
