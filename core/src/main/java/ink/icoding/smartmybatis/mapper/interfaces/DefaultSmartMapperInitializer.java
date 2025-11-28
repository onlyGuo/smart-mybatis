package ink.icoding.smartmybatis.mapper.interfaces;

import ink.icoding.smartmybatis.SpringApplicationUtil;
import ink.icoding.smartmybatis.conf.GlobalConfig;
import ink.icoding.smartmybatis.conf.SmartConfigHolder;
import ink.icoding.smartmybatis.entity.po.PO;
import ink.icoding.smartmybatis.mapper.base.SmartMapper;
import ink.icoding.smartmybatis.utils.entity.ColumnDeclaration;
import ink.icoding.smartmybatis.utils.entity.MapperDeclaration;
import ink.icoding.smartmybatis.utils.entity.MapperUtil;
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

/**
 * 默认的 Smart Mapper 初始化器
 * 用于在 Smart Mapper 创建时进行自定义初始化操作
 * 反射版：不直接依赖 MyBatis
 * @author gsk
 */
public class DefaultSmartMapperInitializer implements SmartMapperInitializer {

    private Logger logger = SpringApplicationUtil.getLogger(SmartMapperInitializer.class);

    private final ApplicationContext applicationContext;

    public DefaultSmartMapperInitializer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public <T extends PO> void initMapper(SmartMapper<T> smartMapper) {
        GlobalConfig config = SmartConfigHolder.config();
        Class<?> mapperInterface = smartMapper.getClass().getInterfaces()[0];
        MapperDeclaration mapperDeclaration = MapperUtil.getMapperDeclaration(mapperInterface);
        if (config.isAutoSyncDb()){
            // 1) 同步数据库结构
            logger.info("Starting to synchronize database structure for mapper: {}", mapperInterface.getName());
            DataSource dataSource = applicationContext.getBean(DataSource.class);
            try (Connection connection = dataSource.getConnection()){
                DatabaseMetaData metaData = connection.getMetaData();
                boolean hasTable = false;
                try (ResultSet rs = metaData.getTables(null, null,
                        mapperDeclaration.getTableName(), new String[]{"TABLE"})){
                    hasTable = rs.next();
                }
                if (!hasTable) {
                    MapperUtil.generateTable(smartMapper, mapperDeclaration);
                }else{
                    List<ColumnDeclaration> existingColumns = new ArrayList<>();
                    try (ResultSet fieldRs = metaData.getColumns(null, null, mapperDeclaration.getTableName(), null);){
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

        // 2) 反射 PATCH：为该 mapper 的 insert 方法以及 insertBatch 注入主键回填
        try {
            patchGeneratedKeysForMapperReflective(mapperInterface, mapperDeclaration);
        } catch (Throwable ex) {
            throw new RuntimeException("Patch generated keys for mapper "
                    + mapperInterface.getName() + " failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 通过反射为指定 mapper 的 INSERT MappedStatement 注入：
     * - Jdbc3KeyGenerator.INSTANCE
     * - keyProperty（兼容多种参数命名）
     * - 可选 keyColumn
     */
    private void patchGeneratedKeysForMapperReflective(Class<?> mapperInterface, MapperDeclaration mapperDeclaration) throws Exception {
        // 若环境未引入 MyBatis，直接跳过
        Class<?> sqlSessionFactoryClass = forNameOrNull();
        if (sqlSessionFactoryClass == null) {
            logger.debug("MyBatis not present, skip generatedKeys patch.");
            return;
        }

        Object sqlSessionFactory = getSpringBean(applicationContext, sqlSessionFactoryClass);
        if (sqlSessionFactory == null) {
            logger.debug("No SqlSessionFactory bean found, skip generatedKeys patch.");
            return;
        }

        // Configuration
        Method getConfiguration = sqlSessionFactoryClass.getMethod("getConfiguration");
        Object configuration = getConfiguration.invoke(sqlSessionFactory);
        Class<?> configurationClass = configuration.getClass();



        // 获取所有 MappedStatement
        Method getMappedStatements = configurationClass.getMethod("getMappedStatements");
        Object mappedStatementsObj = getMappedStatements.invoke(configuration);

        Iterable<Object> mappedStatements = toIterable(mappedStatementsObj);
        if (mappedStatements == null) {
            throw new IllegalStateException("Configuration.getMappedStatements() returned null or non-iterable");
        }

        // 需要的类（反射）
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

        Object jdbc3KeyGenInstance = null;
        try {
            Field instanceField = jdbc3KeyGeneratorClass.getField("INSTANCE");
            jdbc3KeyGenInstance = instanceField.get(null);
        } catch (NoSuchFieldException ignore) {
            // 退路：尝试默认构造
            jdbc3KeyGenInstance = jdbc3KeyGeneratorClass.getDeclaredConstructor().newInstance();
        }

        // 反射访问 MappedStatement 常用 getter
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

        // 遍历并挑选 INSERT + 当前 mapper
        List<Object> targets = new ArrayList<>();
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

            // 通过方法名判断, 只拦截insert 和 insertBatch
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

        if (targets.isEmpty()) {
            throw new IllegalStateException("No INSERT MappedStatement found for mapper "
                    + mapperInterface.getName() + " that requires generatedKeys patch.");
        }

        // 构造新的 MappedStatement 并替换
        for (Object ms : targets) {
            Object cfg = msGetConfiguration.invoke(ms);
            Object sqlSource = msGetSqlSource.invoke(ms);
            String id = String.valueOf(msGetId.invoke(ms));
            Object cmdType = msGetSqlCommandType.invoke(ms);

            // 构造 Builder(configuration, id, sqlSource, sqlCommandType)
            Constructor<?> builderCtor = builderClass.getConstructor(configurationClass, String.class, sqlSourceClass, sqlCommandTypeClass);
            Object builder = builderCtor.newInstance(cfg, id, sqlSource, cmdType);

            // 依次复制原属性
            call(builderClass, builder, "resource", new Class[]{String.class}, new Object[]{msGetResource.invoke(ms)});
            call(builderClass, builder, "fetchSize", new Class[]{Integer.class}, new Object[]{msGetFetchSize.invoke(ms)});
            call(builderClass, builder, "statementType", new Class[]{statementTypeClass}, new Object[]{msGetStatementType.invoke(ms)});

            String pkProperty = mapperDeclaration.getPkName();
            String pkColumn = mapperDeclaration.getPkColumnName();
            String methodName = id.substring(id.lastIndexOf(".") + 1);
            String[] keyProperties = buildKeyPropertiesCandidates(pkProperty, methodName);

            // 设置 keyGenerator / keyProperty / keyColumn
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

            // 构建新的 MappedStatement
            Method build = builderClass.getMethod("build");
            Object newMs = build.invoke(builder);

            // 替换 Configuration.mappedStatements
            replaceMappedStatementReflective(configuration, id, newMs);

            logger.debug("Enabled generatedKeys for {} keyProperty={} keyColumn={}",
                    id, Arrays.toString(keyProperties), pkColumn);
        }
    }

    // 将 Configuration.mappedStatements 替换为新的 MappedStatement
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void replaceMappedStatementReflective(Object configuration, String id, Object newMs) {
        try {
            Field f = configuration.getClass().getDeclaredField("mappedStatements");
            f.setAccessible(true);
            Object mapObj = f.get(configuration);
            if (mapObj instanceof Map) {
                ((Map) mapObj).replace(id, newMs);
                return;
            }
            // 兜底：如果不是 Map（极少见），尝试 remove + put（需要具体类型支持），此处直接抛错
            throw new IllegalStateException("Configuration.mappedStatements is not a Map");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to replace MappedStatement: " + id, e);
        }
    }

    // 兼容不同参数命名的 keyProperty 候选路径
    private static String[] buildKeyPropertiesCandidates(String prop, String methodName) {
        if ("insert".equals(methodName)){
            return new String[] {
                    "record." + prop,
            };
        }else if ("insertBatch".equals(methodName)){
            return new String[] {
                    "list." + prop,
            };
        }else{
            return new String[] {
                    prop,
            };
        }
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
            // 如果存在多个 bean 或没有，尝试 getBeansOfType 取第一个
            try {
                Map<?, ?> map = ctx.getBeansOfType((Class<Object>) type);
                if (!map.isEmpty()) {
                    return map.values().iterator().next();
                }
            } catch (Throwable ignore) {}
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
        // 常见：返回的是 Collection 或 StrictMap.values()
        try {
            // 尝试 values()
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
        } catch (Throwable ignore) {}
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
