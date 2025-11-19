package org.apache.ibatis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a method parameter should be bound to a named parameter in a
 * SQL statement.
 *
 * <p>Example:</p>
 *
 * <pre>
 * &#64;Select("SELECT * FROM users WHERE id = #{userId}")
 * User selectUser(@Param("userId") int id);
 * </pre>
 *
 * @author Clinton Begin
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface Param {
    String value();
}
