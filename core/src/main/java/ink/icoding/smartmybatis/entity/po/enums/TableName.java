package ink.icoding.smartmybatis.entity.po.enums;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表注解
 * 用于标识实体类对应的数据库表名
 * @author gsk
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TableName {

    /**
     * 表名
     */
    String value() default "";

    /**
     * 表描述
     */
    String description() default "";

    /**
     * 初始化语句
     */
    String init() default "";
}
