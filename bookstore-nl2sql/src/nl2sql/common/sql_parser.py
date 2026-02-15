import sqlparse
from sqlparse.sql import Identifier, IdentifierList, Statement
from sqlparse.tokens import Keyword, Whitespace


def extract_tables_from_sql(sql: str) -> list[str]:
    """
    SQL에서 테이블 이름 추출
    """
    parsed = sqlparse.parse(sql)

    if not parsed:
        return []

    return extract_tables_from_statement(parsed[0])


def extract_tables_from_statement(statement: Statement) -> list[str]:
    """
    파싱된 statement에서 FROM/JOIN 대상 테이블 추출
    """
    tables: list[str] = []
    from_seen = False

    for token in statement.tokens:
        if from_seen:
            if token.ttype in Whitespace:
                continue

            if isinstance(token, IdentifierList):
                for identifier in token.get_identifiers():
                    table_name = identifier.get_real_name()
                    if table_name:
                        tables.append(table_name)
                from_seen = False
            elif isinstance(token, Identifier):
                table_name = token.get_real_name()
                if table_name:
                    tables.append(table_name)
                from_seen = False
            elif token.ttype in Keyword:
                from_seen = False

        if token.ttype in Keyword and token.value.upper() == "FROM":
            from_seen = True
        elif token.ttype in Keyword and "JOIN" in token.value.upper():
            from_seen = True

    return tables


def extract_table_aliases_from_sql(sql: str) -> dict[str, str]:
    """
    SQL에서 alias -> table_name 매핑 추출
    """
    parsed = sqlparse.parse(sql)
    if not parsed:
        return {}
    return extract_table_aliases_from_statement(parsed[0])


def extract_table_aliases_from_statement(statement: Statement) -> dict[str, str]:
    """
    파싱된 statement에서 alias -> table_name 매핑 추출
    """
    alias_map: dict[str, str] = {}
    from_seen = False

    for token in statement.tokens:
        if from_seen:
            if token.ttype in Whitespace:
                continue

            if isinstance(token, IdentifierList):
                identifiers = token.get_identifiers()
            elif isinstance(token, Identifier):
                identifiers = [token]
            else:
                identifiers = []

            for identifier in identifiers:
                table_name = identifier.get_real_name()
                alias = identifier.get_alias() or table_name
                if table_name and alias:
                    alias_map[alias] = table_name

            if identifiers:
                from_seen = False
            elif token.ttype in Keyword:
                from_seen = False

        if token.ttype in Keyword and token.value.upper() == "FROM":
            from_seen = True
        elif token.ttype in Keyword and "JOIN" in token.value.upper():
            from_seen = True

    return alias_map
