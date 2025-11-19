package ink.icoding.smartmybatis.entity.po.enums;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 主键注解
 * 用于标识实体类中的主键字段
 * @author gsk
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TableField {

    /**
     * 字段名
     */
     String value() default "";

    /**
     * 字段描述
     */
    String description() default "";

    /**
     * 非数据库字段
     */
    boolean exist() default true;

    /**
     * 是否为 JSON 字段
     */
    boolean json() default false;

    /**
     * 列类型
     */
    String columnType() default "";

    int length() default 255;
}
