/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import static org.mule.util.CollectionUtils.immutableList;
import org.mule.extension.api.introspection.DataType;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.extension.api.introspection.SourceModel;
import org.mule.extension.api.runtime.InterceptorFactory;
import org.mule.extension.api.runtime.source.Source;
import org.mule.extension.api.runtime.source.SourceFactory;

import java.util.List;
import java.util.Map;

/**
 * Immutable implementation of {@link SourceModel}
 *
 * @since 4.0
 */
final class ImmutableSourceModel extends AbstractInterceptableModel implements SourceModel
{

    private final List<ParameterModel> parameterModels;
    private final DataType returnType;
    private final DataType attributesType;
    private final SourceFactory sourceFactory;

    /**
     * Creates a new instance
     *
     * @param name                 the source name. Cannot be blank
     * @param description          the source description
     * @param parameterModels      a {@link List} with the source's {@link ParameterModel parameterModels}
     * @param returnType           a {@link DataType} which represents the payload of generated messages
     * @param attributesType       a {@link DataType} which represents the attributes on the generated messages
     * @param sourceFactory        a {@link SourceFactory} used to create instances of {@link Source} which are consistent with this model
     * @param modelProperties      A {@link Map} of custom properties which extend this model
     * @param interceptorFactories A {@link List} with the {@link InterceptorFactory} instances that should be applied to instances built from this model
     */
    ImmutableSourceModel(String name,
                         String description,
                         List<ParameterModel> parameterModels,
                         DataType returnType,
                         DataType attributesType,
                         SourceFactory sourceFactory,
                         Map<String, Object> modelProperties,
                         List<InterceptorFactory> interceptorFactories)
    {
        super(name, description, modelProperties, interceptorFactories);
        this.parameterModels = immutableList(parameterModels);
        this.returnType = returnType;
        this.sourceFactory = sourceFactory;
        this.attributesType = attributesType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SourceFactory getSourceFactory()
    {
        return sourceFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataType getReturnType()
    {
        return returnType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ParameterModel> getParameterModels()
    {
        return parameterModels;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataType getAttributesType()
    {
        return attributesType;
    }
}
