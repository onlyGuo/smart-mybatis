package org.springframework.context;

import java.util.Map;

public interface ApplicationContext {
    String getId();

    String getApplicationName();

    String getDisplayName();

    long getStartupDate();

    ApplicationContext getParent();

    Object getBean(String name);

    <T> T getBean(Class<T> clazz);

    <T> T getBean(String name, Class<T> clazz);

    Map<?,?> getBeansOfType(Class<Object> type);
}
