from pathlib import Path

import config


def test_relative_path_is_resolved_from_project_root_regardless_of_cwd(tmp_path, monkeypatch):
    monkeypatch.chdir(tmp_path)
    assert config.resolve_path("chroma_db") == (config.PROJECT_ROOT / "chroma_db").resolve()


def test_absolute_path_is_kept(tmp_path):
    assert config.resolve_path(tmp_path / "db") == (tmp_path / "db").resolve()


def test_project_root_contains_batch_dir():
    assert (config.PROJECT_ROOT / "batch" / "config.py").is_file()


def test_settings_defaults_when_env_missing(monkeypatch):
    monkeypatch.setattr(config, "ENV_FILE", Path("does-not-exist.env"))
    for key in ("OPENAI_API_KEY", "OPENAI_EMBEDDING_MODEL", "CHROMA_DIR", "CHROMA_COLLECTION"):
        monkeypatch.delenv(key, raising=False)
    settings = config.load_settings()
    assert settings.openai_api_key is None
    assert settings.embedding_model == "text-embedding-3-small"
    assert settings.chroma_dir == "chroma_db"
    assert settings.collection == "seoul_plan_2040"
