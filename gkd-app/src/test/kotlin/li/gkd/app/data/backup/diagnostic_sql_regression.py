"""Run production diagnostic SQL against real SQLite; no Android/Gradle required."""
import json
from pathlib import Path
import re
import sqlite3

repo = next(p for p in Path(__file__).resolve().parents if (p / "gkd-db").is_dir())
source = repo / "gkd-db/src/commonMain/kotlin/li/gkd/db"
schemas = {
    "A11yEventLog": ("a11y_event_log", "id INTEGER, ctime INTEGER, type INTEGER, app_id TEXT, name TEXT, desc TEXT, text TEXT"),
    "ActionLog": ("action_log", "id INTEGER, ctime INTEGER, app_id TEXT, activity_id TEXT, subs_id INTEGER, subs_version INTEGER, group_key INTEGER, group_type INTEGER, rule_index INTEGER, rule_key INTEGER"),
    "ActivityLog": ("activity_log", "id INTEGER, ctime INTEGER, app_id TEXT, activity_id TEXT"),
    "Snapshot": ("snapshot", "id INTEGER, app_id TEXT, activity_id TEXT, screen_height INTEGER, screen_width INTEGER, is_landscape INTEGER, github_asset_id INTEGER"),
    "SubsItem": ("subs_item", "id INTEGER, ctime INTEGER, mtime INTEGER, enable INTEGER, enable_update INTEGER, `order` INTEGER, update_url TEXT"),
    "SubsAppConfig": ("subs_app_config", "enable INTEGER, subs_id INTEGER, app_id TEXT"),
    "SubsCategoryConfig": ("subs_category_config", "enable INTEGER, subs_id INTEGER, category_key INTEGER"),
    "SubsAppGroupConfig": ("subs_app_group_config", "subs_id INTEGER, app_id TEXT, group_key INTEGER, enable INTEGER, `exclude` TEXT"),
    "SubsGlobalGroupConfig": ("subs_global_group_config", "subs_id INTEGER, group_key INTEGER, enable INTEGER, `exclude` TEXT"),
}
secret = "SYNTHETIC_PREVIEW_SECRET"
marker = '["[omitted: diagnostic text exceeds 1024 characters]"]'
large = json.dumps([json.dumps({"apiKey": secret, "padding": "x" * (256 * 1024)})])
short = json.dumps([json.dumps({"apiKey": secret, "status": "ready"})])
for name, (table, definition) in schemas.items():
    text = (source / (name + ".kt")).read_text(encoding="utf-8")
    lines = text.splitlines()
    method = next(i for i, line in enumerate(lines) if "suspend fun queryForDiagnostics" in line)
    annotation = next(line.strip() for line in reversed(lines[:method]) if line.strip().startswith("@Query("))
    # Kotlin annotation uses the same quoting/escaping as a JSON string here.
    sql = json.loads(annotation[len("@Query("):-1])
    with sqlite3.connect(":memory:") as db:
        db.execute("CREATE TABLE " + table + " (" + definition + ")")
        fields = definition.split(",")
        values = [1 if "INTEGER" in f else large for f in fields]
        placeholders = ",".join("?" for _ in fields)
        for index in range(80):
            row = values.copy()
            row[0] = index
            db.execute("INSERT INTO " + table + " VALUES (" + placeholders + ")", row)
        params = {"limit": 500, "offset": 0, "since": 20, "until": 69}
        rows = db.execute(sql, params).fetchall()
        assert len(rows) == 50, (name, len(rows))
        for row in rows:
            for value in row:
                if isinstance(value, str):
                    assert value == marker, (name, value[:100])
                    assert secret not in value
                    assert isinstance(json.loads(value), list)
        assert db.execute(sql, {**params, "limit": -1}).fetchall() == []
        if ":offset" in sql:
            page = db.execute(sql, {**params, "limit": 10, "offset": 10}).fetchall()
            assert len(page) == 10 and page[0][0] == rows[10][0]
        if name == "Snapshot":
            assert all(20 <= r[0] <= 69 for r in rows)
        # Boundary, nested escape sequences and nulls must be returned whole, not re-encoded.
        for value in (short, "x" * 1024, None):
            for field in fields:
                if "TEXT" in field:
                    column = field.strip().split()[0]
                    db.execute("UPDATE " + table + " SET `" + column.strip("`") + "` = ?", (value,))
            for row in db.execute(sql, params):
                for index, field in enumerate(fields):
                    if "TEXT" in field:
                        assert row[index] == value, (name, index)
        print(name + ": PASS rows<=50, SQL omissions, exact short/boundary/null values, paging/range")
print("PASS: 9 production queries; escaped nested API key absent from oversized SQL output.")
