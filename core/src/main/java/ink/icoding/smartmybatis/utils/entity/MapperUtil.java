package ink.icoding.smartmybatis.utils.entity;

import ink.icoding.smartmybatis.conf.NamingConvention;
import ink.icoding.smartmybatis.conf.SmartConfigHolder;
import ink.icoding.smartmybatis.entity.po.PO;
import ink.icoding.smartmybatis.entity.po.enums.ID;
import ink.icoding.smartmybatis.entity.po.enums.PrimaryGenerateType;
import ink.icoding.smartmybatis.entity.po.enums.TableField;
import ink.icoding.smartmybatis.entity.po.enums.TableName;
import ink.icoding.smartmybatis.mapper.base.SmartMapper;
import ink.icoding.smartmybatis.utils.NamingUtil;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Smart Mybatis Mapper 工具类
 * @author gsk
 */
public class MapperUtil {

    /**
     * Mapper 声明缓存
     */
    private static final Map<Class<?>, MapperDeclaration> MAPPER_DECLARATION_MAP = new HashMap<>();

    /**
     * PO 类对应的 Mapper 声明缓存
     */
    private static final Map<Class<?>, MapperDeclaration> PO_MAPPER_DECLARATION_MAP = new HashMap<>();
    /**
     * 字段列声明缓存
     */
    private static final Map<String, ColumnDeclaration> FIELD_COLUMN_DECLARATION_MAP = new HashMap<>();

    /**
     * 获取 Mapper 声明的信息
     * @param mapperType mapper 类
     * @return Mapper 声明信息
     */
    public static MapperDeclaration getMapperDeclaration(Class<?> mapperType) {
        if (MAPPER_DECLARATION_MAP.containsKey(mapperType)) {
            return MAPPER_DECLARATION_MAP.get(mapperType);
        }
        Class<? extends PO> classType = null;
        Type[] genericInterfaces = mapperType.getGenericInterfaces();
        for (Type type : genericInterfaces) {
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                // 获取泛型参数
                Type[] actualTypeArguments = pt.getActualTypeArguments();
                for (Type arg : actualTypeArguments) {
                    // 判断是否是 PO 的子类
                    if (arg instanceof Class) {
                        Class<?> argClass = (Class<?>) arg;
                        if (PO.class.isAssignableFrom(argClass)) {
                            classType = (Class<? extends PO>) argClass;
                            break;
                        }
                    }
                }
            }
        }

