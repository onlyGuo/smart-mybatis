package ink.icoding.smartmybatis.entity.po.enums;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * 主键注解
 * 用于标识实体类中的主键字段
 * @author gsk
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ID {

    /**
     * 主键生成类型
     * @return 主键生成类型
     */
    PrimaryGenerateType generateType() default PrimaryGenerateType.AUTO;
}
