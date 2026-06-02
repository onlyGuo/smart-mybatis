package ink.icoding.smartmybatis.mapper.provider.dialects.impl;

import ink.icoding.smartmybatis.entity.po.enums.PrimaryGenerateType;
import ink.icoding.smartmybatis.entity.po.enums.TableField;
import ink.icoding.smartmybatis.mapper.provider.dialects.SmartDialect;
import ink.icoding.smartmybatis.mapper.provider.dialects.SqlDialects;
import ink.icoding.smartmybatis.utils.entity.ColumnDeclaration;
import ink.icoding.smartmybatis.utils.entity.MapperDeclaration;

import java.util.ArrayList;
import java.util.List;

/**
 * Oracle 数据库方言实现
 * <p>
 * Oracle 主要差异：
 * <ul>
 *   <li>标识符引用：双引号（标准 SQL，默认实现）</li>
 *   <li>分页：OFFSET m ROWS FETCH FIRST n ROWS ONLY（Oracle 12c+）</li>
 *   <li>自增：GENERATED ALWAYS AS IDENTITY（Oracle 12c+）</li>
 *   <li>类型：NUMBER、VARCHAR2、CLOB</li>
 *   <li>无原生 BOOLEAN 类型，映射为 NUMBER(1)</li>
 *   <li>不支持列级 COMMENT 语法</li>
 *   <li>ALTER TABLE 使用 MODIFY 语法</li>
 * </ul>
 *
 * @author gsk
 */
@SmartDialect({
        "oracle.jdbc.OracleDriver",
        "oracle.jdbc.driver.OracleDriver"
})
public class OracleDialect implements SqlDialects {

    private static final String ADD_COLUMN_PREFIX = "ADD COLUMN ";
    private static final String MODIFY_PREFIX = "MODIFY (";

    @Override
    public String buildLimit(int offset, int size) {
        if (offset <= 0) {
            return " FETCH FIRST " + size + " ROWS ONLY";
        }
        return " OFFSET " + offset + " ROWS FETCH FIRST " + size + " ROWS ONLY";
    }

    @Override
    public String javaTypeToSql(Class<?> type, TableField tableField) {
        int length = 255;
        if (null != tableField) {
            if (tableField.columnType() != null && !tableField.columnType().isEmpty()) {
                return tableField.columnType();
            }
            length = tableField.length();
        }
        if (0 == length) {
            length = 255;
        }
        if (type == Integer.class || type == int.class) {
            return "NUMBER(10)";
        } else if (type == Long.class || type == long.class) {
            return "NUMBER(19)";
        } else if (type == Boolean.class || type == boolean.class) {
            return "NUMBER(1)";
        } else if (type == java.util.Date.class || type == java.sql.Date.class
                || type == java.sql.Timestamp.class) {
            return "TIMESTAMP";
        } else if (type == Double.class || type == double.class) {
            return "NUMBER(17, 6)";
        } else if (type == Float.class || type == float.class) {
            return "NUMBER(17, 1)";
        } else if (type == java.math.BigDecimal.class) {
            return "NUMBER(17, 6)";
        } else if (type.isEnum()) {
            return "VARCHAR2(255)";
        } else {
            if (null != tableField && tableField.json()) {
                return "CLOB";
            }
        }
        if (length <= 4000) {
            return "VARCHAR2(" + length + ")";
        }
        return "CLOB";
    }

    @Override
    public String buildColumnDef(String columnName, String columnType, String description) {
        // Oracle 的列注释需要 COMMENT ON COLUMN 独立语句，自动同步只处理结构差异。
        return quote(columnName) + " " + columnType + " DEFAULT NULL";
    }

    @Override
    public String buildAlterColumn(String columnName, String columnType, String description) {
        return MODIFY_PREFIX + quote(columnName) + " " + columnType + ")";
    }

    @Override
    public String tableAlias() {
        return "t";
    }

    @Override
    public String tableAliasAs() {
        return " ";
    }

    @Override
    public String relationAliasPrefix() {
        return "rel";
    }

    @Override
    public boolean supportsGeneratedKeys(String methodName, MapperDeclaration mapperDeclaration) {
        return !"insertBatch".equals(methodName) && !"insertBatchSql".equals(methodName);
    }

    @Override
    public boolean requiresCallableStatement(String methodName, MapperDeclaration mapperDeclaration) {
        return ("insertBatch".equals(methodName) || "insertBatchSql".equals(methodName))
                && mapperDeclaration.getPkGenerateType() == PrimaryGenerateType.AUTO;
    }

    @Override
    public boolean usesBaseInsertForBatch() {
        return true;
    }

    @Override
    public String buildInsertBatch(MapperDeclaration decl, int recordCount) {
        boolean includePk = decl.getPkGenerateType() != PrimaryGenerateType.AUTO;
        List<ColumnDeclaration> columnDeclarations = decl.getColumnDeclarations();

        if (!includePk) {
            return buildInsertBatchReturningPk(decl, recordCount, columnDeclarations);
        }

        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(quote(decl.getTableName())).append(" (");
        appendInsertColumns(sql, decl, columnDeclarations, includePk);
        sql.append(") ");
        for (int idx = 0; idx < recordCount; idx++) {
            if (idx > 0) {
                sql.append(" UNION ALL ");
            }
            sql.append("SELECT ");
            appendInsertValues(sql, decl, columnDeclarations, includePk, idx);
            sql.append(" FROM DUAL");
        }
        return sql.toString();
    }

