package org.springframework.beans.factory.support;

public interface BeanDefinitionBuilder {
    static <T> BeanDefinitionBuilder rootBeanDefinition(Class<T> clazz) {
        return null;
    }

    Object getBeanDefinition();

}
