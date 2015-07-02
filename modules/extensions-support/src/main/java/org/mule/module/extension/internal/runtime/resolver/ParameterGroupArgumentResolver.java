/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.resolver;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.module.extension.internal.util.IntrospectionUtils.checkInstantiable;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getAlias;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getParameterFields;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getParameterGroupFields;
import org.mule.api.MuleRuntimeException;
import org.mule.extension.annotations.Parameter;
import org.mule.extension.annotations.ParameterGroup;
import org.mule.extension.runtime.OperationContext;
import org.mule.module.extension.internal.runtime.ReflectiveMethodOperationExecutor;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves arguments annotated with {@link ParameterGroup} in a {@link ReflectiveMethodOperationExecutor}.
 * <p/>
 * An implementation of {@link ArgumentResolver} which creates instances of a given {@link #type} and maps
 * the fields annotated with {@link Parameter} to parameter values of a {@link OperationContext}.
 * <p/>
 * It also looks for fields annotated with {@link ParameterGroup} and recursively populates them too.
 *
 * @param <T> the generic type of the argument instances that will be resolved
 * @since 3.7.0
 */
public final class ParameterGroupArgumentResolver<T> implements ArgumentResolver<T>
{

    private final Class<T> type;
    private final Collection<Field> parameterFields;
    private final Map<Field, ParameterGroupArgumentResolver<?>> childGroups;

    /**
     * Creates an instance that will resolve instances of {@code type}
     *
     * @param type the {@link Class} of the instances that will be resolved
     */
    public ParameterGroupArgumentResolver(Class<T> type)
    {
        checkInstantiable(type);
        this.type = type;

        parameterFields = getParameterFields(type);
        for (Field parameterField : parameterFields)
        {
            parameterField.setAccessible(true);
        }

        childGroups = new HashMap<>();
        for (Field groupField : getParameterGroupFields(type))
        {
            groupField.setAccessible(true);
            childGroups.put(groupField, new ParameterGroupArgumentResolver<>(groupField.getType()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T resolve(OperationContext operationContext)
    {
        try
        {
            T group = type.newInstance();
            for (Field parameterField : parameterFields)
            {
                Object value = operationContext.getParameterValue(getAlias(parameterField));
                if (value != null)
                {
                    parameterField.set(group, value);
                }
            }

            for (Map.Entry<Field, ParameterGroupArgumentResolver<?>> childGroup : childGroups.entrySet())
            {
                childGroup.getKey().set(group, childGroup.getValue().resolve(operationContext));
            }

            return group;
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(createStaticMessage("Could not create parameter group"), e);
        }
    }
}
