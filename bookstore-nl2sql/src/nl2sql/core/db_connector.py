from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, Session
from typing import Generator
from contextlib import contextmanager
import yaml
from loguru import logger


class DBConnector:
    """
    데이터베이스 연결 및 세션 관리 클래스
    
    - YAML 설정 파일에서 DB 정보 로드
    - SQLAlchemy Engine 및 Session Factory 생성
    - Connection Pool 관리
    """
    
    def __init__(self, config_path: str = "config/database.yaml"):
        """
        Args:
            config_path: 데이터베이스 설정 YAML 파일 경로
        """
       
        with open(config_path, "r") as f:
            config = yaml.safe_load(f)
        
        self.db_config = config["mysql"]
        
        self.db_url = (
            f"mysql+pymysql://{self.db_config['username']}:"
            f"{self.db_config['password']}@{self.db_config['host']}:"
            f"{self.db_config['port']}/{self.db_config['database']}"
        )
        
        
        self.engine = create_engine(
            self.db_url,
            pool_size=10,          
            max_overflow=20,      
            pool_timeout=30,      
            pool_pre_ping=True,    
            pool_recycle=1800,     
            echo=False             
        )
        
        self.SessionLocal = sessionmaker(
            autocommit=False,
            autoflush=False,
            bind=self.engine
        )
    
    @contextmanager
    def get_db(self) -> Generator[Session, None, None]:
        """
        데이터베이스 세션을 생성하고 반환
        
        사용 예시:
            with db_connector.get_db() as session:
                result = session.query(User).all()
        
        Yields:
            Session: SQLAlchemy 세션 인스턴스
        """
        db = self.SessionLocal()
        try:
            yield db
        finally:
            db.close()
    
    def close(self):
        """
        Engine 종료 및 모든 커넥션 반환
        """
        self.engine.dispose()
