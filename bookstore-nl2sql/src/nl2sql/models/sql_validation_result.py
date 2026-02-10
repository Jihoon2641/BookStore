from typing import Dict
from typing import Optional, List


class SqlValidationResult:
    def __init__(
        self,
        is_valid: bool,
        error_message: Optional[str] = None,
        parsed_sql: Optional[str] = None,
        suggestion: Optional[List[str]] = None,
    ):
        self.is_valid = is_valid
        self.error_message = error_message
        self.parsed_sql = parsed_sql
        self.suggestion = suggestion

    def to_dict(self) -> Dict:
        return {
            "isValid": self.is_valid,
            "errorMessage": self.error_message,
            "parsedSql": self.parsed_sql,
            "suggestion": self.suggestion,
        }
