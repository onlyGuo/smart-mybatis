package ink.icoding.smartmybatis.mapper.provider.dialects.impl;

import ink.icoding.smartmybatis.entity.po.enums.PrimaryGenerateType;
import ink.icoding.smartmybatis.entity.po.enums.TableField;
import ink.icoding.smartmybatis.mapper.provider.dialects.SmartDialect;
import ink.icoding.smartmybatis.mapper.provider.dialects.SqlDialects;
import ink.icoding.smartmybatis.utils.entity.ColumnDeclaration;
import ink.icoding.smartmybatis.utils.entity.MapperDeclaration;

import java.util.List;

/**
 * H2 数据库方言实现
 * <p>
 * H2 语法接近标准 SQL，主要差异：
 * <ul>
 *   <li>标识符引用：双引号（标准 SQL，默认实现）</li>
 *   <li>分页：LIMIT n OFFSET m（标准 SQL，默认实现）</li>
 *   <li>自增：AUTO_INCREMENT / IDENTITY</li>
 *   <li>不支持列级 COMMENT 语法</li>
 *   <li>ALTER COLUMN 使用标准 SQL 语法</li>
 * </ul>
 *
 * @author gsk
 */
@SmartDialect({
        "org.h2.Driver"
})
public class H2Dialect implements SqlDialects {

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
            return "INT";
        } else if (type == Long.class || type == long.class) {
            return "BIGINT";
        } else if (type == Boolean.class || type == boolean.class) {
            return "BOOLEAN";
        } else if (type == java.util.Date.class || type == java.sql.Date.class) {
            return "TIMESTAMP";
        } else if (type == Double.class || type == double.class) {
            return "DECIMAL(17, 6)";
        } else if (type == Float.class || type == float.class) {
            return "DECIMAL(17, 1)";
        } else if (type == java.math.BigDecimal.class) {
            return "DECIMAL(17, 6)";
        } else if (type.isEnum()) {
            return "VARCHAR(255)";
        } else {
            if (null != tableField && tableField.json()) {
                return "CLOB";
            }
        }
        if (length <= 16383) {
            return "VARCHAR(" + length + ")";
        } else {
            return "CLOB";
        }
    }

    @Override
    public String buildAutoPkDef(String columnName, String columnType) {
        return quote(columnName) + " " + columnType + " NOT NULL AUTO_INCREMENT PRIMARY KEY";
    }

    @Override
    public String buildInsertBatch(MapperDeclaration decl, int recordCount) {
        // H2 不允许向 IDENTITY 列插入显式值，AUTO 类型需排除主键列
        boolean includePk = decl.getPkGenerateType() != PrimaryGenerateType.AUTO;
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(quote(decl.getTableName())).append(" (");
        List<ColumnDeclaration> columnDeclarations = decl.getColumnDeclarations();
        for (ColumnDeclaration cd : columnDeclarations) {
            sql.append(quote(cd.getColumnName())).append(", ");
        }
        if (includePk) {
            sql.append(quote(decl.getPkColumnName())).append(") VALUES ");
        } else {
            sql.setLength(sql.length() - 2);
            sql.append(") VALUES ");
        }
        for (int idx = 0; idx < recordCount; idx++) {
            sql.append("(");
            for (ColumnDeclaration cd : columnDeclarations) {
                if (cd.isJson()) {
                    sql.append("#{list[").append(idx).append("].")
                            .append(cd.getFieldName())
                            .append(", typeHandler=ink.icoding.smartmybatis.mapper.handlers.SmartJsonTypeHandler}, ");
                } else {
                    sql.append("#{list[").append(idx).append("].")
                            .append(cd.getFieldName()).append("}, ");
                }
            }
            if (includePk) {
                sql.append("#{list[").append(idx).append("].")
                        .append(decl.getPkName()).append("})");
            } else {
                sql.setLength(sql.length() - 2);
                sql.append(")");
            }
            if (idx < recordCount - 1) {
                sql.append(", ");
            }
        }
        return sql.toString();
    }

    @Override
    public String buildCreateSuffix() {
        return ");";
    }

    @Override
    public String buildAlterColumn(String columnName, String columnType, String description) {
        return "ALTER COLUMN " + quote(columnName) + " SET DATA TYPE " + columnType;
    }
}
