/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.util;

import static org.springframework.util.ReflectionUtils.setField;
import org.mule.VoidMuleEvent;
import org.mule.api.MuleException;
import org.mule.extension.api.introspection.EnrichableModel;
import org.mule.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.module.extension.internal.introspection.ParameterGroup;
import org.mule.module.extension.internal.runtime.DefaultObjectBuilder;
import org.mule.module.extension.internal.runtime.ObjectBuilder;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.util.collection.ImmutableListCollector;

import com.google.common.collect.ImmutableList;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * An implementation of {@link ValueSetter} for parameter groups.
 * Parameter groups are a set of parameters defined inside a Pojo in order to reference them
 * as a group and avoid code repetition. The parameter groups are defined by applying
 * the {@link org.mule.extension.annotation.api.ParameterGroup} annotation to a field.
 * <p/>
 * This {@link ValueSetter} knows how to map a {@link ResolverSetResult} to an object
 * which acts as a group. Because group nesting is allowed, this class is a composite
 * with a {@link #childSetters} collection.
 *
 * @since 3.7.0
 */
public final class GroupValueSetter implements ValueSetter
{

    /**
     * Returns a {@link List} containing one {@link ValueSetter} instance per each
     * {@link ParameterGroup} defined in the {@link ParameterGroupModelProperty} extracted
     * from the given {@code model}. If {@code model} does not contain such model property
     * then an empty {@link List} is returned
     *
     * @param model a {@link EnrichableModel} instance presumed to have the {@link ParameterGroupModelProperty}
     * @return a {@link List} with {@link ValueSetter} instances. May be empty but will never be {@code null}
     */
    public static List<ValueSetter> settersFor(EnrichableModel model)
    {
        ParameterGroupModelProperty parameterGroupModelProperty = model.getModelProperty(ParameterGroupModelProperty.KEY);

        if (parameterGroupModelProperty != null)
        {
            return parameterGroupModelProperty.getGroups().stream()
                    .map(group -> new GroupValueSetter(group))
                    .collect(new ImmutableListCollector<>());
        }

        return ImmutableList.of();
    }

    private final org.mule.module.extension.internal.introspection.ParameterGroup group;
    private final List<ValueSetter> childSetters;

    /**
     * Creates a new instance that can set values defined in the given {@code group}
     *
     * @param group a {@link ParameterGroup}
     */
    public GroupValueSetter(org.mule.module.extension.internal.introspection.ParameterGroup group)
    {
        this.group = group;
        childSetters = settersFor(group);
    }

    @Override
    public void set(Object target, ResolverSetResult result) throws MuleException
    {
        ObjectBuilder<?> groupBuilder = new DefaultObjectBuilder<>(group.getType());

        for (Map.Entry<String, Field> parameter : group.getParameters().entrySet())
        {
            groupBuilder.addPropertyResolver(parameter.getValue(), new StaticValueResolver<>(result.get(parameter.getKey())));
        }

        Object groupValue = groupBuilder.build(VoidMuleEvent.getInstance());
        setField(group.getField(), target, groupValue);

        for (ValueSetter childSetter : childSetters)
        {
            childSetter.set(groupValue, result);
        }
    }
}