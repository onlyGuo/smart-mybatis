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
import ink.icoding.smartmybatis.utils.file.FileUtil;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

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

        // 4) 初始化脚本(如果有, 且表为空)
        String initScriptResourcePath = mapperDeclaration.getInitScriptResourcePath();
        if (initScriptResourcePath != null && !initScriptResourcePath.isEmpty()) {
            executeInitScriptIfTableEmpty(smartMapper, mapperDeclaration, initScriptResourcePath);
        }
    }

    /**
     * 如果表为空，则执行初始化脚本
     */
    private <T extends PO> void executeInitScriptIfTableEmpty(SmartMapper<T> smartMapper,
                                                              MapperDeclaration mapperDeclaration,
                                                              String initScriptResourcePath) {
        // 先检查初始化脚本是否存在
        if (!FileUtil.existsResource(initScriptResourcePath)){
            logger.warn("Initialization script resource not found: {}", initScriptResourcePath);
            return;
        }
        String script = new String(FileUtil.readResource(initScriptResourcePath), StandardCharsets.UTF_8);
        if (script.trim().isEmpty()){
            logger.warn("Initialization script is empty: {}", initScriptResourcePath);
            return;
        }
        // 检查表是否为空
        long count = smartMapper.count();
        if (count == 0){
            logger.info("Table {} is empty, executing initialization script: {}",
                    mapperDeclaration.getTableName(), initScriptResourcePath);
            smartMapper.executeSqlScript(script);
            logger.info("Initialization script executed for table {}", mapperDeclaration.getTableName());
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
     * 指定 mapper 的 INSERT MappedStatement 注入：
     * - Jdbc3KeyGenerator.INSTANCE
     * - keyProperty（兼容多种参数命名）
     * - 可选 keyColumn
     */
    private void patchGeneratedKeysForMapperReflective(Class<?> mapperInterface, MapperDeclaration mapperDeclaration) throws Exception {
        // 若环境未引入 MyBatis，直接跳过
        if (forNameOrNull() == null) {
            logger.debug("MyBatis not present, skip generatedKeys patch.");
            return;
        }

        SqlSessionFactory sqlSessionFactory = (SqlSessionFactory) getSpringBean(applicationContext, SqlSessionFactory.class);
        if (sqlSessionFactory == null) {
            logger.debug("No SqlSessionFactory bean found, skip generatedKeys patch.");
            return;
        }

        // Configuration
        Configuration configuration = sqlSessionFactory.getConfiguration();
        // 获取所有 MappedStatement
        Collection<?> mappedStatements = configuration.getMappedStatements();

        if (mappedStatements == null) {
            throw new IllegalStateException("Configuration.getMappedStatements() returned null or non-iterable");
        }

        // 遍历并挑选 INSERT + 当前 mapper
        List<MappedStatement> targets = new ArrayList<>();
        for (Object obj : mappedStatements) {
            if (!(obj instanceof MappedStatement)) {
                continue;
            }
            MappedStatement ms = (MappedStatement) obj;
            String id = ms.getId();
            if (!id.startsWith(mapperInterface.getName() + ".")) {
                continue;
            }

            SqlCommandType cmdType = ms.getSqlCommandType();
            String cmdName = (String) cmdType.getClass().getMethod("name").invoke(cmdType);
            if (!"INSERT".equals(cmdName)) {
                continue;
            }

            // 通过方法名判断, 只拦截insert 和 insertBatch
            String methodName = id.substring(id.lastIndexOf(".") + 1);
            if (!"insert".equals(methodName) && !"insertBatch".equals(methodName)) {
                continue;
            }
            KeyGenerator keyGen = ms.getKeyGenerator();
            boolean needKeyGenPatch = (keyGen == null) || keyGen instanceof NoKeyGenerator;
            String[] keyProps = ms.getKeyProperties();
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
        for (MappedStatement ms : targets) {
            String id = ms.getId();
            String pkProperty = mapperDeclaration.getPkName();
            String pkColumn = mapperDeclaration.getPkColumnName();
            String methodName = id.substring(id.lastIndexOf(".") + 1);
            String[] keyProperties = buildKeyPropertiesCandidates(pkProperty, methodName);

            // 构造 Builder(configuration, id, sqlSource, sqlCommandType)
            MappedStatement.Builder builder = new MappedStatement.Builder(
                    ms.getConfiguration(), id, ms.getSqlSource(), ms.getSqlCommandType()
            );

            // 设置 keyGenerator / keyProperty / keyColumn
            builder.keyGenerator(Jdbc3KeyGenerator.INSTANCE).keyProperty(String.join(",", keyProperties));
            if (pkColumn != null && !pkColumn.isEmpty()) {
                builder.keyColumn(pkColumn);
            }
            // 依次复制原属性
            builder.fetchSize(ms.getFetchSize())
                    .statementType(ms.getStatementType())
                    .parameterMap(ms.getParameterMap())
                    .databaseId(ms.getDatabaseId())
                    .timeout(ms.getTimeout())
                    .parameterMap(ms.getParameterMap())
                    .resultMaps(ms.getResultMaps())
                    .resultSetType(ms.getResultSetType())
                    .cache(ms.getCache())
                    .flushCacheRequired(ms.isFlushCacheRequired())
                    .useCache(ms.isUseCache())
                    .lang(ms.getLang());

            // 构建新的 MappedStatement
            MappedStatement newMs = builder.build();

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

    /**
     * 【核心修复】利用 MapperDeclaration 中的元数据，为 SELECT 方法注入包含 JSON Handler 的 ResultMap
     */
    private void patchResultMapForJsonFieldsReflective(Class<?> mapperInterface, MapperDeclaration declaration) throws Exception {
        // 筛选出 JSON 字段
        List<ColumnDeclaration> jsonColumns = declaration.getColumnDeclarations().stream()
                .filter(ColumnDeclaration::isJson)
                .collect(Collectors.toList());

        if (jsonColumns.isEmpty()) {
            return;
        }

        //  获取 MyBatis Configuration
        if (forNameOrNull() == null) {
            return;
        }
        SqlSessionFactory sqlSessionFactory = (SqlSessionFactory)getSpringBean(applicationContext, SqlSessionFactory.class);
        if (sqlSessionFactory == null) {
            return;
        }

        Configuration configuration = sqlSessionFactory.getConfiguration();

        // 构建或获取通用的 JSON ResultMap
        // ID 命名规则：实体类全名 + _AutoJsonMap
        String autoJsonResultMapId = declaration.getPoClass().getName() + "_AutoJsonMap";
        ResultMap sharedResultMap;

        if (!configuration.hasResultMap(autoJsonResultMapId)) {
            // 创建新的 ResultMappings
            List<ResultMapping> mappings = new ArrayList<>();

            for (ColumnDeclaration col : jsonColumns) {
                if (col.getField() == null) {
                    logger.warn("Skipping JSON mapping for column {} because Field is missing.", col.getColumnName());
                    continue;
                }
                // 【重点】实例化 Handler，传入泛型类型，解决 List<POJO> 无法转换的问题
                SmartJsonTypeHandler<?> handlerInstance = new SmartJsonTypeHandler<>(col.getField().getGenericType());

//                ResultMapping mappingColumn = new ResultMapping.Builder(
//                        configuration, col.getFieldName(), col.getColumnName(), handlerInstance
//                ).build();
//                mappings.add(mappingColumn);

                ResultMapping mappingField = new ResultMapping.Builder(
                        configuration, col.getFieldName(), col.getFieldName(), handlerInstance
                ).build();
                mappings.add(mappingField);
            }

            // 创建 ResultMap.Builder
            // autoMapping = true 是关键，保证非 JSON 字段依然能自动映射
            ResultMap.Builder resultMapBuilder = new ResultMap.Builder(
                    configuration, autoJsonResultMapId, declaration.getPoClass(), mappings, true);


            sharedResultMap = resultMapBuilder.build();

            // 注册到 Configuration
            configuration.addResultMap(sharedResultMap);
            logger.debug("Created Auto-JSON ResultMap: {}", autoJsonResultMapId);
        } else {
            sharedResultMap = configuration.getResultMap(autoJsonResultMapId);
        }

        // 5. 遍历 MappedStatements 并替换 SELECT 的 ResultMap
        Collection<?> mappedStatements = configuration.getMappedStatements();
        if (mappedStatements == null) {
            return;
        }

        for (Object obj : mappedStatements) {
            if (!(obj instanceof MappedStatement)) {
                continue;
            }
            MappedStatement ms = (MappedStatement) obj;
            String id = ms.getId();

            // 只处理当前 Mapper 接口下的方法
            if (!id.startsWith(mapperInterface.getName() + ".")) {
                continue;
            }

            // 只处理 SELECT 语句
            SqlCommandType cmdType = ms.getSqlCommandType();
            String cmdName = (String) cmdType.getClass().getMethod("name").invoke(cmdType);
            if (!"SELECT".equals(cmdName)) {
                continue;
            }

            // 检查当前的 ResultMap
            List<ResultMap> currentResultMaps = ms.getResultMaps();
            boolean shouldReplace = false;

            if (currentResultMaps == null || currentResultMaps.isEmpty()) {
                // 如果没有 ResultMap（通常不可能，除非是 void），则替换
                shouldReplace = true;
            } else {
                ResultMap firstMap = currentResultMaps.get(0);
                String mapId = firstMap.getId();
                Class<?> type = firstMap.getType();

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
                Field resultMapsField = MappedStatement.class.getDeclaredField("resultMaps");
                resultMapsField.setAccessible(true);
                resultMapsField.set(ms, Collections.singletonList(sharedResultMap));
                logger.debug("Injected JSON ResultMap into Statement: {}", id);
            }
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
