import sqlparse
from sqlparse.sql import Identifier, IdentifierList
from sqlparse.tokens import Keyword


def extract_tables_from_sql(sql: str) -> list[str]:
    """
    SQL에서 테이블 이름 추출
    """
    tables: list[str] = []
    parsed = sqlparse.parse(sql)

    if not parsed:
        return tables

    statement = parsed[0]
    from_seen = False

    for token in statement.tokens:
        if from_seen:
            if isinstance(token, IdentifierList):
                for identifier in token.get_identifiers():
                    real_name = identifier.get_real_name()
                    if real_name:
                        tables.append(real_name)
            elif isinstance(token, Identifier):
                real_name = token.get_real_name()
                if real_name:
                    tables.append(real_name)
            from_seen = False

        if token.ttype is Keyword and token.value.upper() == "FROM":
            from_seen = True
        elif token.ttype is Keyword and "JOIN" in token.value.upper():
            from_seen = True

    return tables
