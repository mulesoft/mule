/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import org.mule.extension.api.introspection.InterceptableModel;
import org.mule.extension.api.runtime.InterceptorFactory;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Map;

/**
 * Base class for implementations of {@link AbstractImmutableModel} which also implement the
 * {@link InterceptableModel} interface
 *
 * @since 4.0
 */
public abstract class AbstractInterceptableModel extends AbstractImmutableModel implements InterceptableModel
{

    private final List<InterceptorFactory> interceptorFactories;

    /**
     * Creates a new instance
     *
     * @param name                 the model's name
     * @param description          the model's description
     * @param modelProperties      A {@link Map} of custom properties which extend this model
     * @param interceptorFactories a {@link List} with {@link InterceptorFactory} instances. Could be empty or even {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public AbstractInterceptableModel(String name, String description, Map<String, Object> modelProperties, List<InterceptorFactory> interceptorFactories)
    {
        super(name, description, modelProperties);
        this.interceptorFactories = interceptorFactories != null ? ImmutableList.copyOf(interceptorFactories) : ImmutableList.of();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<InterceptorFactory> getInterceptorFactories()
    {
        return interceptorFactories;
    }
}
