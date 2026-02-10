package ink.icoding.smartmybatis.mapper.provider;

import ink.icoding.smartmybatis.entity.expression.*;
import ink.icoding.smartmybatis.entity.po.PO;
import ink.icoding.smartmybatis.entity.po.enums.PrimaryGenerateType;
import ink.icoding.smartmybatis.entity.po.enums.TableField;
import ink.icoding.smartmybatis.utils.LambdaFieldUtil;
import ink.icoding.smartmybatis.utils.SnowflakeIdGeneratorUtil;
import ink.icoding.smartmybatis.utils.entity.ColumnDeclaration;
import ink.icoding.smartmybatis.utils.entity.MapperDeclaration;
import ink.icoding.smartmybatis.utils.entity.MapperUtil;
import org.apache.ibatis.builder.annotation.ProviderContext;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;

/**
 * 基础 SQL 提供者
 * @author gsk
 */
public class BaseSqlProvider {

    /**
     * 插入记录 SQL 语句生成
     * @param record
     *      记录
     * @param context
     *      上下文
     * @param <T>
     *      记录类型
     * @return SQL 语句
     */
    public <T extends PO> String insert(T record, ProviderContext context) {
        Class<?> mapperType = context.getMapperType();
        MapperDeclaration mapperDeclaration = MapperUtil.getMapperDeclaration(mapperType);

        // 构建主键
        if (mapperDeclaration.getPkGenerateType() != PrimaryGenerateType.AUTO){
            switch (mapperDeclaration.getPkGenerateType()){
                case INPUT:
                    // 如果主键是手动输入, 则插入主键字段
                    Object pkValue = MapperUtil.getFieldValue(record, mapperDeclaration.getPkName());
                    if (null == pkValue){
                        throw new IllegalArgumentException(
                                "Primary key value must be provided for INPUT generate type, but it is null. at "
                                        + mapperDeclaration.getPoClass().getName());
                    }
                case UUID:
                    MapperUtil.setFieldValue(record, mapperDeclaration.getPkName(),
                            UUID.randomUUID().toString().replace("-", ""));
                case SNOWFLAKE:
                    MapperUtil.setFieldValue(record, mapperDeclaration.getPkName(),
                            String.valueOf(SnowflakeIdGeneratorUtil.getInstance().nextId()));
                    break;
                case SNOWFLAKE_HEX:
                    MapperUtil.setFieldValue(record, mapperDeclaration.getPkName(),
                            Long.toHexString(SnowflakeIdGeneratorUtil.getInstance().nextId()));
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Unsupported primary key generate type: " + mapperDeclaration.getPkGenerateType()
                                    + " at " + mapperDeclaration.getPoClass().getName());
            }
        }
        String sql = mapperDeclaration.getBaseInsertSql();
        if (null == sql){
            mapperDeclaration.buildBaseSql();
            sql = mapperDeclaration.getBaseInsertSql();
        }
        return sql;
    }

    /**
     * 批量插入记录 SQL 语句生成
     * @param params
     *      参数
     * @param context
     *      上下文
     * @return SQL 语句
     */
    public String insertBatch(Map<String, Object> params, ProviderContext context) {
        @SuppressWarnings("unchecked")
        Collection<PO> records = (Collection<PO>) params.get("list");
        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException("The records collection for batch insert cannot be null or empty.");
        }
        Class<?> mapperType = context.getMapperType();
        MapperDeclaration mapperDeclaration = MapperUtil.getMapperDeclaration(mapperType);

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO `").append(mapperDeclaration.getTableName()).append("` (");
        List<ColumnDeclaration> columnDeclarations = mapperDeclaration.getColumnDeclarations();
        // 添加列名
        for (ColumnDeclaration columnDeclaration : columnDeclarations) {
            sql.append("`").append(columnDeclaration.getColumnName()).append("`, ");
        }
        // 添加主键列名
        sql.append("`").append(mapperDeclaration.getPkColumnName()).append("`) VALUES ");

        int recordIndex = 0;
        for (PO record : records) {
            // 构建主键
            if (mapperDeclaration.getPkGenerateType() != PrimaryGenerateType.AUTO){
                switch (mapperDeclaration.getPkGenerateType()){
                    case INPUT:
                        // 如果主键是手动输入, 则插入主键字段
                        Object pkValue = MapperUtil.getFieldValue(record, mapperDeclaration.getPkName());
                        if (null == pkValue){
                            throw new IllegalArgumentException(
                                    "Primary key value must be provided for INPUT generate type, but it is null. at "
                                            + mapperDeclaration.getPoClass().getName());
                        }
                    case UUID:
                        MapperUtil.setFieldValue(record, mapperDeclaration.getPkName(),
                                UUID.randomUUID().toString().replace("-", ""));
                    case SNOWFLAKE:
                        MapperUtil.setFieldValue(record, mapperDeclaration.getPkName(),
                                String.valueOf(SnowflakeIdGeneratorUtil.getInstance().nextId()));
                        break;
                    case SNOWFLAKE_HEX:
                        MapperUtil.setFieldValue(record, mapperDeclaration.getPkName(),
                                Long.toHexString(SnowflakeIdGeneratorUtil.getInstance().nextId()));
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Unsupported primary key generate type: " + mapperDeclaration.getPkGenerateType()
                                        + " at " + mapperDeclaration.getPoClass().getName());
                }
            }
            sql.append("(");
            // 添加列值
            for (ColumnDeclaration columnDeclaration : columnDeclarations) {
                if (columnDeclaration.isJson()){
                    sql.append("#{list[").append(recordIndex).append("].")
                            .append(columnDeclaration.getFieldName())
                            .append(", typeHandler=ink.icoding.smartmybatis.mapper.handlers.SmartJsonTypeHandler}, ");
                }else{
                    sql.append("#{list[").append(recordIndex).append("].")
                            .append(columnDeclaration.getFieldName()).append("}, ");
                }
            }
            // 添加主键列值
            sql.append("#{list[").append(recordIndex).append("].")
                    .append(mapperDeclaration.getPkName()).append("})");
            recordIndex++;
            if (recordIndex < records.size()) {
                sql.append(", ");
            }
        }
        return sql.toString();
    }

    /**
     * 根据 Where 条件生成查询 SQL 语句
     * @param where
     *      查询条件
     * @return SQL 语句
     */
    public String selectByWhere(Where where, ProviderContext context){
        Class<?> mapperType = context.getMapperType();
        return buildSelectFields(MapperUtil.getMapperDeclaration(mapperType), where) + buildWherePart(where, false);
    }

    /**
     * 根据 Where 条件生成统计记录数 SQL 语句
     * @param where
     *      查询条件
     * @return SQL 语句
     */
    public String countByWhere(Where where, ProviderContext context) {
        Class<?> mapperType = context.getMapperType();
        MapperDeclaration declaration = MapperUtil.getMapperDeclaration(mapperType);
        StringBuilder sql = new StringBuilder("SELECT COUNT(").append("_t.").append(declaration.getPkColumnName()).append(") AS ");
        sql.append("`count` FROM `").append(declaration.getTableName()).append("`").append(" AS _t");
        sql.append(buildWherePart(where, false));
        return sql.toString();
    }

    /**
     * 根据主键生成查询 SQL 语句
     * @param id
     *      主键
     * @return SQL 语句
     */
    public String selectByPrimaryKey(Serializable id, ProviderContext context) {
        Class<?> mapperType = context.getMapperType();
        MapperDeclaration declaration = MapperUtil.getMapperDeclaration(mapperType);
        return buildSelectFields(declaration, null) + " WHERE _t.`" + declaration.getPkColumnName() + "` = #{id}";
    }

    /**
     * 自定义 SQL 查询
     * @param params
     *      参数
     * @return SQL 语句
     */
    public String queryBySql(Map<String, Object> params, ProviderContext context) {
        return params.get("sql").toString();
    }

    /**
     * 自定义 SQL 执行
     * @param params
     *      参数
     * @return SQL 语句
     */
    public String executeSql(Map<String, Object> params, ProviderContext context) {
        return params.get("sql").toString();
    }

    /**
     * 根据主键生成删除 SQL 语句
     * @param id
     *      主键
     * @return SQL 语句
     */
    public String deleteById(Serializable id, ProviderContext context) {
        Class<?> mapperType = context.getMapperType();
        MapperDeclaration declaration = MapperUtil.getMapperDeclaration(mapperType);
        return "DELETE FROM `" +
                declaration.getTableName() + "` AS _t WHERE _t.`" +
                declaration.getPkColumnName() + "` = #{id}";
    }

    /**
     * 根据主键集合生成批量删除 SQL 语句
     * @param ids
     *      主键集合
     * @return SQL 语句
     */
    public String deleteByIds(Collection<Serializable> ids, ProviderContext context) {
        Class<?> mapperType = context.getMapperType();
        MapperDeclaration declaration = MapperUtil.getMapperDeclaration(mapperType);
        StringBuilder sql = new StringBuilder("DELETE FROM `")
                .append(declaration.getTableName())
                .append("` WHERE `")
                .append(declaration.getPkColumnName())
                .append("` IN (");
        for (int i = 0; i < ids.size(); i++) {
            sql.append("#{ids[").append(i).append("]}");
            if (i < ids.size() - 1) {
                sql.append(", ");
            }
        }
        sql.append(")");
        return sql.toString();
    }

    /**
     * 根据 Where 条件生成删除 SQL 语句
     * @param where
     *      删除条件
     * @return SQL 语句
     */
    public String deleteByWhere(Where where, ProviderContext context) {
        Class<?> mapperType = context.getMapperType();
        MapperDeclaration declaration = MapperUtil.getMapperDeclaration(mapperType);
        StringBuilder sql = new StringBuilder("DELETE FROM `")
                .append(declaration.getTableName()).append("`").append(" AS _t");
        sql.append(buildWherePart(where, false));
        return sql.toString();
    }

    /**
     * 根据主键生成更新 SQL 语句
     * @param params
     *      参数
     * @return SQL 语句
     */
    public String updateById(Map<String, Object> params, ProviderContext context) {
        PO record = (PO) params.get("record");
        Class<?> mapperType = context.getMapperType();
        MapperDeclaration mapperDeclaration = MapperUtil.getMapperDeclaration(mapperType);
        StringBuilder sql = new StringBuilder("UPDATE `")
                .append(mapperDeclaration.getTableName())
                .append("` SET ");
        List<ColumnDeclaration> columnDeclarations = mapperDeclaration.getColumnDeclarations();
        for (int i = 0; i < columnDeclarations.size(); i++) {
            ColumnDeclaration columnDeclaration = columnDeclarations.get(i);
            if (columnDeclaration.isJson()){
                sql.append("`").append(columnDeclaration.getColumnName()).append("` = #{record.")
                        .append(columnDeclaration.getFieldName())
                        .append(", typeHandler=ink.icoding.smartmybatis.mapper.handlers.SmartJsonTypeHandler}");
            }else{
                sql.append("`").append(columnDeclaration.getColumnName()).append("` = #{record.")
                        .append(columnDeclaration.getFieldName()).append("}");
            }
            if (i < columnDeclarations.size() - 1) {
                sql.append(", ");
            }
        }
        sql.append(" WHERE `").append(mapperDeclaration.getPkColumnName())
                .append("` = #{record.").append(mapperDeclaration.getPkName()).append("}");
        return sql.toString();
    }


    /**
     * 构建 Where 部分 SQL 语句
     * @param where
     *      查询条件表达式列表
     * @return Where 部分 SQL 语句
     */
    private String buildWherePart(Where where, boolean inOn) {
        if (null == where){
            return "";
        }
        List<ComparisonExpression<?>> expressions = where.getExpressions();
        int limitSize = where.getLimitSize();
        if ((null == expressions || expressions.isEmpty()) && limitSize == 0){
            return "";
        }
        Map<String, AliasMapping<?>> aliasMappings = where.getAliasMappings();
        Map<Class<? extends PO>, String> aliasMappingMap = new HashMap<>();
        if (null != aliasMappings && !aliasMappings.isEmpty()){
            for (AliasMapping<?> aliasMapping : aliasMappings.values()) {
                aliasMappingMap.put(aliasMapping.getEntityClass(), aliasMapping.getAlias());
            }
        }
        StringBuilder wherePart = new StringBuilder();
        if (null != expressions && !expressions.isEmpty()){
            wherePart.append(" ").append(inOn ? "ON" : "WHERE");
            for (int i = 0; i < expressions.size(); i++) {
                ComparisonExpression<?> expression = expressions.get(i);
                Link link = expression.getLink();
                if (null != link && i > 0) {
                    wherePart.append(" ").append(link.name()).append(" ");
                }
                SFunction<? extends PO, ?> func = expression.getFunc();
                Field field = LambdaFieldUtil.getField(func);
                // 取出实体类Class
                Class<? extends PO> poClass = LambdaFieldUtil.getPoClass(func);
                TableField tableField = field.getAnnotation(TableField.class);
                C comparison = expression.getComparison();
                String alias = aliasMappingMap.getOrDefault(poClass, "_t");
                if (null != tableField && !tableField.exist()){
                    // 可能是关联表字段, 需要处理
                    if (tableField.link() == PO.class){
                        throw new IllegalArgumentException("Field " + field.getName()
                                + " is marked as non-existent in table, but no link entity specified.");
                    }
                    wherePart.append(" ").append(where.getGlobalWhereAliasValue(field)).append(" ");
                }else{
                    ColumnDeclaration columnDeclaration = MapperUtil.getColumnDeclaration(field);
                    wherePart.append(" ").append(alias).append(".").append("`").append(columnDeclaration.getColumnName()).append("` ");
                }
                Object value = expression.getValue();
                if (null == value){
                    if (comparison == C.EQ || comparison == C.equals){
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

                // value 是否是 SFunction
                if (value instanceof SFunction){
                    SFunction<? extends PO, ?> valueFunc = (SFunction<? extends PO, ?>) value;
                    Field valueField = LambdaFieldUtil.getField(valueFunc);
                    ColumnDeclaration valueColumnDeclaration = MapperUtil.getColumnDeclaration(valueField);
                    // 取出实体类Class
                    Class<? extends PO> valuePoClass = LambdaFieldUtil.getPoClass(valueFunc);
                    String valueAlias = aliasMappingMap.getOrDefault(valuePoClass, "_t");
                    wherePart.append(valueAlias).append(".").append("`")
                            .append(valueColumnDeclaration.getColumnName()).append("` ");
                    where.putGlobalWhere(valueField, valueAlias);
                    continue;
                }

                if (comparison == C.IN || comparison == C.NOT_IN || comparison == C.in || comparison == C.notIn) {
                    wherePart.append(" (");
                    // 判断 value 是否为 数组, 如果是数组则转换成 List
                    if (value.getClass().isArray()) {
                        value = Arrays.asList((Object[]) value);
                    }
                    if (!(value instanceof Collection)) {
                        throw new IllegalArgumentException("Value for IN or NOT IN comparison must be a Collection or Array, but got: "
                                + value.getClass().getName());
                    }
                    Collection<?> valueList = (Collection<?>) value;
                    for (int cl = 0; cl < valueList.size(); cl++) {
                        wherePart.append("#{expressions[").append(i)
                                .append("].value[").append(cl).append("]}");
                        if (cl < valueList.size() - 1) {
                            wherePart.append(", ");
                        }
                    }
                    wherePart.append(") ");
                } else {
                    if (inOn){
                        wherePart.append("#{aliasMappings.").append(alias).append(".onWhere.expressions[").append(i).append("].value} ");
                    }else{
                        wherePart.append("#{expressions[").append(i).append("].value} ");
                    }
                    if (comparison == C.LIKE || comparison == C.NOT_LIKE ||
                            comparison == C.like || comparison == C.notLike) {
                        // 如果是模糊查询, 则在值前后添加 %
                        String strValue = value.toString();
                        if (!strValue.contains("%")) {
                            strValue = "%" + strValue + "%";
                            expression.setValue(strValue);
                        }
                    }
                }
            }
        }

        StringBuilder orderByPart = new StringBuilder();
        if (null != where.getSortExpressions() && !where.getSortExpressions().isEmpty()){
            orderByPart.append(" ORDER BY ");
            List<SortExpression<?>> sortExpressions = where.getSortExpressions();
            for (int i = 0; i < sortExpressions.size(); i++) {
                SortExpression<?> sortExpression = sortExpressions.get(i);
                SFunction<? extends PO, ?> func = sortExpression.getFunc();
                Field field = LambdaFieldUtil.getField(func);
                ColumnDeclaration columnDeclaration = MapperUtil.getColumnDeclaration(field);
                orderByPart.append("`").append(columnDeclaration.getColumnName()).append("` ")
                        .append(sortExpression.getDirection().name());
                if (i < sortExpressions.size() - 1) {
                    orderByPart.append(", ");
                }
            }
            wherePart.append(orderByPart);
        }

        StringBuilder limitPart = new StringBuilder();
        if (limitSize > 0){
            limitPart.append(" LIMIT ").append(where.getLimitStart()).append(", ").append(where.getLimitSize());
        }



        return wherePart.toString() + limitPart;
    }

    /**
     * 构建 SELECT 字段部分 SQL 语句
     * @param mapperDeclaration
     *      映射声明
     * @return SELECT 字段部分 SQL 语句
     */
    private String buildSelectFields(MapperDeclaration mapperDeclaration, Where where) {
        StringBuilder sql = new StringBuilder("SELECT ");
        for (ColumnDeclaration columnDeclaration : mapperDeclaration.getColumnDeclarations()) {
            sql.append("_t.`").append(columnDeclaration.getColumnName()).append("` AS ");
            sql.append(columnDeclaration.getFieldName()).append(", ");
        }
        // ID
        sql.append("_t.`").append(mapperDeclaration.getPkColumnName()).append("` AS ").append(mapperDeclaration.getPkName());
        Map<String, AliasMapping<?>> aliasMappings = null;
        if (null != where){
            aliasMappings = where.getAliasMappings();
        }
        if (aliasMappings == null || aliasMappings.isEmpty()){
            sql.append(" FROM `").append(mapperDeclaration.getTableName()).append("`").append(" AS _t");
            return sql.toString();
        }

        // 有别名连接时, 继续处理别名连接部分
        aliasMappings.values().forEach(aliasMapping -> {
            aliasMapping.getSelectFields().forEach(sField -> {
                Field field = LambdaFieldUtil.getField(sField);
                TableField tableField = field.getAnnotation(TableField.class);
                ColumnDeclaration columnDeclaration = null;
                if (null != tableField && !tableField.exist() && tableField.link() != PO.class){
                    // 如果是非数据库表字段, 且有关联表, 则查询关联表的字段
                    String linkFieldName = tableField.linkField();
                    if (linkFieldName == null || linkFieldName.isEmpty()){
                        linkFieldName = field.getName();
                    }
                    try {
                        Field declaredField = tableField.link().getDeclaredField(linkFieldName);
                        columnDeclaration = MapperUtil.getColumnDeclaration(declaredField);
                    } catch (NoSuchFieldException e) {
                        throw new IllegalArgumentException("Linked field " + linkFieldName + " not found in class "
                                + tableField.link().getName() + " for field " + field.getName());
                    }
                }else{
                    throw new IllegalArgumentException("Select field " + field.getName()
                            + " is not a linked field in alias mapping for alias "
                            + aliasMapping.getAlias());
                }
                sql.append(", ")
                        .append(aliasMapping.getAlias()).append(".`")
                        .append(columnDeclaration.getColumnName()).append("` AS ")
                        .append(field.getName());
                where.putGlobalWhere(field, aliasMapping.getAlias() + ".`" + columnDeclaration.getColumnName() + "`");
            });
        });

        // 处理别名连接
        sql.append(" FROM `").append(mapperDeclaration.getTableName()).append("`").append(" AS _t");
        for (AliasMapping<?> aliasMapping : aliasMappings.values()) {
            MapperDeclaration declaration = MapperUtil.getMapperDeclarationByPoClass(aliasMapping.getEntityClass());

            sql.append(" ").append(aliasMapping.getType())
                    .append(" ").append("`").append(declaration.getTableName()).append("`")
                    .append(" ").append(aliasMapping.getAlias());
            // 构建 ON 条件
            Where onWhere = aliasMapping.getOnWhere();
            if (null != onWhere){
                onWhere.setAliasMappings(aliasMappings);
                sql.append(" ").append(buildWherePart(onWhere, true));
            }
        }
        return sql.toString();
    }
}
