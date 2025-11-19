package org.slf4j;

import ink.icoding.smartmybatis.utils.entity.MapperDeclaration;

public interface Logger {
    void info(String s);

    void info(String s, Object... objects);

    void debug(String s);

    void debug(String s, Object... objects);
}
