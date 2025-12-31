import pytest

from ingest.lastfm_r2_main import (
    _build_candidates_query,
    validate_s3_config_value,
    validate_s3_path_component,
)


class TestValidateS3ConfigValue:
    def test_valid_alphanumeric(self):
        assert validate_s3_config_value("valid123") == "valid123"

    def test_escape_single_quotes(self):
        assert validate_s3_config_value("O'Reilly") == "O''Reilly"
        assert validate_s3_config_value("'test'") == "''test''"

    def test_complex_string(self):
        # Tests a string with various allowed characters if any, but focus on quoting
        assert validate_s3_config_value("abc'def'ghi") == "abc''def''ghi"

class TestValidateS3PathComponent:
    def test_valid_paths(self):
        valid_paths = [
            "start/middle/end",
            "bucket-name",
            "file.parquet",
            "2023/01/01",
            "my_folder"
        ]
        for path in valid_paths:
            assert validate_s3_path_component(path) == path

    def test_invalid_characters(self):
        invalid_paths = [
            "",  # Empty string
            "path with spaces",
            "path; drop table",
            "path'with'quotes",
            "path$(command)",
            "invalid!"
        ]
        for path in invalid_paths:
            with pytest.raises(ValueError, match="Invalid S3 path component"):
                validate_s3_path_component(path)

class TestQueryBuilding:
    def test_build_candidates_query(self):
        glob = "s3://bucket/events/spotify/plays/*/*/*.parquet"
        query = _build_candidates_query(glob)
        assert f"read_parquet('{glob}')" in query
        assert "SELECT DISTINCT" in query
        assert "track_name" in query
        assert "artist_name" in query
