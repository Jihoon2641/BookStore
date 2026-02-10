"""
DBConnector 테스트
"""
import pytest
from sqlalchemy import text
from nl2sql.core.database.db_connector import DBConnector


@pytest.fixture
def db_connector():
    """DBConnector 인스턴스 생성"""
    connector = DBConnector()
    yield connector
    connector.close()


def test_db_connector_initialization(db_connector):
    """DBConnector 초기화 테스트"""
    assert db_connector.engine is not None
    assert db_connector.SessionLocal is not None
    assert db_connector.db_url.startswith("mysql+pymysql://")


def test_get_db_session(db_connector):
    """get_db()로 세션 생성 테스트"""
    with db_connector.get_db() as session:
        assert session is not None
        # 세션이 정상 작동하는지 확인
        result = session.execute(text("SELECT 1"))
        assert result.fetchone()[0] == 1


def test_db_connection(db_connector):
    """실제 DB 연결 테스트"""
    with db_connector.get_db() as session:
        # 간단한 쿼리 실행
        result = session.execute(text("SELECT 1 as num"))
        row = result.fetchone()
        
        assert row is not None
        assert row[0] == 1


def test_multiple_sessions(db_connector):
    """여러 세션 동시 생성 테스트"""
    with db_connector.get_db() as session1:
        with db_connector.get_db() as session2:
            # 두 세션은 서로 다른 객체여야 함
            assert session1 is not session2
            
            # 각각 독립적으로 작동해야 함
            result1 = session1.execute(text("SELECT 1"))
            result2 = session2.execute(text("SELECT 2"))
            
            assert result1.fetchone()[0] == 1
            assert result2.fetchone()[0] == 2
