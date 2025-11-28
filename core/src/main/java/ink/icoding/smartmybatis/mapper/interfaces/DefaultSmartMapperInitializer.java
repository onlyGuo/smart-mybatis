package ink.icoding.smartmybatis.mapper.interfaces;

import ink.icoding.smartmybatis.SpringApplicationUtil;
import ink.icoding.smartmybatis.conf.GlobalConfig;
import ink.icoding.smartmybatis.conf.SmartConfigHolder;
import ink.icoding.smartmybatis.entity.po.PO;
import ink.icoding.smartmybatis.mapper.base.SmartMapper;
import ink.icoding.smartmybatis.mapper.handlers.SmartJsonTypeHandler;
import ink.icoding.smartmybatis.utils.entity.ColumnDeclaration;
import ink.icoding.smartmybatis.utils.entity.MapperDeclaration;
import ink.icoding.smartmybatis.utils.entity.MapperUtil;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 默认的 Smart Mapper 初始化器
 * 用于在 Smart Mapper 创建时进行自定义初始化操作
 *
 * @author gsk
 */
public class DefaultSmartMapperInitializer implements SmartMapperInitializer {

    private final Logger logger = SpringApplicationUtil.getLogger(SmartMapperInitializer.class);

    private final ApplicationContext applicationContext;

    public DefaultSmartMapperInitializer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public <T extends PO> void initMapper(SmartMapper<T> smartMapper) {
        GlobalConfig config = SmartConfigHolder.config();
        Class<?> mapperInterface = smartMapper.getClass().getInterfaces()[0];
        MapperDeclaration mapperDeclaration = MapperUtil.getMapperDeclaration(mapperInterface);

        // 1) 同步数据库结构
        if (config.isAutoSyncDb()) {
            syncDatabaseStructure(smartMapper, mapperInterface, mapperDeclaration);
        }

        // 2) 反射 PATCH：为该 mapper 的 insert 方法以及 insertBatch 注入主键回填
        try {
            patchGeneratedKeysForMapperReflective(mapperInterface, mapperDeclaration);
        } catch (Throwable ex) {
            throw new RuntimeException("Patch generated keys for mapper "
                    + mapperInterface.getName() + " failed: " + ex.getMessage(), ex);
        }

        // 3) 【新增】反射 PATCH：自动注入 JSON ResultMap
        try {
            patchResultMapForJsonFieldsReflective(mapperInterface, mapperDeclaration);
        } catch (Throwable ex) {
            throw new RuntimeException("Patch JSON ResultMap for mapper "
                    + mapperInterface.getName() + " failed: " + ex.getMessage(), ex);
        }
    }


