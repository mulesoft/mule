/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.spring.dsl.spring;

/**
 * Holder for a bean value and the object {@code Class} that will be created from it.
 *
 * The bean value currently it's of an Object type since it can be a {@link org.springframework.beans.factory.config.BeanDefinition}
 * or a {@link org.springframework.beans.factory.config.RuntimeBeanReference}
 *
 * @since 4.0
 */
class BeanValueTypePair
{

    private final Class<?> type;
    private final Object bean;

    /**
     * @param type the type of the object to be created
     * @param bean the bean definition
     */
    public BeanValueTypePair(Class<?> type, Object bean)
    {
        this.type = type;
        this.bean = bean;
    }

    /**
     * @return the object type that will be created from the bean value
     */
    public Class<?> getType()
    {
        return type;
    }

    /**
     * @return the bean value definition. It may be a {@link org.springframework.beans.factory.config.RuntimeBeanReference} or a {@link org.springframework.beans.factory.config.BeanDefinition}
     */
    public Object getBean()
    {
        return bean;
    }
}
