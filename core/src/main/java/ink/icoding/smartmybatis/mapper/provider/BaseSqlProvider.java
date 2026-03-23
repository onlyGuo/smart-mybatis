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
import org.springframework.util.StringUtils;

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

    public String selectWithRelationsByWhere(Where where, ProviderContext context){
        Class<?> mapperType = context.getMapperType();
        return buildSelectFields(MapperUtil.getMapperDeclaration(mapperType), where, true) + buildWherePart(where, false);
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
    private String buildWherePart(Where where, boolean inOn){
        return buildWherePart(where, inOn, null);
    }

    /**
     * 构建 Where 部分 SQL 语句
     * @param where
     *      查询条件表达式列表
     * @return Where 部分 SQL 语句
     */
    private String buildWherePart(Where where, boolean inOn, String parentParamPrefix) {
        if (null == where){
            return "";
        }
        if (null == parentParamPrefix){
            parentParamPrefix = "";
        }
        List<Expression<?>> expressions = where.getExpressions();
        int limitSize = where.getLimitSize();
        if ((null == expressions || expressions.isEmpty()) && limitSize == 0){
            return "";
        }
        Map<String, AliasMapping<?>> aliasMappings = where.getAliasMappings();
        Map<String, String> aliasMappingMap = new HashMap<>();
        if (null != aliasMappings && !aliasMappings.isEmpty()){
            for (AliasMapping<?> aliasMapping : aliasMappings.values()) {
                aliasMappingMap.put(aliasMapping.getEntityClass().getName(), aliasMapping.getAlias());
            }
        }
        StringBuilder wherePart = new StringBuilder();
        if (null != expressions && !expressions.isEmpty()){
            wherePart.append(" ").append(inOn ? "ON" : "WHERE");
            for (int i = 0; i < expressions.size(); i++) {
                Expression<?> expression = expressions.get(i);
                if (expression instanceof WhereExpression){
                    WhereExpression whereExpression = (WhereExpression) expression;
                    Where subWhere = whereExpression.getWhere();
                    subWhere.setAliasMappings(aliasMappings);
                    String subWhereSql = buildWherePart(subWhere, false, parentParamPrefix + "expressions[" + i + "].where.");
                    if (subWhereSql.isEmpty()){
                        continue;
                    }
                    if (subWhereSql.trim().startsWith("WHERE")){
                        subWhereSql = subWhereSql.trim().substring(5);
                    }
                    if (i > 0){
                        wherePart.append(" ").append(whereExpression.getLink().name()).append(" ");
                    }
                    wherePart.append(" (").append(subWhereSql).append(") ");
                    continue;
                }
                ComparisonExpression<?> comparisonExpression = (ComparisonExpression<?>) expression;

                Link link = comparisonExpression.getLink();
                if (null != link && i > 0) {
                    wherePart.append(" ").append(link.name()).append(" ");
                }
                SFunction<? extends PO, ?> func = comparisonExpression.getFunc();
                Field field = LambdaFieldUtil.getField(func);
                Class<? extends PO> poClass = LambdaFieldUtil.getPoClass(func);
                C comparison = comparisonExpression.getComparison();
                String alias = aliasMappingMap.getOrDefault(poClass.getName(), "_t");

                String fieldSqlRef = resolveFieldSqlRef(where, field, poClass, aliasMappingMap);
                if (StringUtils.hasText(fieldSqlRef)) {
                    wherePart.append(" ").append(fieldSqlRef).append(" ");
                } else {
                    wherePart.append(" ").append(alias).append(".").append("`")
                            .append(MapperUtil.getColumnDeclaration(field).getColumnName()).append("` ");
                }
                Object value = comparisonExpression.getValue();
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

                if (value instanceof SFunction){
                    SFunction<? extends PO, ?> valueFunc = (SFunction<? extends PO, ?>) value;
                    Field valueField = LambdaFieldUtil.getField(valueFunc);
                    Class<? extends PO> valuePoClass = LambdaFieldUtil.getPoClass(valueFunc);
                    String valueRef = resolveFieldSqlRef(where, valueField, valuePoClass, aliasMappingMap);
                    if (!StringUtils.hasText(valueRef)) {
                        ColumnDeclaration valueColumnDeclaration = MapperUtil.getColumnDeclaration(valueField);
                        String valueAlias = aliasMappingMap.getOrDefault(valuePoClass.getName(), "_t");
                        valueRef = valueAlias + ".`" + valueColumnDeclaration.getColumnName() + "`";
                    }
                    wherePart.append(valueRef).append(" ");
                    continue;
                }

                if (comparison == C.IN || comparison == C.NOT_IN || comparison == C.in || comparison == C.notIn) {
                    wherePart.append(" (");
                    // 判断 value 是否为 数组, 如果是数组则转换成 List
                    if (value.getClass().isArray()) {
                        value = Arrays.asList((Object[]) value);
                    }
                    if (!(value instanceof Collection<?>)) {
                        throw new IllegalArgumentException("Value for IN or NOT IN comparison must be a Collection or Array, but got: "
                                + value.getClass().getName());
                    } else {
                        // 如果是 Collection, 还需要判断是否为空, 如果为空则替换成一个永远不成立的条件
                        if (((Collection<?>) value).isEmpty()){

                            if (comparison == C.IN || comparison == C.in){
                                wherePart.setLength(wherePart.length() - "IN (".length() - 1);
                                wherePart.setLength(wherePart.toString().trim().lastIndexOf(" ") + 1);
                                // 永远不成立的条件
                                wherePart.append(" 1=0");
                            }else {
                                wherePart.setLength(wherePart.length() - "NOT IN (".length() - 1);
                                wherePart.setLength(wherePart.toString().trim().lastIndexOf(" ") + 1);
                                // 永远成立的条件
                                wherePart.append(" 1=1");
                            }
                        }else if (value instanceof Set){
                            // 如果是 Set, 需要转换成 List, 因为 Set 没有 get(int index) 方法
                            value = new ArrayList<>((Set<?>) value);
                            comparisonExpression.setValue(value);
                        }
                    }
                    Collection<?> valueList = (Collection<?>) value;
                    for (int cl = 0; cl < valueList.size(); cl++) {
                        wherePart.append("#{").append(parentParamPrefix).append("expressions[").append(i)
                                .append("].value[").append(cl).append("]}");
                        if (cl < valueList.size() - 1) {
                            wherePart.append(", ");
                        }
                    }
                    if (!valueList.isEmpty()){
                        wherePart.append(") ");
                    }
                } else {
                    if (inOn){
                        wherePart.append("#{aliasMappings.").append(alias).append(".onWhere.expressions[").append(i).append("].value} ");
                    }else{
                        wherePart.append("#{").append(parentParamPrefix).append("expressions[").append(i).append("].value} ");
                    }
                    if (comparison == C.LIKE || comparison == C.NOT_LIKE ||
                            comparison == C.like || comparison == C.notLike) {
                        // 如果是模糊查询, 则在值前后添加 %
                        String strValue = value.toString();
                        if (!strValue.contains("%")) {
                            strValue = "%" + strValue + "%";
                            comparisonExpression.setValue(strValue);
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
                Class<? extends PO> poClass = LambdaFieldUtil.getPoClass(func);
                String orderRef = resolveFieldSqlRef(where, field, poClass, aliasMappingMap);
                if (!StringUtils.hasText(orderRef)) {
                    ColumnDeclaration columnDeclaration = MapperUtil.getColumnDeclaration(field);
                    String orderAlias = aliasMappingMap.getOrDefault(poClass.getName(), "_t");
                    orderRef = orderAlias + ".`" + columnDeclaration.getColumnName() + "`";
                }
                orderByPart.append(orderRef).append(" ")
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
        return buildSelectFields(mapperDeclaration, where, false);
    }

    /**
     * 构建 SELECT 字段部分 SQL 语句
     * @param mapperDeclaration
     *      映射声明
     * @return SELECT 字段部分 SQL 语句
     */
    private String buildSelectFields(MapperDeclaration mapperDeclaration, Where where, boolean inRelation) {
        StringBuilder sql = new StringBuilder("SELECT ");
        int inRelationIndex = 0;
        Map<Class<? extends PO>, String> relationAliasMap = new LinkedHashMap<>();
        for (ColumnDeclaration columnDeclaration : mapperDeclaration.getColumnDeclarations(inRelation)) {
            if (columnDeclaration.isLink()){
                TableField tableField = columnDeclaration.getAnnotation();
                Class<? extends PO> linkClass = tableField == null ? PO.class : tableField.link();
                String alias = columnDeclaration.getAlias();
                if (!StringUtils.hasText(alias) && linkClass != PO.class) {
                    alias = relationAliasMap.get(linkClass);
                }
                if (!StringUtils.hasText(alias)){
                    alias = "_rel" + inRelationIndex++;
                }
                if (linkClass != PO.class && !relationAliasMap.containsKey(linkClass)) {
                    relationAliasMap.put(linkClass, alias);
                }
                sql.append(alias).append(".`").append(columnDeclaration.getColumnName()).append("` AS ");
                if (where != null && columnDeclaration.getField() != null) {
                    where.putGlobalWhere(columnDeclaration.getField(), alias + ".`" + columnDeclaration.getColumnName() + "`");
                }
            }else{
                sql.append("_t.`").append(columnDeclaration.getColumnName()).append("` AS ");
            }
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
            if (inRelation) {
                appendAutoRelationJoins(sql, mapperDeclaration, relationAliasMap);
            }
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

        if (inRelation) {
            appendAutoRelationJoins(sql, mapperDeclaration, relationAliasMap);
        }

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

    private void appendAutoRelationJoins(StringBuilder sql,
                                         MapperDeclaration mapperDeclaration,
                                         Map<Class<? extends PO>, String> relationAliasMap) {
        if (relationAliasMap.isEmpty()) {
            return;
        }
        Set<String> joinedAliases = new HashSet<>();
        for (ColumnDeclaration columnDeclaration : mapperDeclaration.getColumnDeclarations(true)) {
            if (!columnDeclaration.isLink()) {
                continue;
            }
            TableField tableField = columnDeclaration.getAnnotation();
            if (tableField == null || tableField.link() == PO.class) {
                continue;
            }
            String alias = relationAliasMap.get(tableField.link());
            if (!StringUtils.hasText(alias) || !joinedAliases.add(alias)) {
                continue;
            }
            MapperDeclaration relDeclaration = MapperUtil.getMapperDeclarationByPoClass(tableField.link());
            String relJoinColumn = resolveRelationJoinColumn(tableField, relDeclaration);
            String baseJoinColumn = resolveBaseJoinColumn(mapperDeclaration, columnDeclaration);
            sql.append(" LEFT JOIN `").append(relDeclaration.getTableName()).append("` ")
                    .append(alias)
                    .append(" ON ")
                    .append(alias).append(".`").append(relJoinColumn).append("`")
                    .append(" = _t.`").append(baseJoinColumn).append("`");
        }
    }

    private String resolveRelationJoinColumn(TableField tableField, MapperDeclaration relDeclaration) {
        if (StringUtils.hasText(tableField.value())) {
            return tableField.value();
        }
        return relDeclaration.getPkColumnName();
    }

    private String resolveBaseJoinColumn(MapperDeclaration mapperDeclaration,
                                         ColumnDeclaration linkColumn) {
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
        ColumnDeclaration declaration = MapperUtil.getFieldDeclarationByPoClass(mapperDeclaration.getPoClass(), self);
        return declaration.getColumnName();
    }

    private String resolveFieldSqlRef(Where where,
                                      Field field,
                                      Class<? extends PO> poClass,
                                      Map<String, String> aliasMappingMap) {
        String cached = where.getGlobalWhereAliasValue(field);
        if (StringUtils.hasText(cached)) {
            return cached;
        }
        TableField tableField = field.getAnnotation(TableField.class);
        if (tableField != null && !tableField.exist()) {
            if (tableField.link() == PO.class) {
                return null;
            }
            String alias = aliasMappingMap.get(tableField.link().getName());
            if (!StringUtils.hasText(alias)) {
                return null;
            }
            String linkFieldName = tableField.linkField();
            if (!StringUtils.hasText(linkFieldName)) {
                linkFieldName = field.getName();
            }
            try {
                Field declaredField = tableField.link().getDeclaredField(linkFieldName);
                ColumnDeclaration declaration = MapperUtil.getColumnDeclaration(declaredField);
                return alias + ".`" + declaration.getColumnName() + "`";
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException("Linked field " + linkFieldName + " not found in class "
                        + tableField.link().getName() + " for field " + field.getName(), e);
            }
        }
        ColumnDeclaration declaration = MapperUtil.getColumnDeclaration(field);
        String alias = aliasMappingMap.getOrDefault(poClass.getName(), "_t");
        return alias + ".`" + declaration.getColumnName() + "`";
    }
}
