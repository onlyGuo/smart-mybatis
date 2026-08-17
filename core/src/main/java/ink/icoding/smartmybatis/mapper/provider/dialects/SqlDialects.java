package ink.icoding.smartmybatis.mapper.provider.dialects;

import ink.icoding.smartmybatis.entity.expression.*;
import ink.icoding.smartmybatis.entity.po.PO;
import ink.icoding.smartmybatis.entity.po.enums.PrimaryGenerateType;
import ink.icoding.smartmybatis.entity.po.enums.TableField;
import ink.icoding.smartmybatis.utils.LambdaFieldUtil;
import ink.icoding.smartmybatis.utils.entity.ColumnDeclaration;
import ink.icoding.smartmybatis.utils.entity.MapperDeclaration;
import ink.icoding.smartmybatis.utils.entity.MapperUtil;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * SQL 方言接口
 * <p>
 * 所有 SQL 生成均通过此接口完成。标准 SQL 部分提供默认实现，
 * 具体数据库方言只需覆写有差异的方法。
 * <p>
 * 设计分为两层：
 * <ul>
 *   <li>原子层：标识符引用、分页、类型映射、DDL 构件等最小差异点</li>
 *   <li>语句层：INSERT / SELECT / UPDATE / DELETE / CREATE TABLE / ALTER TABLE 完整语句生成</li>
 * </ul>
 *
 * @author gsk
 */
public interface SqlDialects {

    // ══════════════════════════════════════════════════════════
    //  第一层：原子操作（方言差异点，具体实现按需覆写）
    // ══════════════════════════════════════════════════════════

    /**
     * 引用标识符（表名、列名）
     * <p>标准 SQL: "name" &nbsp;&nbsp; MySQL: `name` &nbsp;&nbsp; SQLServer: [name]</p>
     */
    default String quote(String identifier) {
        return "\"" + identifier + "\"";
    }

    /**
     * 生成分页子句
     * <p>标准 SQL: LIMIT size OFFSET offset &nbsp;&nbsp; MySQL: LIMIT offset, size</p>
     */
    default String buildLimit(int offset, int size) {
        return " LIMIT " + size + " OFFSET " + offset;
    }

