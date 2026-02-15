import json
from pathlib import Path


class FewShotJsonStore:
    def __init__(self, file_path: str | Path):
        self.file_path = Path(file_path)

    def load(self) -> dict:
        if not self.file_path.exists():
            return {"version": "1.0.0", "last_updated": "", "examples": []}

        with open(self.file_path, encoding="utf-8") as f:
            return json.load(f)

    def generate_next_id(self) -> str:
        data = self.load()
        examples = data.get("examples", [])
        if not examples:
            return "example_001"

        last_id = examples[-1]["example_id"]
        num = int(last_id.split("_")[1]) + 1
        return f"example_{num:03d}"

    def append_example(
        self,
        example_id: str,
        query: str,
        sql: str,
        explanation: str,
        tables_used: list[str],
    ) -> None:
        data = self.load()
        new_example = {
            "example_id": example_id,
            "query": query,
            "sql": sql,
            "explanation": explanation,
            "tables_used": tables_used,
            "tags": ["사용자 피드백"],
        }
        data["examples"].append(new_example)

        with open(self.file_path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=4)