        if (classType == null) {
            throw new IllegalArgumentException("Failed to determine PO class for mapper, " +
                    "not found generic type extends PO at:" + mapperType.getName());
        }
        // 字段
        MapperDeclaration declaration = getMapperDeclarationByPoClass(classType);
        MAPPER_DECLARATION_MAP.put(mapperType, declaration);
        return declaration;
    }




    /**
     * 应用表名到 Mapper 声明
     * @param declaration Mapper 声明
     */
    private static void applyTableName(MapperDeclaration declaration){
        Class<? extends PO> poClass = declaration.getPoClass();
        if (null == poClass){
            throw new IllegalArgumentException("PO class is null");
        }
        TableName tableName = poClass.getAnnotation(TableName.class);
        String name = null;
        NamingConvention namingConvention = SmartConfigHolder.config().getNamingConvention();
        if (null == tableName || !StringUtils.hasText(tableName.value())){
            if (namingConvention == NamingConvention.UNDERLINE_LOWER){
                name = SmartConfigHolder.config().getTablePrefix().toLowerCase() + NamingUtil.camelToUnderlineLower(poClass.getSimpleName());
            } else if (namingConvention == NamingConvention.UNDERLINE_UPPER){
                name = SmartConfigHolder.config().getTablePrefix().toUpperCase() + NamingUtil.camelToUnderlineUpper(poClass.getSimpleName());
            } else if (namingConvention == NamingConvention.AS_IS){
                name = SmartConfigHolder.config().getTablePrefix() + poClass.getSimpleName();
            } else{
                throw new IllegalArgumentException("Unknown naming convention:" + namingConvention +", for PO class:" + poClass.getName());
            }
        }else{
            String prefix = SmartConfigHolder.config().getTablePrefix();
            name = tableName.value();
            if (namingConvention == NamingConvention.UNDERLINE_LOWER){
                prefix = prefix.toLowerCase();
                name = name.toLowerCase();
            }else if (namingConvention == NamingConvention.UNDERLINE_UPPER){
                prefix = prefix.toUpperCase();
                name = name.toUpperCase();
            }
            if (!name.startsWith(prefix)){
                name = prefix + name;
            }
        }
        declaration.setTableName(name);
    }

    public static ColumnDeclaration getColumnDeclaration(Field field){
        ColumnDeclaration columnDeclaration = FIELD_COLUMN_DECLARATION_MAP.get(field.getName());
        if (null == columnDeclaration){
            columnDeclaration = new ColumnDeclaration();
            columnDeclaration.setField(field);
            applyFieldColumnName(columnDeclaration);
            FIELD_COLUMN_DECLARATION_MAP.put(field.getName(), columnDeclaration);
        }
        return columnDeclaration;
    }

    /**
     * 应用列信息声明
     * @param declaration 列声明
     */
    public static void applyFieldColumnName(ColumnDeclaration declaration){
        Field field = declaration.getField();
        TableField tableField = field.getAnnotation(TableField.class);
        declaration.setColumnName(getFieldColumnName(tableField, field));
        declaration.setFieldName(field.getName());
        declaration.setJson(tableField != null && tableField.json());
        declaration.setColumnType(NamingUtil.javaTypeToSqlType(field.getType(), tableField));
        declaration.setAnnotation(tableField);
        if (null != tableField){
            declaration.setDescription(tableField.description());
        }
    }

    /**
     * 获取字段对应的列名
     * @param tableField 字段注解
     * @param field 字段
     * @return 列名
     */
    private static String getFieldColumnName(TableField tableField, Field field){
        String columnName = null;
        if (null != tableField && null != tableField.value() && !tableField.value().isEmpty()){
            columnName = tableField.value();
        }
        NamingConvention namingConvention = SmartConfigHolder.config().getNamingConvention();
        if (namingConvention == NamingConvention.UNDERLINE_LOWER){
            columnName = NamingUtil.camelToUnderlineLower(field.getName());
        } else if (namingConvention == NamingConvention.UNDERLINE_UPPER){
            columnName = NamingUtil.camelToUnderlineUpper(field.getName());
        } else if (namingConvention == NamingConvention.AS_IS){
            columnName = field.getName();
        } else{
            throw new IllegalArgumentException("Unknown naming convention:" + namingConvention
                    + ", for field:" + field.getName());
        }
        return columnName;
    }

    /**
     * 生成数据表
     * @param smartMapper Smart Mapper 实例
     * @param declaration Mapper 声明
     */
    public static <T extends PO> void generateTable(SmartMapper<T> smartMapper, MapperDeclaration declaration) {
        StringBuilder sb = new StringBuilder("CREATE TABLE `").append(declaration.getTableName()).append("` (\n");
        // 主键字段
        sb.append("`").append(declaration.getPkColumnName()).append("` ")
                .append(NamingUtil.javaTypeToSqlType(declaration.getPkClass(), declaration.getPkAnnotation()))
                .append(" NOT NULL ");
        if (declaration.getPkGenerateType() == PrimaryGenerateType.AUTO){
            sb.append("AUTO_INCREMENT ");
        }
        sb.append("PRIMARY KEY, ");
        // 其他字段
        for (ColumnDeclaration columnDeclaration : declaration.getColumnDeclarations()) {
            sb.append("`").append(columnDeclaration.getColumnName()).append("` ")
                    .append(columnDeclaration.getColumnType());
            // 注释
            if (null != columnDeclaration.getDescription() && !columnDeclaration.getDescription().isEmpty()) {
                sb.append(" COMMENT '").append(columnDeclaration.getDescription()).append("'");
            }
            sb.append(" DEFAULT NULL, \n");
        }
        // 去掉最后的逗号和换行
        sb.setLength(sb.length() - 3);
        sb.append("\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
        String createTableSql = sb.toString();
        smartMapper.executeSql(createTableSql);
    }

    /**
     * 更新数据表结构
     * @param smartMapper      Smart Mapper 实例
     * @param declaration      Mapper 声明（包含表名、主键、字段声明等）
     * @param existingColumns  已存在的列信息（来自数据库元数据）
     */
    public static <T extends PO> void updateTable(SmartMapper<T> smartMapper,
                                                  MapperDeclaration declaration,
                                                  List<ColumnDeclaration> existingColumns) {
        String tableName = declaration.getTableName();
        String pkName = declaration.getPkColumnName();

        // 1. 建立索引：已存在列、声明列
        Map<String, ColumnDeclaration> existMap = new HashMap<>();
        if (existingColumns != null) {
            for (ColumnDeclaration c : existingColumns) {
                existMap.put(c.getColumnName().toLowerCase(), c);
            }
        }

        List<ColumnDeclaration> declared = declaration.getColumnDeclarations() != null
                ? declaration.getColumnDeclarations() : Collections.emptyList();
        Map<String, ColumnDeclaration> declaredMap = declared.stream()
                .collect(Collectors.toMap(c -> c.getColumnName().toLowerCase(Locale.ROOT),
                        c -> c, (a, b) -> a));

        // 2. 生成 ALTER 子句
        List<String> alterClauses = new ArrayList<>();

        // 2.1 若数据库缺失主键列（保守补齐）
        boolean pkExists = existMap.containsKey(pkName.toLowerCase());
        if (!pkExists) {
            String pkType = NamingUtil.javaTypeToSqlType(declaration.getPkClass(), declaration.getPkAnnotation());
            StringBuilder pkDef = new StringBuilder();
            pkDef.append("ADD COLUMN `").append(pkName).append("` ")
                    .append(pkType).append(" NOT NULL ");
            if (declaration.getPkGenerateType() == PrimaryGenerateType.AUTO) {
                pkDef.append("AUTO_INCREMENT ");
            }
            // 添加主键列
            alterClauses.add(pkDef.toString());
            // 添加主键约束
            alterClauses.add("ADD PRIMARY KEY (`" + pkName + "`)");
        }

        // 2.2 新增/修改非主键列
        for (ColumnDeclaration declCol : declared) {
            String name = declCol.getColumnName();
            if (pkName.equalsIgnoreCase(name)) {
                // 主键列已在上面处理
                continue;
            }
            String key = name.toLowerCase();
            ColumnDeclaration existCol = existMap.get(key);

            if (existCol == null) {
                // 新增列
                alterClauses.add("ADD COLUMN " + buildColumnDefinition(declCol));
            } else {
                // 是否需要修改：比较类型、注释（可按需扩展更多属性）
                boolean typeChanged = !normalizeType(declCol.getColumnType())
                        .equals(normalizeType(existCol.getColumnType()));
                boolean commentChanged = !normalizeComment(declCol.getDescription())
                        .equals(normalizeComment(existCol.getDescription()));

                if (typeChanged || commentChanged) {
                    alterClauses.add("MODIFY COLUMN " + buildColumnDefinition(declCol));
                }
            }
        }

        // 2.3 删除多余列（排除主键列）
//        for (ColumnDeclaration existCol : existMap.values()) {
//            String name = existCol.getColumnName();
//            if (pkName.equalsIgnoreCase(name)) continue;
//            if (!declaredMap.containsKey(name.toLowerCase())) {
//                alterClauses.add("DROP COLUMN `" + name + "`");
//            }
//        }

        // 3. 执行 SQL
        if (alterClauses.isEmpty()) {
            return; // 无需变更
        }
        String sql = "ALTER TABLE `" + tableName + "` " + String.join(", ", alterClauses) + ";";
        smartMapper.executeSql(sql);
    }

    /**
     * 构造列定义（与 generateTable 风格一致：默认 DEFAULT NULL + 可选 COMMENT）
     * 示例返回：`name` VARCHAR(64) COMMENT '说明' DEFAULT NULL
     */
    private static String buildColumnDefinition(ColumnDeclaration column) {
        StringBuilder sb = new StringBuilder();
        sb.append("`").append(column.getColumnName()).append("` ")
                .append(column.getColumnType());

        // 注释
        if (column.getDescription() != null && !column.getDescription().isEmpty()) {
            sb.append(" COMMENT '").append(escapeSqlComment(column.getDescription())).append("'");
        }

        // 与 generateTable 保持一致：统一默认 NULL
        sb.append(" DEFAULT NULL");
        return sb.toString();
    }

    /**
     * 规范化类型字符串：忽略大小写与多余空白，便于比较
     */
    private static String normalizeType(String type) {
        if (type == null) {
            return "";
        }
        return type.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    /**
     * 规范化注释（null 等同于空串）
     */
    private static String normalizeComment(String comment) {
        return comment == null ? "" : comment;
    }

    /**
     * 转义注释中的单引号，避免 SQL 语法错误
     */
    private static String escapeSqlComment(String comment) {
        return comment.replace("'", "''");
    }

    public static <T extends PO> Object getFieldValue(T record, String fieldName) {
        try {
            Field field = record.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(record);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to get field value: " + fieldName, e);
        }
    }

    public static <T extends PO> void setFieldValue(T record, String fieldName, Object replace) {
        try {
            Field field = record.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(record, replace);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set field value: " + fieldName, e);
        }
    }

    public static MapperDeclaration getMapperDeclarationByPoClass(Class<? extends PO> poClass) {
        if (PO_MAPPER_DECLARATION_MAP.containsKey(poClass)) {
            return PO_MAPPER_DECLARATION_MAP.get(poClass);
        }
        for (MapperDeclaration declaration : MAPPER_DECLARATION_MAP.values()) {
            if (declaration.getPoClass().equals(poClass)) {
                PO_MAPPER_DECLARATION_MAP.put(poClass, declaration);
                return declaration;
            }
        }
        // 未找到对应 Mapper 声明, 构建
        MapperDeclaration declaration = new MapperDeclaration();
        declaration.setPoClass(poClass);
        Field[] declaredFields = declaration.getPoClass().getDeclaredFields();
        List<ColumnDeclaration> columnDeclarations = new ArrayList<>();
        for (Field field : declaredFields) {
            ID id = field.getAnnotation(ID.class);
            if (null != id){
                if (declaration.getPkName() != null){
                    throw new IllegalArgumentException("Multiple primary key fields found in mapper:" +
                            declaration.getPoClass().getName() + ", fields:" + declaration.getPkName() + " and " + field.getName());
                }
                declaration.setPkName(field.getName());
                declaration.setPkClass((Class<? extends java.io.Serializable>) field.getType());
                declaration.setPkColumnName(getFieldColumnName(field.getAnnotation(TableField.class), field));
                declaration.setPkGenerateType(id.generateType());
                declaration.setPkAnnotation(field.getAnnotation(TableField.class));
                if (id.generateType() != PrimaryGenerateType.AUTO && id.generateType() != PrimaryGenerateType.INPUT){
                    if (declaration.getPkClass() != String.class){
                        // 不是自增且不是手动输入，则必须是 String 类型
                        throw new IllegalArgumentException("Primary key field with generate type "
                                + id.generateType() + " must be String type, but found "
                                + declaration.getPkClass().getName() + ", in mapper:" + poClass.getName());
                    }
                }
            }else{
                TableField tableField = field.getAnnotation(TableField.class);
                if (tableField != null && !tableField.exist()){
                    // 非数据库字段，跳过
                    continue;
                }
                ColumnDeclaration columnDeclaration = getColumnDeclaration(field);
                columnDeclarations.add(columnDeclaration);
            }
        }
        if (null == declaration.getPkName()){
            throw new IllegalArgumentException("Primary key field not found in mapper:" + poClass.getName());
        }
        applyTableName(declaration);
        declaration.setColumnDeclarations(columnDeclarations);
        TableName annotation = poClass.getAnnotation(TableName.class);
        if (null != annotation && annotation.init() != null && !annotation.init().isEmpty()){
            declaration.setInitScriptResourcePath(annotation.init());
        }
        PO_MAPPER_DECLARATION_MAP.put(poClass, declaration);
        return declaration;
    }
}
