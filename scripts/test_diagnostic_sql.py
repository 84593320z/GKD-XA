"""Keep diagnostic query bounds and JSON integrity under the regular CI gate."""
from pathlib import Path
import runpy
import unittest


class DiagnosticSqlTest(unittest.TestCase):
    def test_production_queries_keep_limits_and_nested_json_integrity(self):
        root = Path(__file__).resolve().parents[1]
        runpy.run_path(str(root / "gkd-app/src/test/kotlin/li/gkd/app/data/backup/diagnostic_sql_regression.py"))


if __name__ == "__main__":
    unittest.main()
