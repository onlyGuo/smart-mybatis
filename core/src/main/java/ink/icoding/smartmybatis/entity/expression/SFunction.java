package ink.icoding.smartmybatis.entity.expression;

import java.io.Serializable;
import java.util.function.Function;

/**
 * 可序列化的Function对象
 * @author gsk
 */
@FunctionalInterface
public interface SFunction<T, R> extends Function<T, R>, Serializable {
}
