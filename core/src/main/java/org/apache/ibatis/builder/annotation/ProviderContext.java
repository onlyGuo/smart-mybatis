package org.apache.ibatis.builder.annotation;

import java.lang.reflect.Method;

public final class ProviderContext {
    private final Class<?> mapperType;
    private final Method mapperMethod;
    private final String databaseId;

    ProviderContext(Class<?> mapperType, Method mapperMethod, String databaseId) {
        this.mapperType = mapperType;
        this.mapperMethod = mapperMethod;
        this.databaseId = databaseId;
    }

    public Class<?> getMapperType() {
        return this.mapperType;
    }

    public Method getMapperMethod() {
        return this.mapperMethod;
    }

    public String getDatabaseId() {
        return this.databaseId;
    }
}
