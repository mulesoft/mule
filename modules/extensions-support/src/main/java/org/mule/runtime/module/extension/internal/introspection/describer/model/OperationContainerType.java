/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer.model;

import static java.util.stream.Collectors.toList;

import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * {@link TypeWrapper} specification for classes that contains Operations inside of it
 *
 * @param <T> type that the {@link OperationContainerType} represents
 * @since 4.0
 */
public final class OperationContainerType<T> extends TypeWrapper<T> implements WithOperations
{

    private final Class<T> aClass;

    public OperationContainerType(Class<T> aClass)
    {
        super(aClass);
        this.aClass = aClass;
    }

    /**
     * @return The list of {@link MethodWrapper} that the this type holds
     */
    @Override
    public List<MethodWrapper> getOperations()
    {
        return Stream
                .of(aClass)
                .map(IntrospectionUtils::getOperationMethods)
                .flatMap(Collection::stream)
                .map(MethodWrapper::new)
                .collect(toList());
    }
}
