package org.springframework.beans.factory.support;

public interface DefaultListableBeanFactory {

    void registerBeanDefinition(Object o1, Object o2);

    <T> T getBean(String name, Class<T> clazz);

    void registerSingleton(String name, Object obj);

    void destroySingleton(String name);
}