    /**
     * Java 类型 → SQL 列类型映射
     * <p>标准 SQL: INTEGER, BIGINT, BOOLEAN, VARCHAR, TEXT ...</p>
     */
    default String javaTypeToSql(Class<?> type, TableField tableField) {
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
            return "INTEGER";
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
                return "TEXT";
            }
        }
        if (length <= 16383) {
            return "VARCHAR(" + length + ")";
        } else if (length <= 65535) {
            return "TEXT";
        } else {
            return "TEXT";
        }
    }

    /**
     * 构建列定义（用于 CREATE TABLE / ALTER TABLE ADD COLUMN）
     * <p>标准 SQL: "name" TYPE DEFAULT NULL</p>
     */
    default String buildColumnDef(String columnName, String columnType, String description) {
        StringBuilder sb = new StringBuilder();
        sb.append(quote(columnName)).append(" ").append(columnType);
        sb.append(" DEFAULT NULL");
        return sb.toString();
    }

    /**
     * 构建自增主键列定义（CREATE TABLE 中使用）
     * <p>标准 SQL: "id" BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY</p>
     */
    default String buildAutoPkDef(String columnName, String columnType) {
        return quote(columnName) + " " + columnType
                + " GENERATED ALWAYS AS IDENTITY PRIMARY KEY";
    }

    /**
     * CREATE TABLE 语句后缀
     * <p>标准 SQL: ); &nbsp;&nbsp; MySQL: ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;</p>
     */
    default String buildCreateSuffix() {
        return ");";
    }

    /**
     * 构建 ALTER TABLE 的修改列子句
     * <p>标准 SQL: ALTER COLUMN "col" TYPE ...</p>
     */
    default String buildAlterColumn(String columnName, String columnType, String description) {
        return "ALTER COLUMN " + quote(columnName) + " TYPE " + columnType;
    }

    /**
     * 默认表别名
     * <p>标准 SQL: "_t" &nbsp;&nbsp; Oracle: "t"（Oracle 不接受下划线开头的未加引号别名）</p>
     */
    default String tableAlias() {
        return "_t";
    }

    /**
     * 表别名连接词
     * <p>标准 SQL: " AS " &nbsp;&nbsp; Oracle: " "（Oracle 不支持表别名使用 AS）</p>
     */
    default String tableAliasAs() {
        return " AS ";
    }

    /**
     * 当前方言是否支持为指定 INSERT 方法启用 JDBC generated keys。
     */
    default boolean supportsGeneratedKeys(String methodName, MapperDeclaration mapperDeclaration) {
        return true;
    }

    /**
     * 当前方言是否要求指定 mapper 方法使用 CallableStatement。
     * <p>默认所有语句都走 PreparedStatement；需要使用 OUT 参数回填主键的方言可覆写。</p>
     */
    default boolean requiresCallableStatement(String methodName, MapperDeclaration mapperDeclaration) {
        return false;
    }

    /**
     * 当前方言的批量插入是否复用基础单条插入。
     * <p>默认仍走批量 SQL；Oracle 等需要逐条获取 identity 主键并保持返回行数语义的方言可覆写。</p>
     */
    default boolean usesBaseInsertForBatch() {
        return false;
    }

    /**
     * 自动关联字段查询时生成的别名前缀。
     */
    default String relationAliasPrefix() {
        return "_rel";
    }

    /**
     * 将 ALTER TABLE 子句列表组合为完整语句
     * <p>标准 SQL: ALTER TABLE "t" clause1, clause2, ...;</p>
     * <p>Oracle: 每个 ADD 为独立语句，MODIFY 合并为一组</p>
     *
     * @param tableName   表名
     * @param alterClauses 子句列表（如 ADD COLUMN ..., MODIFY COLUMN ...）
     * @return 完整的 ALTER TABLE SQL 语句
     */
    default String buildAlterTableStatement(String tableName, List<String> alterClauses) {
        return "ALTER TABLE " + quote(tableName) + " " + String.join(", ", alterClauses) + ";";
    }

    // ══════════════════════════════════════════════════════════
    //  第二层：CRUD 语句生成（标准 SQL 默认实现）
    // ══════════════════════════════════════════════════════════

    /**
     * 构建单条 INSERT 语句
     */
    default String buildInsert(MapperDeclaration decl) {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(quote(decl.getTableName())).append(" (");
        StringBuilder valuesPart = new StringBuilder(" VALUES (");
        boolean first = true;
        List<ColumnDeclaration> columnDeclarations = decl.getColumnDeclarations();
        for (ColumnDeclaration cd : columnDeclarations) {
            if (!first) {
                sql.append(", ");
                valuesPart.append(", ");
            }
            sql.append(quote(cd.getColumnName()));
            valuesPart.append(buildParameterPlaceholder("record." + cd.getFieldName(), cd));
            first = false;
        }
        if (decl.getPkGenerateType() != PrimaryGenerateType.AUTO) {
            if (!first) {
                sql.append(", ");
                valuesPart.append(", ");
            }
            sql.append(quote(decl.getPkColumnName()));
            valuesPart.append(buildParameterPlaceholder("record." + decl.getPkName(), decl.getPkClass(), false));
        }
        sql.append(")");
        valuesPart.append(")");
        sql.append(valuesPart);
        return sql.toString();
    }

    /**
     * 构建批量 INSERT 语句
     *
     * @param recordCount 记录数量
     */
    default String buildInsertBatch(MapperDeclaration decl, int recordCount) {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(quote(decl.getTableName())).append(" (");
        List<ColumnDeclaration> columnDeclarations = decl.getColumnDeclarations();
        for (ColumnDeclaration cd : columnDeclarations) {
            sql.append(quote(cd.getColumnName())).append(", ");
        }
        sql.append(quote(decl.getPkColumnName())).append(") VALUES ");

        for (int idx = 0; idx < recordCount; idx++) {
            sql.append("(");
            for (ColumnDeclaration cd : columnDeclarations) {
                sql.append(buildParameterPlaceholder("list[" + idx + "]." + cd.getFieldName(), cd)).append(", ");
            }
            sql.append(buildParameterPlaceholder("list[" + idx + "]." + decl.getPkName(), decl.getPkClass(), false)).append(")");
            if (idx < recordCount - 1) {
                sql.append(", ");
            }
        }
        return sql.toString();
    }

    /**
     * 构建 SELECT 语句
     *
     * @param whereSql     方言构建好的 WHERE 子句（含 ORDER BY / LIMIT）
     * @param withRelations 是否包含自动关联 JOIN
     */
    default String buildSelect(MapperDeclaration decl, Where where, String whereSql, boolean withRelations) {
        StringBuilder sql = new StringBuilder("SELECT ");
        int relIndex = 0;
        Map<Class<? extends PO>, String> relationAliasMap = new LinkedHashMap<>();

        // 构建 SELECT 字段列表
        for (ColumnDeclaration cd : decl.getColumnDeclarations(withRelations)) {
            if (cd.isLink()) {
                TableField tf = cd.getAnnotation();
                Class<? extends PO> linkClass = tf == null ? PO.class : tf.link();
                String alias = cd.getAlias();
                if (!StringUtils.hasText(alias) && linkClass != PO.class) {
                    alias = relationAliasMap.get(linkClass);
                }
                if (!StringUtils.hasText(alias)) {
                    alias = relationAliasPrefix() + relIndex++;
                }
                if (linkClass != PO.class && !relationAliasMap.containsKey(linkClass)) {
                    relationAliasMap.put(linkClass, alias);
                }
                sql.append(alias).append(".").append(quote(cd.getColumnName())).append(" AS ");
                if (where != null && cd.getField() != null) {
                    where.putGlobalWhere(cd.getField(), alias + "." + quote(cd.getColumnName()));
                }
            } else {
                sql.append(tableAlias()).append(".").append(quote(cd.getColumnName())).append(" AS ");
            }
            sql.append(cd.getFieldName()).append(", ");
        }
        // 主键
        sql.append(tableAlias()).append(".").append(quote(decl.getPkColumnName())).append(" AS ").append(decl.getPkName());

        // FROM 子句
        Map<String, AliasMapping<?>> aliasMappings = where == null ? null : where.getAliasMappings();
        if (aliasMappings == null || aliasMappings.isEmpty()) {
            sql.append(" FROM ").append(quote(decl.getTableName())).append(tableAliasAs()).append(tableAlias());
            if (withRelations) {
                appendAutoRelationJoins(sql, decl, relationAliasMap);
            }
            return sql.toString() + whereSql;
        }

        // 有别名连接时, 处理别名连接的 SELECT 字段
        aliasMappings.values().forEach(am -> {
            am.getSelectFields().forEach(sField -> {
                Field field = LambdaFieldUtil.getField(sField);
                TableField tf = field.getAnnotation(TableField.class);
                ColumnDeclaration cd = null;
                if (null != tf && !tf.exist() && tf.link() != PO.class) {
                    String linkFieldName = tf.linkField();
                    if (linkFieldName == null || linkFieldName.isEmpty()) {
                        linkFieldName = field.getName();
                    }
                    try {
                        Field declaredField = tf.link().getDeclaredField(linkFieldName);
                        cd = MapperUtil.getColumnDeclaration(declaredField);
                    } catch (NoSuchFieldException e) {
                        throw new IllegalArgumentException("Linked field " + linkFieldName + " not found in class "
                                + tf.link().getName() + " for field " + field.getName());
                    }
                } else {
                    throw new IllegalArgumentException("Select field " + field.getName()
                            + " is not a linked field in alias mapping for alias " + am.getAlias());
                }
                sql.append(", ").append(am.getAlias()).append(".")
                        .append(quote(cd.getColumnName())).append(" AS ").append(field.getName());
                where.putGlobalWhere(field, am.getAlias() + "." + quote(cd.getColumnName()));
            });
        });

        // FROM + JOIN
        sql.append(" FROM ").append(quote(decl.getTableName())).append(tableAliasAs()).append(tableAlias());
        if (withRelations) {
            appendAutoRelationJoins(sql, decl, relationAliasMap);
        }
        for (AliasMapping<?> am : aliasMappings.values()) {
            MapperDeclaration joinDecl = MapperUtil.getMapperDeclarationByPoClass(am.getEntityClass());
            sql.append(" ").append(am.getType()).append(" ")
                    .append(quote(joinDecl.getTableName())).append(" ").append(am.getAlias());
            Where onWhere = am.getOnWhere();
            if (null != onWhere) {
                onWhere.setAliasMappings(aliasMappings);
                sql.append(" ").append(buildWherePart(onWhere, true, ""));
            }
        }
        return sql.toString() + whereSql;
    }

    /**
     * 构建 COUNT 语句
     */
    default String buildCount(MapperDeclaration decl, String whereSql) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(");
        sql.append(tableAlias()).append(".").append(quote(decl.getPkColumnName())).append(") AS ");
        sql.append(quote("count")).append(" FROM ").append(quote(decl.getTableName())).append(tableAliasAs()).append(tableAlias());
        return sql.toString() + whereSql;
    }

    /**
     * 构建按主键查询语句
     */
    default String buildSelectById(MapperDeclaration decl) {
        return buildSelect(decl, null, "", false)
                + " WHERE " + tableAlias() + "." + quote(decl.getPkColumnName()) + " = #{id}";
    }

    /**
     * 构建按主键删除语句
     */
    default String buildDeleteById(MapperDeclaration decl) {
        return "DELETE FROM " + quote(decl.getTableName())
                + " WHERE " + quote(decl.getPkColumnName()) + " = #{id}";
    }

    /**
     * 构建按主键批量删除语句
     *
     * @param idCount ID 数量
     */
    default String buildDeleteByIds(MapperDeclaration decl, int idCount) {
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

    /**
     * 构建按条件删除语句
     */
    default String buildDelete(MapperDeclaration decl, String whereSql) {
        return "DELETE FROM " + quote(decl.getTableName()) + tableAliasAs() + tableAlias() + whereSql;
    }

    /**
     * 构建按主键更新语句
     */
    default String buildUpdate(MapperDeclaration decl) {
        StringBuilder sql = new StringBuilder("UPDATE ")
                .append(quote(decl.getTableName()))
                .append(" SET ");
        List<ColumnDeclaration> columnDeclarations = decl.getColumnDeclarations();
        for (int i = 0; i < columnDeclarations.size(); i++) {
            ColumnDeclaration cd = columnDeclarations.get(i);
            sql.append(quote(cd.getColumnName())).append(" = ")
                    .append(buildParameterPlaceholder("record." + cd.getFieldName(), cd));
            if (i < columnDeclarations.size() - 1) {
                sql.append(", ");
            }
        }
        sql.append(" WHERE ").append(quote(decl.getPkColumnName()))
                .append(" = ").append(buildParameterPlaceholder("record." + decl.getPkName(), decl.getPkClass(), false));
        return sql.toString();
    }

    /**
     * 构建 CREATE TABLE DDL 语句
     */
    default String buildCreateTable(MapperDeclaration decl) {
        StringBuilder sb = new StringBuilder("CREATE TABLE ")
                .append(quote(decl.getTableName())).append(" (\n");
        // 主键列
        String pkType = javaTypeToSql(decl.getPkClass(), decl.getPkAnnotation());
        sb.append(buildAutoPkDef(decl.getPkColumnName(), pkType)).append(", \n");
        // 其他列
        for (ColumnDeclaration cd : decl.getColumnDeclarations()) {
            sb.append(buildColumnDef(cd.getColumnName(), cd.getColumnType(), cd.getDescription()));
            sb.append(", \n");
        }
        sb.setLength(sb.length() - 3);
        sb.append("\n").append(buildCreateSuffix());
        return sb.toString();
    }

    /**
     * 构建 ALTER TABLE DDL 语句
     *
     * @param existingColumns 数据库中已存在的列
     */
    default String buildAlterTable(MapperDeclaration decl, List<ColumnDeclaration> existingColumns) {
        String tableName = decl.getTableName();
        String pkName = decl.getPkColumnName();

        Map<String, ColumnDeclaration> existMap = new HashMap<>();
        if (existingColumns != null) {
            for (ColumnDeclaration c : existingColumns) {
                existMap.put(c.getColumnName().toLowerCase(), c);
            }
        }

        List<ColumnDeclaration> declared = decl.getColumnDeclarations() != null
                ? decl.getColumnDeclarations() : Collections.emptyList();

        List<String> alterClauses = new ArrayList<>();

        // 主键列补齐
        boolean pkExists = existMap.containsKey(pkName.toLowerCase());
        if (!pkExists) {
            String pkType = javaTypeToSql(decl.getPkClass(), decl.getPkAnnotation());
            alterClauses.add("ADD COLUMN " + buildAutoPkDef(pkName, pkType));
        }

        // 新增/修改非主键列
        for (ColumnDeclaration declCol : declared) {
            String name = declCol.getColumnName();
            if (pkName.equalsIgnoreCase(name)) {
                continue;
            }
            String key = name.toLowerCase();
            ColumnDeclaration existCol = existMap.get(key);

            if (existCol == null) {
                alterClauses.add("ADD COLUMN " + buildColumnDef(declCol.getColumnName(),
                        declCol.getColumnType(), declCol.getDescription()));
            } else {
                boolean typeChanged = !normalizeType(declCol.getColumnType())
                        .equals(normalizeType(existCol.getColumnType()));
                boolean commentChanged = !normalizeComment(declCol.getDescription())
                        .equals(normalizeComment(existCol.getDescription()));
                if (typeChanged || commentChanged) {
                    alterClauses.add(buildAlterColumn(declCol.getColumnName(),
                            declCol.getColumnType(), declCol.getDescription()));
                }
            }
        }

        if (alterClauses.isEmpty()) {
            return null;
        }
        return buildAlterTableStatement(tableName, alterClauses);
    }

    // ══════════════════════════════════════════════════════════
    //  WHERE 子句构建
    // ══════════════════════════════════════════════════════════

    /**
     * 构建 WHERE 子句（含 ORDER BY / LIMIT）
     *
     * @param where            条件表达式
     * @param inOn             是否在 ON 子句中
     * @param parentParamPrefix MyBatis 参数前缀
     * @return WHERE 子句 SQL
     */
    default String buildWherePart(Where where, boolean inOn, String parentParamPrefix) {
        if (null == where) {
            return "";
        }
        if (null == parentParamPrefix) {
            parentParamPrefix = "";
        }
        List<Expression<?>> expressions = where.getExpressions();
        List<SortExpression<?>> sortExpressions = where.getSortExpressions();
        int limitSize = where.getLimitSize();
        if ((null == expressions || expressions.isEmpty())
                && (null == sortExpressions || sortExpressions.isEmpty())
                && limitSize == 0) {
            return "";
        }

        Map<String, AliasMapping<?>> aliasMappings = where.getAliasMappings();
        Map<String, String> aliasMappingMap = new HashMap<>();
        if (null != aliasMappings && !aliasMappings.isEmpty()) {
            for (AliasMapping<?> am : aliasMappings.values()) {
                aliasMappingMap.put(am.getEntityClass().getName(), am.getAlias());
            }
        }

        StringBuilder wherePart = new StringBuilder();
        if (null != expressions && !expressions.isEmpty()) {
            wherePart.append(" ").append(inOn ? "ON" : "WHERE");
            for (int i = 0; i < expressions.size(); i++) {
                Expression<?> expression = expressions.get(i);

                // 嵌套 Where 表达式
                if (expression instanceof WhereExpression) {
                    WhereExpression whereExpression = (WhereExpression) expression;
                    Where subWhere = whereExpression.getWhere();
                    subWhere.setAliasMappings(aliasMappings);
                    String subWhereSql = buildWherePart(subWhere, false,
                            parentParamPrefix + "expressions[" + i + "].where.");
                    if (subWhereSql.isEmpty()) {
                        continue;
                    }
                    if (subWhereSql.trim().startsWith("WHERE")) {
                        subWhereSql = subWhereSql.trim().substring(5);
                    }
                    if (i > 0) {
                        wherePart.append(" ").append(whereExpression.getLink().name()).append(" ");
                    }
                    wherePart.append(" (").append(subWhereSql).append(") ");
                    continue;
                }

                ComparisonExpression<?> compExpr = (ComparisonExpression<?>) expression;
                Link link = compExpr.getLink();
                if (null != link && i > 0) {
                    wherePart.append(" ").append(link.name()).append(" ");
                }

                SFunction<? extends PO, ?> func = compExpr.getFunc();
                Field field = LambdaFieldUtil.getField(func);
                Class<? extends PO> poClass = LambdaFieldUtil.getPoClass(func);
                C comparison = compExpr.getComparison();
                String alias = aliasMappingMap.getOrDefault(poClass.getName(), tableAlias());

                // 字段引用
                String fieldSqlRef = resolveFieldSqlRef(where, field, poClass, aliasMappingMap);
                if (StringUtils.hasText(fieldSqlRef)) {
                    wherePart.append(" ").append(fieldSqlRef).append(" ");
                } else {
                    wherePart.append(" ").append(alias).append(".")
                            .append(quote(MapperUtil.getColumnDeclaration(field).getColumnName())).append(" ");
                }

                Object value = compExpr.getValue();
                // NULL 值处理
                if (null == value) {
                    if (comparison == C.EQ || comparison == C.equals) {
                        wherePart.append("IS NULL ");
                        continue;
                    } else if (comparison == C.NE || comparison == C.notEquals) {
                        wherePart.append("IS NOT NULL ");
                        continue;
                    } else {
                        throw new IllegalArgumentException("Cannot use comparison " + comparison.name()
                                + " with NULL value for field " + field.getName());
                    }
                }

                wherePart.append(comparison.value()).append(" ");

                // 字段对字段比较
                if (value instanceof SFunction) {
                    SFunction<? extends PO, ?> valueFunc = (SFunction<? extends PO, ?>) value;
                    Field valueField = LambdaFieldUtil.getField(valueFunc);
                    Class<? extends PO> valuePoClass = LambdaFieldUtil.getPoClass(valueFunc);
                    String valueRef = resolveFieldSqlRef(where, valueField, valuePoClass, aliasMappingMap);
                    if (!StringUtils.hasText(valueRef)) {
                        ColumnDeclaration valueCd = MapperUtil.getColumnDeclaration(valueField);
                        String valueAlias = aliasMappingMap.getOrDefault(valuePoClass.getName(), tableAlias());
                        valueRef = valueAlias + "." + quote(valueCd.getColumnName());
                    }
                    wherePart.append(valueRef).append(" ");
                    continue;
                }

                // IN / NOT IN
                if (comparison == C.IN || comparison == C.NOT_IN || comparison == C.in || comparison == C.notIn) {
                    wherePart.append(" (");
                    if (value.getClass().isArray()) {
                        value = Arrays.asList((Object[]) value);
                    }
                    if (!(value instanceof Collection<?>)) {
                        throw new IllegalArgumentException("Value for IN or NOT IN comparison must be a Collection or Array, but got: "
                                + value.getClass().getName());
                    } else {
                        if (((Collection<?>) value).isEmpty()) {
                            if (comparison == C.IN || comparison == C.in) {
                                wherePart.setLength(wherePart.length() - "IN (".length() - 1);
                                wherePart.setLength(wherePart.toString().trim().lastIndexOf(" ") + 1);
                                wherePart.append(" 1=0");
                            } else {
                                wherePart.setLength(wherePart.length() - "NOT IN (".length() - 1);
                                wherePart.setLength(wherePart.toString().trim().lastIndexOf(" ") + 1);
                                wherePart.append(" 1=1");
                            }
                        } else if (value instanceof Set) {
                            value = new ArrayList<>((Set<?>) value);
                            compExpr.setValue(value);
                        }
                    }
                    Collection<?> valueList = (Collection<?>) value;
                    for (int cl = 0; cl < valueList.size(); cl++) {
                        wherePart.append("#{").append(parentParamPrefix).append("expressions[")
                                .append(i).append("].value[").append(cl).append("]}");
                        if (cl < valueList.size() - 1) {
                            wherePart.append(", ");
                        }
                    }
                    if (!valueList.isEmpty()) {
                        wherePart.append(") ");
                    }
                } else {
                    // 普通参数绑定
                    if (inOn) {
                        wherePart.append("#{aliasMappings.").append(alias)
                                .append(".onWhere.expressions[").append(i).append("].value} ");
                    } else {
                        wherePart.append("#{").append(parentParamPrefix)
                                .append("expressions[").append(i).append("].value} ");
                    }
                    // LIKE 自动加 %
                    if (comparison == C.LIKE || comparison == C.NOT_LIKE
                            || comparison == C.like || comparison == C.notLike) {
                        String strValue = value.toString();
                        if (!strValue.contains("%")) {
                            strValue = "%" + strValue + "%";
                            compExpr.setValue(strValue);
                        }
                    }
                }
            }
        }

        // ORDER BY
        if (null != sortExpressions && !sortExpressions.isEmpty()) {
            wherePart.append(" ORDER BY ");
            for (int i = 0; i < sortExpressions.size(); i++) {
                SortExpression<?> se = sortExpressions.get(i);
                SFunction<? extends PO, ?> func = se.getFunc();
                Field field = LambdaFieldUtil.getField(func);
                Class<? extends PO> poClass = LambdaFieldUtil.getPoClass(func);
                String orderRef = resolveFieldSqlRef(where, field, poClass, aliasMappingMap);
                if (!StringUtils.hasText(orderRef)) {
                    ColumnDeclaration cd = MapperUtil.getColumnDeclaration(field);
                    String orderAlias = aliasMappingMap.getOrDefault(poClass.getName(), tableAlias());
                    orderRef = orderAlias + "." + quote(cd.getColumnName());
                }
                wherePart.append(orderRef).append(" ").append(se.getDirection().name());
                if (i < sortExpressions.size() - 1) {
                    wherePart.append(", ");
                }
            }
        }

        // LIMIT
        if (limitSize > 0) {
            wherePart.append(buildLimit(where.getLimitStart(), where.getLimitSize()));
        }

        return wherePart.toString();
    }

    // ══════════════════════════════════════════════════════════
    //  内部辅助方法
    // ══════════════════════════════════════════════════════════

    /**
     * 追加自动关联 LEFT JOIN 子句
     */
    default void appendAutoRelationJoins(StringBuilder sql,
                                         MapperDeclaration mapperDeclaration,
                                         Map<Class<? extends PO>, String> relationAliasMap) {
        if (relationAliasMap.isEmpty()) {
            return;
        }
        Set<String> joinedAliases = new HashSet<>();
        for (ColumnDeclaration cd : mapperDeclaration.getColumnDeclarations(true)) {
            if (!cd.isLink()) {
                continue;
            }
            TableField tf = cd.getAnnotation();
            if (tf == null || tf.link() == PO.class) {
                continue;
            }
            String alias = relationAliasMap.get(tf.link());
            if (!StringUtils.hasText(alias) || !joinedAliases.add(alias)) {
                continue;
            }
            MapperDeclaration relDecl = MapperUtil.getMapperDeclarationByPoClass(tf.link());
            String relJoinColumn = resolveRelationJoinColumn(tf, relDecl);
            String baseJoinColumn = resolveBaseJoinColumn(mapperDeclaration, cd);
            sql.append(" LEFT JOIN ").append(quote(relDecl.getTableName())).append(" ")
                    .append(alias)
                    .append(" ON ")
                    .append(alias).append(".").append(quote(relJoinColumn))
                    .append(" = ").append(tableAlias()).append(".").append(quote(baseJoinColumn));
        }
    }

    default String resolveRelationJoinColumn(TableField tf, MapperDeclaration relDecl) {
        if (StringUtils.hasText(tf.value())) {
            return tf.value();
        }
        return relDecl.getPkColumnName();
    }

    default String resolveBaseJoinColumn(MapperDeclaration mapperDeclaration, ColumnDeclaration linkColumn) {
        TableField annotation = linkColumn.getAnnotation();
        if (null == annotation) {
            throw new IllegalArgumentException("Link column " + linkColumn.getFieldName()
                    + " must have TableField annotation for auto relation join");
        }
        String self = annotation.self();
        if (!StringUtils.hasText(self)) {
            throw new IllegalArgumentException("Link column " + linkColumn.getFieldName()
                    + " must specify self field name for auto relation join");
        }
        ColumnDeclaration declaration = MapperUtil.getFieldDeclarationByPoClass(
                mapperDeclaration.getPoClass(), self);
        return declaration.getColumnName();
    }

    /**
     * 解析字段的 SQL 引用（处理关联表字段）
     */
    default String resolveFieldSqlRef(Where where,
                                      Field field,
                                      Class<? extends PO> poClass,
                                      Map<String, String> aliasMappingMap) {
        String cached = where.getGlobalWhereAliasValue(field);
        if (StringUtils.hasText(cached)) {
            return cached;
        }
        TableField tf = field.getAnnotation(TableField.class);
        if (tf != null && !tf.exist()) {
            if (tf.link() == PO.class) {
                return null;
            }
            String alias = aliasMappingMap.get(tf.link().getName());
            if (!StringUtils.hasText(alias)) {
                return null;
            }
            String linkFieldName = tf.linkField();
            if (!StringUtils.hasText(linkFieldName)) {
                linkFieldName = field.getName();
            }
            try {
                Field declaredField = tf.link().getDeclaredField(linkFieldName);
                ColumnDeclaration cd = MapperUtil.getColumnDeclaration(declaredField);
                return alias + "." + quote(cd.getColumnName());
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException("Linked field " + linkFieldName + " not found in class "
                        + tf.link().getName() + " for field " + field.getName(), e);
            }
        }
        ColumnDeclaration cd = MapperUtil.getColumnDeclaration(field);
        String alias = aliasMappingMap.getOrDefault(poClass.getName(), tableAlias());
        return alias + "." + quote(cd.getColumnName());
    }

    // ══════════════════════════════════════════════════════════
    //  工具方法
    // ══════════════════════════════════════════════════════════

    default String normalizeType(String type) {
        if (type == null) return "";
        return type.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    default String normalizeComment(String comment) {
        return comment == null ? "" : comment;
    }

    default String buildParameterPlaceholder(String propertyPath, ColumnDeclaration columnDeclaration) {
        Class<?> javaType = columnDeclaration.getField() == null ? String.class : columnDeclaration.getField().getType();
        return buildParameterPlaceholder(propertyPath, javaType, columnDeclaration.isJson());
    }

    default String buildParameterPlaceholder(String propertyPath, Class<?> javaType, boolean json) {
        StringBuilder placeholder = new StringBuilder("#{").append(propertyPath);
        if (json) {
            placeholder.append(", typeHandler=ink.icoding.smartmybatis.mapper.handlers.SmartJsonTypeHandler");
        }
        String jdbcType = resolveJdbcType(javaType, json);
        if (jdbcType != null && !jdbcType.isEmpty()) {
            placeholder.append(", jdbcType=").append(jdbcType);
        }
        placeholder.append("}");
        return placeholder.toString();
    }

    default String resolveJdbcType(Class<?> javaType, boolean json) {
        if (json) {
            return "CLOB";
        }
        if (javaType == null) {
            return "VARCHAR";
        }
        if (javaType == Integer.class || javaType == int.class
                || javaType == Long.class || javaType == long.class
                || javaType == Short.class || javaType == short.class
                || javaType == Byte.class || javaType == byte.class
                || javaType == Boolean.class || javaType == boolean.class) {
            return "NUMERIC";
        }
        if (javaType == Double.class || javaType == double.class
                || javaType == Float.class || javaType == float.class
                || javaType == java.math.BigDecimal.class) {
            return "DECIMAL";
        }
        if (javaType == java.util.Date.class || javaType == java.sql.Date.class
                || javaType == java.sql.Timestamp.class) {
            return "TIMESTAMP";
        }
        if (javaType.isEnum() || javaType == String.class || CharSequence.class.isAssignableFrom(javaType)) {
            return "VARCHAR";
        }
        return "VARCHAR";
    }
}
