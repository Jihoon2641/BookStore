class SqlValidationResult:
    def __init__(
        self,
        is_valid: bool,
        error_message: str | None = None,
        parsed_sql: str | None = None,
        suggestion: list[str] | None = None,
    ):
        self.is_valid = is_valid
        self.error_message = error_message
        self.parsed_sql = parsed_sql
        self.suggestion = suggestion

    def to_dict(self) -> dict:
        return {
            "isValid": self.is_valid,
            "errorMessage": self.error_message,
            "parsedSql": self.parsed_sql,
            "suggestion": self.suggestion,
        }