    /**
     * 同步数据库结构
     */
    private <T extends PO> void syncDatabaseStructure(SmartMapper<T> smartMapper, Class<?> mapperInterface,
                                                      MapperDeclaration mapperDeclaration) {
        logger.info("Starting to synchronize database structure for mapper: {}", mapperInterface.getName());
        DataSource dataSource = applicationContext.getBean(DataSource.class);
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            boolean hasTable = false;
            try (ResultSet rs = metaData.getTables(null, null,
                    mapperDeclaration.getTableName(), new String[]{"TABLE"})) {
                hasTable = rs.next();
            }
            if (!hasTable) {
                MapperUtil.generateTable(smartMapper, mapperDeclaration);
            } else {
                List<ColumnDeclaration> existingColumns = new ArrayList<>();
                try (ResultSet fieldRs = metaData.getColumns(null, null, mapperDeclaration.getTableName(), null)) {
                    while (fieldRs.next()) {
                        String columnName = fieldRs.getString("COLUMN_NAME");
                        String dataType = fieldRs.getString("TYPE_NAME");
                        String comment = fieldRs.getString("REMARKS");
                        ColumnDeclaration columnDeclaration = new ColumnDeclaration();
                        columnDeclaration.setColumnName(columnName);
                        columnDeclaration.setColumnType(dataType);
                        columnDeclaration.setDescription(comment);
                        existingColumns.add(columnDeclaration);
                    }
                }
                MapperUtil.updateTable(smartMapper, mapperDeclaration, existingColumns);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        logger.info("Synchronized database structure for mapper: {}", mapperInterface.getName());
    }


    /**
     * 通过反射为指定 mapper 的 INSERT MappedStatement 注入主键策略
     */
    private void patchGeneratedKeysForMapperReflective(Class<?> mapperInterface, MapperDeclaration mapperDeclaration) throws Exception {
        Class<?> sqlSessionFactoryClass = forNameOrNull();
        if (sqlSessionFactoryClass == null) {
            return;
        }

        Object sqlSessionFactory = getSpringBean(applicationContext, sqlSessionFactoryClass);
        if (sqlSessionFactory == null) {
            return;
        }

        Method getConfiguration = sqlSessionFactoryClass.getMethod("getConfiguration");
        Object configuration = getConfiguration.invoke(sqlSessionFactory);
        Class<?> configurationClass = configuration.getClass();

        String pkProperty = mapperDeclaration.getPkName();
        String pkColumn = mapperDeclaration.getPkColumnName();
        String[] keyProperties = buildKeyPropertiesCandidates(pkProperty);

        Method getMappedStatements = configurationClass.getMethod("getMappedStatements");
        Object mappedStatementsObj = getMappedStatements.invoke(configuration);
        Iterable<Object> mappedStatements = toIterable(mappedStatementsObj);

        // 反射获取需要的类
        Class<?> mappedStatementClass = forNameOrFail("org.apache.ibatis.mapping.MappedStatement");
        Class<?> sqlCommandTypeClass = forNameOrFail("org.apache.ibatis.mapping.SqlCommandType");
        Class<?> keyGeneratorInterface = forNameOrFail("org.apache.ibatis.executor.keygen.KeyGenerator");
        Class<?> jdbc3KeyGeneratorClass = forNameOrFail("org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator");
        Class<?> noKeyGeneratorClass = forNameOrFail("org.apache.ibatis.executor.keygen.NoKeyGenerator");
        Class<?> sqlSourceClass = forNameOrFail("org.apache.ibatis.mapping.SqlSource");
        Class<?> builderClass = forNameOrFail("org.apache.ibatis.mapping.MappedStatement$Builder");
        Class<?> statementTypeClass = forNameOrFail("org.apache.ibatis.mapping.StatementType");
        Class<?> resultSetTypeClass = forNameOrFail("org.apache.ibatis.mapping.ResultSetType");
        Class<?> parameterMapClass = forNameOrFail("org.apache.ibatis.mapping.ParameterMap");
        Class<?> cacheClass = forNameOrFail("org.apache.ibatis.cache.Cache");
        Class<?> languageDriverClass = forNameOrFail("org.apache.ibatis.scripting.LanguageDriver");

        Object jdbc3KeyGenInstance;
        try {
            Field instanceField = jdbc3KeyGeneratorClass.getField("INSTANCE");
            jdbc3KeyGenInstance = instanceField.get(null);
        } catch (NoSuchFieldException ignore) {
            jdbc3KeyGenInstance = jdbc3KeyGeneratorClass.getDeclaredConstructor().newInstance();
        }

        // 常用 Getter
        Method msGetId = mappedStatementClass.getMethod("getId");
        Method msGetSqlCommandType = mappedStatementClass.getMethod("getSqlCommandType");
        Method msGetKeyGenerator = mappedStatementClass.getMethod("getKeyGenerator");
        Method msGetKeyProperties = mappedStatementClass.getMethod("getKeyProperties");
        Method msGetConfiguration = mappedStatementClass.getMethod("getConfiguration");
        Method msGetSqlSource = mappedStatementClass.getMethod("getSqlSource");
        Method msGetResource = mappedStatementClass.getMethod("getResource");
        Method msGetFetchSize = mappedStatementClass.getMethod("getFetchSize");
        Method msGetStatementType = mappedStatementClass.getMethod("getStatementType");
        Method msGetDatabaseId = mappedStatementClass.getMethod("getDatabaseId");
        Method msGetTimeout = mappedStatementClass.getMethod("getTimeout");
        Method msGetParameterMap = mappedStatementClass.getMethod("getParameterMap");
        Method msGetResultMaps = mappedStatementClass.getMethod("getResultMaps");
        Method msGetResultSetType = mappedStatementClass.getMethod("getResultSetType");
        Method msGetCache = mappedStatementClass.getMethod("getCache");
        Method msIsFlushCacheRequired = mappedStatementClass.getMethod("isFlushCacheRequired");
        Method msIsUseCache = mappedStatementClass.getMethod("isUseCache");
        Method msGetLang = mappedStatementClass.getMethod("getLang");

        List<Object> targets = new ArrayList<>();
        if (mappedStatements != null) {
            for (Object ms : mappedStatements) {
                if (!mappedStatementClass.isInstance(ms)) {
                    continue;
                }

                String id = String.valueOf(msGetId.invoke(ms));
                if (!id.startsWith(mapperInterface.getName() + ".")) {
                    continue;
                }

                Object cmdType = msGetSqlCommandType.invoke(ms);
                String cmdName = (String) cmdType.getClass().getMethod("name").invoke(cmdType);
                if (!"INSERT".equals(cmdName)) {
                    continue;
                }

                String methodName = id.substring(id.lastIndexOf(".") + 1);
                if (!"insert".equals(methodName) && !"insertBatch".equals(methodName)) {
                    continue;
                }

                Object keyGen = msGetKeyGenerator.invoke(ms);
                boolean needKeyGenPatch = (keyGen == null) || noKeyGeneratorClass.isInstance(keyGen);
                String[] keyProps = (String[]) msGetKeyProperties.invoke(ms);
                boolean hasKeyProps = keyProps != null && keyProps.length > 0;

                if (needKeyGenPatch && !hasKeyProps) {
                    targets.add(ms);
                }
            }
        }

        if (targets.isEmpty()) {
            return;
        }

        for (Object ms : targets) {
            Object cfg = msGetConfiguration.invoke(ms);
            Object sqlSource = msGetSqlSource.invoke(ms);
            String id = String.valueOf(msGetId.invoke(ms));
            Object cmdType = msGetSqlCommandType.invoke(ms);

            Constructor<?> builderCtor = builderClass.getConstructor(configurationClass, String.class, sqlSourceClass, sqlCommandTypeClass);
            Object builder = builderCtor.newInstance(cfg, id, sqlSource, cmdType);

            call(builderClass, builder, "resource", new Class[]{String.class}, new Object[]{msGetResource.invoke(ms)});
            call(builderClass, builder, "fetchSize", new Class[]{Integer.class}, new Object[]{msGetFetchSize.invoke(ms)});
            call(builderClass, builder, "statementType", new Class[]{statementTypeClass}, new Object[]{msGetStatementType.invoke(ms)});

            call(builderClass, builder, "keyGenerator", new Class[]{keyGeneratorInterface}, new Object[]{jdbc3KeyGenInstance});
            call(builderClass, builder, "keyProperty", new Class[]{String.class}, new Object[]{String.join(",", keyProperties)});
            if (pkColumn != null && !pkColumn.isEmpty()) {
                call(builderClass, builder, "keyColumn", new Class[]{String.class}, new Object[]{pkColumn});
            }

            call(builderClass, builder, "databaseId", new Class[]{String.class}, new Object[]{msGetDatabaseId.invoke(ms)});
            call(builderClass, builder, "timeout", new Class[]{Integer.class}, new Object[]{msGetTimeout.invoke(ms)});
            call(builderClass, builder, "parameterMap", new Class[]{parameterMapClass}, new Object[]{msGetParameterMap.invoke(ms)});
            call(builderClass, builder, "resultMaps", new Class[]{List.class}, new Object[]{msGetResultMaps.invoke(ms)});
            call(builderClass, builder, "resultSetType", new Class[]{resultSetTypeClass}, new Object[]{msGetResultSetType.invoke(ms)});
            call(builderClass, builder, "cache", new Class[]{cacheClass}, new Object[]{msGetCache.invoke(ms)});
            call(builderClass, builder, "flushCacheRequired", new Class[]{boolean.class}, new Object[]{msIsFlushCacheRequired.invoke(ms)});
            call(builderClass, builder, "useCache", new Class[]{boolean.class}, new Object[]{msIsUseCache.invoke(ms)});
            call(builderClass, builder, "lang", new Class[]{languageDriverClass}, new Object[]{msGetLang.invoke(ms)});

            Method build = builderClass.getMethod("build");
            Object newMs = build.invoke(builder);

            replaceMappedStatementReflective(configuration, id, newMs);
            logger.debug("Enabled generatedKeys for {} keyProperty={} keyColumn={}", id, Arrays.toString(keyProperties), pkColumn);
        }
    }

    /**
     * 【核心修复】利用 MapperDeclaration 中的元数据，为 SELECT 方法注入包含 JSON Handler 的 ResultMap
     */
    private void patchResultMapForJsonFieldsReflective(Class<?> mapperInterface, MapperDeclaration declaration) throws Exception {
        // 1. 筛选出 JSON 字段
        List<ColumnDeclaration> jsonColumns = declaration.getColumnDeclarations().stream()
                .filter(ColumnDeclaration::isJson)
                .collect(Collectors.toList());

        if (jsonColumns.isEmpty()) {
            return;
        }

        // 2. 获取 MyBatis Configuration
        Class<?> sqlSessionFactoryClass = forNameOrNull();
        if (sqlSessionFactoryClass == null) {
            return;
        }
        Object sqlSessionFactory = getSpringBean(applicationContext, sqlSessionFactoryClass);
        if (sqlSessionFactory == null) {
            return;
        }

        Method getConfiguration = sqlSessionFactoryClass.getMethod("getConfiguration");
        Object configuration = getConfiguration.invoke(sqlSessionFactory);
        Class<?> configurationClass = configuration.getClass();

        // 3. 准备反射需要的类
        Class<?> resultMapClass = forNameOrFail("org.apache.ibatis.mapping.ResultMap");
        Class<?> resultMapBuilderClass = forNameOrFail("org.apache.ibatis.mapping.ResultMap$Builder");
        Class<?> resultMappingBuilderClass = forNameOrFail("org.apache.ibatis.mapping.ResultMapping$Builder");
        Class<?> typeHandlerInterface = forNameOrFail("org.apache.ibatis.type.TypeHandler");

        // 4. 构建或获取通用的 JSON ResultMap
        // ID 命名规则：实体类全名 + _AutoJsonMap
        String autoJsonResultMapId = declaration.getPoClass().getName() + "_AutoJsonMap";

        Method hasResultMapMethod = configurationClass.getMethod("hasResultMap", String.class);
        Method addResultMapMethod = configurationClass.getMethod("addResultMap", resultMapClass);
        Method getResultMapMethod = configurationClass.getMethod("getResultMap", String.class);

        Object sharedResultMap;

        if (!(boolean) hasResultMapMethod.invoke(configuration, autoJsonResultMapId)) {
            // 创建新的 ResultMappings
            List<Object> mappings = new ArrayList<>();

            // 构造函数: Builder(Configuration, property, column, TypeHandler)
            Constructor<?> mappingBuilderCtor = resultMappingBuilderClass.getDeclaredConstructor(
                    configurationClass, String.class, String.class, typeHandlerInterface);

            for (ColumnDeclaration col : jsonColumns) {
                if (col.getField() == null) {
                    logger.warn("Skipping JSON mapping for column {} because Field is missing.", col.getColumnName());
                    continue;
                }
                // 【重点】实例化 Handler，传入泛型类型，解决 List<POJO> 无法转换的问题
                SmartJsonTypeHandler<?> handlerInstance = new SmartJsonTypeHandler<>(col.getField().getGenericType());

                Object mappingBuilder = mappingBuilderCtor.newInstance(
                        configuration, col.getFieldName(), col.getColumnName(), handlerInstance);

                Object mapping = mappingBuilder.getClass().getMethod("build").invoke(mappingBuilder);
                mappings.add(mapping);
            }

            // 创建 ResultMap.Builder
            // Builder(Configuration, id, type, mappings, autoMapping)
            Constructor<?> resultMapBuilderCtor = resultMapBuilderClass.getDeclaredConstructor(
                    configurationClass, String.class, Class.class, List.class, Boolean.class);

            // autoMapping = true 是关键，保证非 JSON 字段依然能自动映射
            Object resultMapBuilder = resultMapBuilderCtor.newInstance(
                    configuration, autoJsonResultMapId, declaration.getPoClass(), mappings, true);

            sharedResultMap = resultMapBuilder.getClass().getMethod("build").invoke(resultMapBuilder);

            // 注册到 Configuration
            addResultMapMethod.invoke(configuration, sharedResultMap);
            logger.debug("Created Auto-JSON ResultMap: {}", autoJsonResultMapId);
        } else {
            sharedResultMap = getResultMapMethod.invoke(configuration, autoJsonResultMapId);
        }

        // 5. 遍历 MappedStatements 并替换 SELECT 的 ResultMap
        Method getMappedStatements = configurationClass.getMethod("getMappedStatements");
        Iterable<Object> mappedStatements = toIterable(getMappedStatements.invoke(configuration));

        if (mappedStatements == null) {
            return;
        }

        // 获取 MappedStatement 的相关方法
        Method msGetId = MappedStatement.class.getMethod("getId");
        Method msGetSqlCommandType = MappedStatement.class.getMethod("getSqlCommandType");
        Method msGetResultMaps = MappedStatement.class.getMethod("getResultMaps");
        Field resultMapsField = MappedStatement.class.getDeclaredField("resultMaps");
        resultMapsField.setAccessible(true);

        for (Object ms : mappedStatements) {
            if (!(ms instanceof MappedStatement)) {
                continue;
            }

            String id = (String) msGetId.invoke(ms);

            // 只处理当前 Mapper 接口下的方法
            if (!id.startsWith(mapperInterface.getName() + ".")) {
                continue;
            }

            // 只处理 SELECT 语句
            Object cmdType = msGetSqlCommandType.invoke(ms);
            String cmdName = (String) cmdType.getClass().getMethod("name").invoke(cmdType);
            if (!"SELECT".equals(cmdName)) {
                continue;
            }

            // 检查当前的 ResultMap
            List<?> currentResultMaps = (List<?>) msGetResultMaps.invoke(ms);
            boolean shouldReplace = false;

            if (currentResultMaps == null || currentResultMaps.isEmpty()) {
                // 如果没有 ResultMap（通常不可能，除非是 void），则替换
                shouldReplace = true;
            } else {
                Object firstMap = currentResultMaps.get(0);
                String mapId = ((ResultMap) firstMap).getId();
                Class<?> type = ((ResultMap) firstMap).getType();

                // 【修复逻辑】
                // 1. 如果当前的 ResultMap 就是我们生成的那个，跳过（防止重复处理）
                if (mapId.equals(autoJsonResultMapId)) {
                    continue;
                }

                // 2. 只要 ResultMap 的返回类型是当前实体类，就进行替换
                // 这样无论是 "-Inline" 还是 "BaseResultMap"，都能被覆盖并增强
                if (declaration.getPoClass().isAssignableFrom(type)) {
                    shouldReplace = true;
                }
            }

            if (shouldReplace) {
                // 替换为我们生成的包含 JSON Handler 的 ResultMap
                resultMapsField.set(ms, Collections.singletonList(sharedResultMap));
                logger.debug("Injected JSON ResultMap into Statement: {}", id);
            }
        }
    }


    /**
     * 将 Configuration.mappedStatements 替换为新的 MappedStatement
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void replaceMappedStatementReflective(Object configuration, String id, Object newMs) {
        try {
            Field f = configuration.getClass().getDeclaredField("mappedStatements");
            f.setAccessible(true);
            Object mapObj = f.get(configuration);
            if (mapObj instanceof Map) {
                if (((Map) mapObj).containsKey(id)){
                    ((Map) mapObj).replace(id, newMs);
                }else{
                    ((Map) mapObj).put(id, newMs);
                }
            } else {
                // 如果不是标准 Map (如 StrictMap), 通常也实现了 Map 接口
                throw new IllegalStateException("Configuration.mappedStatements is not a Map");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to replace MappedStatement: " + id, e);
        }
    }

    private static String[] buildKeyPropertiesCandidates(String prop) {
        return new String[]{"record." + prop};
    }

    // ———————— 反射与 Spring 辅助方法 ————————

    private static Class<?> forNameOrNull() {
        try {
            return Class.forName("org.apache.ibatis.session.SqlSessionFactory");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static Class<?> forNameOrFail(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Class not found: " + name, e);
        }
    }

    private static Object getSpringBean(ApplicationContext ctx, Class<?> type) {
        try {
            return ctx.getBean(type);
        } catch (Throwable e) {
            try {
                Map<?, ?> map = ctx.getBeansOfType((Class<Object>) type);
                if (!map.isEmpty()) {
                    return map.values().iterator().next();
                }
            } catch (Throwable ignore) {
            }
            return null;
        }
    }

    private static Iterable<Object> toIterable(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Iterable) {
            return (Iterable<Object>) obj;
        }
        try {
            Method values = obj.getClass().getMethod("iterator");
            Object it = values.invoke(obj);
            if (it instanceof Iterator) {
                List<Object> list = new ArrayList<>();
                Iterator<?> iterator = (Iterator<?>) it;
                while (iterator.hasNext()) {
                    list.add(iterator.next());
                }
                return list;
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static Object call(Class<?> clazz, Object target, String name, Class<?>[] paramTypes, Object[] args) {
        try {
            Method m = clazz.getMethod(name, paramTypes);
            return m.invoke(target, args);
        } catch (Exception e) {
            throw new IllegalStateException("Reflective call failed: " + clazz.getName() + "." + name, e);
        }
    }
}