    private String buildInsertBatchReturningPk(MapperDeclaration decl, int recordCount,
                                               List<ColumnDeclaration> columnDeclarations) {
        StringBuilder sql = new StringBuilder("BEGIN");
        for (int idx = 0; idx < recordCount; idx++) {
            sql.append(" INSERT INTO ")
                    .append(quote(decl.getTableName()))
                    .append(" (");
            appendInsertColumns(sql, decl, columnDeclarations, false);
            sql.append(") VALUES (");
            appendInsertValues(sql, decl, columnDeclarations, false, idx);
            sql.append(") RETURNING ")
                    .append(quote(decl.getPkColumnName()))
                    .append(" INTO ")
                    .append(buildOutParameterPlaceholder("list[" + idx + "]." + decl.getPkName(),
                            decl.getPkClass()))
                    .append(";");
        }
        sql.append(" END;");
        return sql.toString();
    }

    @Override
    public String buildAlterTableStatement(String tableName, List<String> alterClauses) {
        List<String> statements = new ArrayList<>();
        List<String> addColumns = new ArrayList<>();
        List<String> modifyColumns = new ArrayList<>();

        for (String clause : alterClauses) {
            if (clause == null || clause.trim().isEmpty()) {
                continue;
            }
            if (clause.startsWith(ADD_COLUMN_PREFIX)) {
                addColumns.add(clause.substring(ADD_COLUMN_PREFIX.length()).trim());
            } else if (clause.startsWith(MODIFY_PREFIX)) {
                modifyColumns.add(stripModifyClause(clause));
            } else {
                statements.add("ALTER TABLE " + quote(tableName) + " " + clause);
            }
        }

        if (!addColumns.isEmpty()) {
            statements.add("ALTER TABLE " + quote(tableName) + " ADD ("
                    + String.join(", ", addColumns) + ")");
        }
        if (!modifyColumns.isEmpty()) {
            statements.add("ALTER TABLE " + quote(tableName) + " MODIFY ("
                    + String.join(", ", modifyColumns) + ")");
        }

        if (statements.isEmpty()) {
            return null;
        }
        if (statements.size() == 1) {
            return statements.get(0) + ";";
        }

        StringBuilder sql = new StringBuilder("BEGIN");
        for (String statement : statements) {
            sql.append(" EXECUTE IMMEDIATE '")
                    .append(statement.replace("'", "''"))
                    .append("';");
        }
        sql.append(" END;");
        return sql.toString();
    }

    @Override
    public String normalizeComment(String comment) {
        return "";
    }

    private void appendInsertColumns(StringBuilder sql, MapperDeclaration decl,
                                     List<ColumnDeclaration> columnDeclarations,
                                     boolean includePk) {
        for (ColumnDeclaration cd : columnDeclarations) {
            sql.append(quote(cd.getColumnName())).append(", ");
        }
        if (includePk) {
            sql.append(quote(decl.getPkColumnName()));
        } else if (!columnDeclarations.isEmpty()) {
            sql.setLength(sql.length() - 2);
        }
    }

    private void appendInsertValues(StringBuilder sql, MapperDeclaration decl,
                                    List<ColumnDeclaration> columnDeclarations,
                                    boolean includePk, int idx) {
        for (ColumnDeclaration cd : columnDeclarations) {
            sql.append(buildParameterPlaceholder("list[" + idx + "]." + cd.getFieldName(), cd))
                    .append(", ");
        }
        if (includePk) {
            sql.append(buildParameterPlaceholder("list[" + idx + "]." + decl.getPkName(),
                    decl.getPkClass(), false));
        } else if (!columnDeclarations.isEmpty()) {
            sql.setLength(sql.length() - 2);
        }
    }

    private String buildOutParameterPlaceholder(String propertyPath, Class<?> javaType) {
        String jdbcType = resolveJdbcType(javaType, false);
        StringBuilder placeholder = new StringBuilder("#{")
                .append(propertyPath)
                .append(", mode=OUT, javaType=")
                .append(resolveJavaType(javaType));
        if (jdbcType != null && !jdbcType.isEmpty()) {
            placeholder.append(", jdbcType=").append(jdbcType);
        }
        placeholder.append("}");
        return placeholder.toString();
    }

    private String resolveJavaType(Class<?> javaType) {
        if (javaType == null) {
            return String.class.getName();
        }
        if (!javaType.isPrimitive()) {
            return javaType.getName();
        }
        if (javaType == int.class) {
            return Integer.class.getName();
        }
        if (javaType == long.class) {
            return Long.class.getName();
        }
        if (javaType == boolean.class) {
            return Boolean.class.getName();
        }
        if (javaType == double.class) {
            return Double.class.getName();
        }
        if (javaType == float.class) {
            return Float.class.getName();
        }
        if (javaType == short.class) {
            return Short.class.getName();
        }
        if (javaType == byte.class) {
            return Byte.class.getName();
        }
        if (javaType == char.class) {
            return Character.class.getName();
        }
        return javaType.getName();
    }

    private String stripModifyClause(String clause) {
        String columnDef = clause.substring(MODIFY_PREFIX.length());
        if (columnDef.endsWith(")")) {
            return columnDef.substring(0, columnDef.length() - 1);
        }
        return columnDef;
    }

}
