package ink.icoding.smartmybatis.mapper.provider.dialects;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SQL 方言注解
 * <p>
 * 标注在 {@link SqlDialects} 实现类上，声明该方言匹配的 JDBC 驱动类名。
 * 启动时根据数据源驱动自动选择对应的方言实现。
 *
 * @author gsk
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SmartDialect {
    /**
     * 匹配的 JDBC 驱动类名数组
     */
    String[] value();
}
