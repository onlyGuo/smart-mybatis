package ink.icoding.smartmybatis.utils.entity;

import ink.icoding.smartmybatis.conf.NamingConvention;
import ink.icoding.smartmybatis.conf.SmartConfigHolder;
import ink.icoding.smartmybatis.entity.po.PO;
import ink.icoding.smartmybatis.entity.po.enums.ID;
import ink.icoding.smartmybatis.entity.po.enums.PrimaryGenerateType;
import ink.icoding.smartmybatis.entity.po.enums.TableField;
import ink.icoding.smartmybatis.entity.po.enums.TableName;
import ink.icoding.smartmybatis.mapper.base.SmartMapper;
import ink.icoding.smartmybatis.mapper.provider.dialects.SqlDialects;
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

    private static final String GENERATED_META_SUBPACKAGE = ".M.";
    private static final String GENERATED_META_PREFIX = "$";

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
        declaration.setColumnType(SmartConfigHolder.getDialect().javaTypeToSql(field.getType(), tableField));
        declaration.setAnnotation(tableField);
        if (null != tableField){
            declaration.setDescription(tableField.description());
            declaration.setLink(tableField.link() != PO.class);
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
     * 生成数据表（委托给方言）
     * @param smartMapper Smart Mapper 实例
     * @param declaration Mapper 声明
     */
    public static <T extends PO> void generateTable(SmartMapper<T> smartMapper, MapperDeclaration declaration) {
        String createTableSql = SmartConfigHolder.getDialect().buildCreateTable(declaration);
        smartMapper.executeSql(createTableSql);
    }

    /**
     * 更新数据表结构（委托给方言）
     * @param smartMapper      Smart Mapper 实例
     * @param declaration      Mapper 声明（包含表名、主键、字段声明等）
     * @param existingColumns  已存在的列信息（来自数据库元数据）
     */
    public static <T extends PO> void updateTable(SmartMapper<T> smartMapper,
                                                  MapperDeclaration declaration,
                                                  List<ColumnDeclaration> existingColumns) {
        String sql = SmartConfigHolder.getDialect().buildAlterTable(declaration, existingColumns);
        if (sql != null) {
            smartMapper.executeSql(sql);
        }
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

        MapperDeclaration generatedDeclaration = resolveGeneratedMapperDeclaration(poClass);
        if (generatedDeclaration != null) {
            PO_MAPPER_DECLARATION_MAP.put(poClass, generatedDeclaration);
            return generatedDeclaration;
        }

        for (MapperDeclaration declaration : MAPPER_DECLARATION_MAP.values()) {
            if (declaration.getPoClass().equals(poClass)) {
                PO_MAPPER_DECLARATION_MAP.put(poClass, declaration);
                return declaration;
            }
        }

        MapperDeclaration declaration = buildMapperDeclarationByPoClass(poClass);
        PO_MAPPER_DECLARATION_MAP.put(poClass, declaration);
        return declaration;
    }

    public static MapperDeclaration buildMapperDeclarationByPoClass(Class<? extends PO> poClass) {
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
                        throw new IllegalArgumentException("Primary key field with generate type "
                                + id.generateType() + " must be String type, but found "
                                + declaration.getPkClass().getName() + ", in mapper:" + poClass.getName());
                    }
                }
            }else{
                TableField tableField = field.getAnnotation(TableField.class);
                if (tableField != null && !tableField.exist()){
                    if (tableField.link() == null || tableField.link() == PO.class){
                        // 非数据库字段且非关联字段，跳过
                        continue;
                    }
                    ColumnDeclaration linkDeclaration = buildLinkColumnDeclaration(field, tableField);
                    columnDeclarations.add(linkDeclaration);
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
        return declaration;
    }

    private static ColumnDeclaration buildLinkColumnDeclaration(Field field, TableField tableField) {
        String linkFieldName = tableField.linkField();
        if (!StringUtils.hasText(linkFieldName)) {
            linkFieldName = field.getName();
        }
        try {
            Field declaredField = tableField.link().getDeclaredField(linkFieldName);
            ColumnDeclaration relationColumn = getColumnDeclaration(declaredField);
            ColumnDeclaration linkDeclaration = new ColumnDeclaration();
            linkDeclaration.setField(field);
            linkDeclaration.setFieldName(field.getName());
            linkDeclaration.setColumnName(relationColumn.getColumnName());
            linkDeclaration.setColumnType(relationColumn.getColumnType());
            linkDeclaration.setAnnotation(tableField);
            linkDeclaration.setLink(true);
            return linkDeclaration;
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Linked field " + linkFieldName + " not found in class "
                    + tableField.link().getName() + " for field " + field.getName(), e);
        }
    }

    public static ColumnDeclaration getFieldDeclarationByPoClass(Class<? extends PO> poClass, String fieldName) {
        try {
            Field field = poClass.getDeclaredField(fieldName);
            ID id = field.getAnnotation(ID.class);
            if (id != null) {
                TableField tableField = field.getAnnotation(TableField.class);
                ColumnDeclaration declaration = new ColumnDeclaration();
                declaration.setField(field);
                declaration.setFieldName(field.getName());
                declaration.setColumnName(getFieldColumnName(tableField, field));
                declaration.setColumnType(SmartConfigHolder.getDialect().javaTypeToSql(field.getType(), tableField));
                declaration.setJson(tableField != null && tableField.json());
                declaration.setAnnotation(tableField);
                if (tableField != null) {
                    declaration.setDescription(tableField.description());
                }
                return declaration;
            }
            ColumnDeclaration declaration = new ColumnDeclaration();
            declaration.setField(field);
            applyFieldColumnName(declaration);
            return declaration;
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Field not found in PO: " + poClass.getName() + "." + fieldName, e);
        }
    }

    private static MapperDeclaration resolveGeneratedMapperDeclaration(Class<? extends PO> poClass) {
        String poPackage = poClass.getPackage() == null ? "" : poClass.getPackage().getName();
        String prefixedMetaName = poPackage + GENERATED_META_SUBPACKAGE + GENERATED_META_PREFIX + poClass.getSimpleName();
        MapperDeclaration declaration = resolveGeneratedMapperDeclaration(prefixedMetaName, poClass);
        if (declaration != null) {
            return declaration;
        }
        String legacyMetaName = poPackage + GENERATED_META_SUBPACKAGE + poClass.getSimpleName();
        return resolveGeneratedMapperDeclaration(legacyMetaName, poClass);
    }

    private static MapperDeclaration resolveGeneratedMapperDeclaration(String metaTypeName, Class<? extends PO> poClass) {
        try {
            Class<?> generated = Class.forName(metaTypeName, true, poClass.getClassLoader());
            Object instance = generated.getField("INSTANCE").get(null);
            if (instance instanceof MapperDeclaration) {
                return (MapperDeclaration) instance;
            }
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to read generated mapper metadata: " + metaTypeName, e);
        }
    }
}
