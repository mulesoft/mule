/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer.model;

import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.lang.reflect.Type;
import java.util.List;

/**
 * {@link TypeBasedComponent} specification for {@link Source} types.
 *
 * @since 4.0
 */
public final class SourceType<T extends Source> extends TypeBasedComponent<T> implements WithGenerics
{

    private final Class<T> aClass;

    public SourceType(Class<T> aClass)
    {
        super(aClass);
        this.aClass = aClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Type> getSuperClassGenerics()
    {
        return IntrospectionUtils.getSuperClassGenerics(aClass, Source.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Class<?>> getInterfaceGenerics(Class clazz)
    {
        return IntrospectionUtils.getInterfaceGenerics(getDeclaredClass(), clazz);
    }
}
