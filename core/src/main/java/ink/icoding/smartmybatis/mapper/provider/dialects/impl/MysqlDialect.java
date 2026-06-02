package ink.icoding.smartmybatis.mapper.provider.dialects.impl;

import ink.icoding.smartmybatis.entity.po.enums.TableField;
import ink.icoding.smartmybatis.mapper.provider.dialects.SmartDialect;
import ink.icoding.smartmybatis.mapper.provider.dialects.SqlDialects;
import ink.icoding.smartmybatis.utils.entity.MapperDeclaration;

/**
 * MySQL 方言实现
 *
 * @author gsk
 */
@SmartDialect({"com.mysql.cj.jdbc.Driver", "com.mysql.jdbc.Driver"})
public class MysqlDialect implements SqlDialects {

    @Override
    public String quote(String identifier) {
        return "`" + identifier + "`";
    }

    @Override
    public String buildLimit(int offset, int size) {
        return " LIMIT " + offset + ", " + size;
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
            return "INT(11)";
        } else if (type == Long.class || type == long.class) {
            return "BIGINT";
        } else if (type == Boolean.class || type == boolean.class) {
            return "TINYINT(1)";
        } else if (type == java.util.Date.class || type == java.sql.Date.class) {
            return "DATETIME";
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
                return "LONGTEXT";
            }
        }
        if (length <= 16383) {
            return "VARCHAR(" + length + ")";
        } else if (length <= 65535) {
            return "TEXT";
        } else {
            return "LONGTEXT";
        }
    }

    @Override
    public String buildColumnDef(String columnName, String columnType, String description) {
        StringBuilder sb = new StringBuilder();
        sb.append(quote(columnName)).append(" ").append(columnType);
        if (description != null && !description.isEmpty()) {
            sb.append(" COMMENT '").append(escapeSqlComment(description)).append("'");
        }
        sb.append(" DEFAULT NULL");
        return sb.toString();
    }

    @Override
    public String buildAutoPkDef(String columnName, String columnType) {
        return quote(columnName) + " " + columnType + " NOT NULL AUTO_INCREMENT PRIMARY KEY";
    }

    @Override
    public String buildCreateSuffix() {
        return ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
    }

    @Override
    public String buildAlterColumn(String columnName, String columnType, String description) {
        return "MODIFY COLUMN " + buildColumnDef(columnName, columnType, description);
    }

    @Override
    public String buildDeleteById(MapperDeclaration decl) {
        return "DELETE FROM " + quote(decl.getTableName())
                + tableAliasAs() + tableAlias() + " WHERE " + tableAlias() + "." + quote(decl.getPkColumnName()) + " = #{id}";
    }

    @Override
    public String buildDeleteByIds(MapperDeclaration decl, int idCount) {
        StringBuilder sql = new StringBuilder("DELETE FROM ")
                .append(quote(decl.getTableName()))
                .append(" WHERE ").append(quote(decl.getPkColumnName()))
                .append(" IN (");
        for (int i = 0; i < idCount; i++) {
            sql.append("#{ids[").append(i).append("]}");
            if (i < idCount - 1) {
                sql.append(", ");
            }
        }
        sql.append(")");
        return sql.toString();
    }

    @Override
    public String buildDelete(MapperDeclaration decl, String whereSql) {
        return "DELETE FROM " + quote(decl.getTableName()) + tableAliasAs() + tableAlias() + whereSql;
    }

    private static String escapeSqlComment(String comment) {
        return comment.replace("'", "''");
    }
}
