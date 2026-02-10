from ast import Dict

from sqlalchemy import text

from nl2sql.core.database.db_connector import DBConnector
from nl2sql.models.shema import ColumnInfo, TableSchema


class SchemaIndexer:
    """
    스키마 인덱서

    Args:
        db_connector: DBConnector - 데이터베이스 연결 객체
    """

    def __init__(self, db_connector: DBConnector):
        """
        Args:
            db_connector: DBConnector - 데이터베이스 연결 객체
        """
        self.db_connector = db_connector

    def _get_table_list(self) -> list[str]:
        """
        테이블 목록 조회

        Returns:
            List[str]: 테이블 목록
        """
        with self.db_connector.get_db() as conn:
            result = conn.execute(text("SHOW TABLES"))
            return [row[0] for row in result]

    def _get_column_info(self, table_name: str) -> list[Dict]:
        """
        컬럼 정보 조회

        Args:
            table_name: str - 테이블 이름

        Returns:
            List[Dict]: 컬럼 정보 목록
        """
        with self.db_connector.get_db() as conn:
            result = conn.execute(text(f"DESCRIBE {table_name}"))
            columns = result.fetchall()

            return [
                {
                    "name": column[0],
                    "type": column[1],
                    "nullable": column[2] == "YES",
                    "key": column[3] if column[3] else None,
                    "default": column[4],
                    "extra": column[5] if column[5] else None,
                }
                for column in columns
            ]

    def extract_schema(self) -> list[TableSchema]:
        """
        모든 테이블의 스키마 추출

        Returns:
            List[TableSchema]: 테이블 스키마 목록
        """

        tables = self._get_table_list()
        schemas = []

        for table_name in tables:
            columns_data = self._get_column_info(table_name)

            columns = [ColumnInfo(**col_data, description_ko="") for col_data in columns_data]

            schema = TableSchema(
                table_name=table_name, columns=columns, description_ko="", foreign_keys=[]
            )

            schemas.append(schema)

        return schemas
