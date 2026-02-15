from langchain_core.runnables import chain
from nl2sql.core.database.db_connector import DBConnector
from nl2sql.error.syntax.sql_validator import SQLValidator

def create_validation_chain(db_connector: DBConnector):

    syntax_validator = create_syntax_validator(db_connector)

    validation_chain = (
        syntax_validator
    )

    return validation_chain
    
def create_syntax_validator(db_connector: DBConnector):
    tables = db_connector.get_table_names()
        
    validator = SQLValidator(tables)

    @chain
    def validate_syntax(input_data):
        sql = input_data['sql']

        with db_connector.get_db() as session:
            validation_result = validator.validate(sql, session=session)

        return {
            **input_data,
            'is_valid': validation_result.is_valid,
            'error_message': validation_result.error_message,
            'parsed_sql': validation_result.parsed_sql,
            'suggestions': validation_result.suggestion or []
        }

    return validate_syntax
