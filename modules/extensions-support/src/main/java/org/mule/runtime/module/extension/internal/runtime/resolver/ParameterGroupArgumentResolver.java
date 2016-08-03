/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.checkInstantiable;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAlias;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.introspection.EnrichableModel;
import org.mule.runtime.extension.api.runtime.operation.OperationContext;
import org.mule.runtime.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.executor.ReflectiveMethodOperationExecutor;

import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Resolves arguments annotated with {@link ParameterGroup} in a {@link ReflectiveMethodOperationExecutor}.
 * <p/>
 * An implementation of {@link ArgumentResolver} which creates instances of a given  and maps
 * the fields annotated with {@link Parameter} to parameter values of a {@link OperationContext}.
 * <p/>
 * It also looks for fields annotated with {@link ParameterGroup} and recursively populates them too.
 *
 * @param <T> the generic type of the argument instances that will be resolved
 * @since 3.7.0
 */
public final class ParameterGroupArgumentResolver<T> implements ArgumentResolver<T>
{

    private final org.mule.runtime.module.extension.internal.introspection.ParameterGroup<?> group;
    private final Map<? extends Object, ParameterGroupArgumentResolver<? extends Object>> childResolvers;

    public ParameterGroupArgumentResolver(org.mule.runtime.module.extension.internal.introspection.ParameterGroup<?> group)
    {
        checkInstantiable(group.getType());
        this.group = group;
        childResolvers = getResolversForModel(group);
    }

    /**
     * Can be either a {@link Field} or a {@link java.lang.reflect.Parameter}
     *
     * @return {@link org.mule.runtime.module.extension.internal.introspection.ParameterGroup} container
     */
    public Object getContainer()
    {
        return group.getContainer();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public T resolve(OperationContext operationContext)
    {
        try
        {
            T parameterGroup = (T) group.getType().newInstance();
            for (Field parameterField : group.getParameters())
            {
                if (operationContext.hasParameter(getAlias(parameterField)))
                {
                    parameterField.set(parameterGroup, operationContext.getParameter(getAlias(parameterField)));
                }
            }

            for (ParameterGroupArgumentResolver<?> childResolver : childResolvers.values())
            {
                childResolver.resolve(operationContext);
            }

            return parameterGroup;
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(createStaticMessage("Could not create parameter group"), e);
        }
    }

    /**
     * Recursively creates resolvers for the nested {@link org.mule.runtime.module.extension.internal.introspection.ParameterGroup}
     *
     * @param model enrichable model to check for the presence of {@link ParameterGroupModelProperty}
     * @return map with the {@link Method} parameter as key and the associated resolver as value
     */
    public static Map<? extends Object, ParameterGroupArgumentResolver<? extends Object>> getResolversForModel(EnrichableModel model)
    {
        Optional<ParameterGroupModelProperty> parameterGroupModelProperty = model.getModelProperty(ParameterGroupModelProperty.class);

        if (parameterGroupModelProperty.isPresent())
        {
            return parameterGroupModelProperty.get().getGroups().stream()
                    .map(group -> new ParameterGroupArgumentResolver(group))
                    .collect(Collectors.toMap(argumentResolver -> argumentResolver.getContainer(), argumentResolver -> argumentResolver));
        }

        return ImmutableMap.of();
    }
}
