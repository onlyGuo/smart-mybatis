package ink.icoding.smartmybatis.mapper.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 智能 JSON 类型处理器
 * 支持对象、List、Map 等复杂类型的自动序列化与反序列化
 *
 * @author gsk
 */
public class SmartJsonTypeHandler<T> extends BaseTypeHandler<T> {

    private static final Logger log = LoggerFactory.getLogger(SmartJsonTypeHandler.class);

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        // 忽略 JSON 中存在但 Java 对象中不存在的字段
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 允许空对象序列化
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // 支持 Java 8 时间模块 (LocalDateTime 等)
        objectMapper.registerModule(new JavaTimeModule());
    }

    private final JavaType javaType;

    /**
     * 供 MyBatis 默认行为使用 (可能导致泛型丢失，变成 LinkedHashMap)
     * @param type 目标类型 Class
     */
    public SmartJsonTypeHandler(Class<T> type) {
        this.javaType = objectMapper.getTypeFactory().constructType(type);
    }

    /**
     * 供 SmartMapperInitializer 手动实例化使用，完美支持 List<Entity>等复杂泛型
     * @param type 反射获取的 GenericType
     */
    public SmartJsonTypeHandler(Type type) {
        this.javaType = objectMapper.getTypeFactory().constructType(type);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
        try {
            String json = objectMapper.writeValueAsString(parameter);
            ps.setString(i, json);
        } catch (JsonProcessingException e) {
            throw new SQLException("Error converting object to JSON: " + parameter, e);
        }
    }

    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return parseJson(json);
    }

    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return parseJson(json);
    }

    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return parseJson(json);
    }

    private T parseJson(String json) throws SQLException {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, javaType);
        } catch (IOException e) {
            log.error("Failed to parse JSON: {}", json, e);
            throw new SQLException("Error converting JSON to object", e);
        }
    }
}
