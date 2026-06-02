package ink.icoding.smartmybatis.mapper.provider.dialects.impl;

import ink.icoding.smartmybatis.entity.po.enums.PrimaryGenerateType;
import ink.icoding.smartmybatis.utils.entity.ColumnDeclaration;
import ink.icoding.smartmybatis.utils.entity.MapperDeclaration;
import junit.framework.TestCase;

import java.util.Arrays;

public class OracleDialectTest extends TestCase {

    private final OracleDialect dialect = new OracleDialect();

    public void testBuildLimitUsesOracleFetchSyntax() {
        assertEquals(" FETCH FIRST 10 ROWS ONLY", dialect.buildLimit(0, 10));
        assertEquals(" OFFSET 20 ROWS FETCH FIRST 10 ROWS ONLY", dialect.buildLimit(20, 10));
    }

    public void testJavaTypeToSqlUsesOracleTypes() {
        assertEquals("NUMBER(10)", dialect.javaTypeToSql(Integer.class, null));
        assertEquals("NUMBER(19)", dialect.javaTypeToSql(Long.class, null));
        assertEquals("NUMBER(1)", dialect.javaTypeToSql(Boolean.class, null));
        assertEquals("TIMESTAMP", dialect.javaTypeToSql(java.sql.Timestamp.class, null));
        assertEquals("VARCHAR2(255)", dialect.javaTypeToSql(String.class, null));
    }

    public void testTableAliasesDoNotUseAs() {
        assertEquals("t", dialect.tableAlias());
        assertEquals(" ", dialect.tableAliasAs());
        assertEquals("rel", dialect.relationAliasPrefix());
        assertTrue(dialect.usesBaseInsertForBatch());
    }

    public void testInsertBatchUsesReturningIntoForAutoPk() {
        MapperDeclaration declaration = mapperDeclaration(PrimaryGenerateType.AUTO);

        String sql = dialect.buildInsertBatch(declaration, 2);

        assertTrue(dialect.requiresCallableStatement("insertBatch", declaration));
        assertTrue(sql.startsWith("BEGIN INSERT INTO \"SM_STUDENT\" (\"NAME\", \"AGE\") VALUES "));
        assertTrue(sql.contains("RETURNING \"ID\" INTO #{list[0].id, mode=OUT, javaType=java.lang.String, jdbcType=VARCHAR};"));
        assertTrue(sql.contains("RETURNING \"ID\" INTO #{list[1].id, mode=OUT, javaType=java.lang.String, jdbcType=VARCHAR};"));
        assertTrue(sql.endsWith(" END;"));
    }

    public void testInsertBatchIncludesNonAutoPk() {
        MapperDeclaration declaration = mapperDeclaration(PrimaryGenerateType.INPUT);

        String sql = dialect.buildInsertBatch(declaration, 1);

        assertTrue(sql.contains("(\"NAME\", \"AGE\", \"ID\")"));
        assertTrue(sql.contains("#{list[0].id, jdbcType=VARCHAR}"));
    }

    public void testBuildAlterTableStatementUsesOracleAddAndModify() {
        String sql = dialect.buildAlterTableStatement("SM_STUDENT", Arrays.asList(
                "ADD COLUMN \"NAME\" VARCHAR2(64) DEFAULT NULL",
                "ADD COLUMN \"AGE\" NUMBER(10) DEFAULT NULL",
                "MODIFY (\"BIO\" CLOB)"
        ));

        assertEquals("BEGIN EXECUTE IMMEDIATE 'ALTER TABLE \"SM_STUDENT\" ADD (\"NAME\" VARCHAR2(64) DEFAULT NULL, \"AGE\" NUMBER(10) DEFAULT NULL)'; EXECUTE IMMEDIATE 'ALTER TABLE \"SM_STUDENT\" MODIFY (\"BIO\" CLOB)'; END;", sql);
    }

    public void testNormalizeCommentIgnoresColumnComments() {
        assertEquals("", dialect.normalizeComment("changed"));
    }

    private static MapperDeclaration mapperDeclaration(PrimaryGenerateType pkGenerateType) {
        MapperDeclaration declaration = new MapperDeclaration();
        declaration.setTableName("SM_STUDENT");
        declaration.setPkName("id");
        declaration.setPkColumnName("ID");
        declaration.setPkClass(String.class);
        declaration.setPkGenerateType(pkGenerateType);
        declaration.setColumnDeclarations(Arrays.asList(column("name", "NAME"), column("age", "AGE")));
        return declaration;
    }

    private static ColumnDeclaration column(String fieldName, String columnName) {
        ColumnDeclaration column = new ColumnDeclaration();
        column.setFieldName(fieldName);
        column.setColumnName(columnName);
        column.setColumnType("VARCHAR2(255)");
        return column;
    }
}
