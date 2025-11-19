package org.apache.ibatis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method will use a provider class to generate
 * the SQL SELECT statement.
 *
 * <p>Example:</p>
 *
 * <pre>
 * &#64;SelectProvider(type=SqlProvider.class, method="selectById")
 * User selectUser(int id);
 * </pre>
 *
 * @author Clinton Begin
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface SelectProvider {
    Class<?> value() default void.class;

    Class<?> type() default void.class;

    String method() default "";

    String databaseId() default "";

    boolean affectData() default false;

}
