/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.util;

import static org.mule.module.extension.internal.util.CapabilityUtils.getSingleCapability;
import static org.mule.repackaged.internal.org.springframework.util.ReflectionUtils.invokeMethod;
import org.mule.VoidMuleEvent;
import org.mule.api.MuleException;
import org.mule.extension.introspection.Capable;
import org.mule.module.extension.internal.capability.metadata.ParameterGroupCapability;
import org.mule.module.extension.internal.introspection.ParameterGroup;
import org.mule.module.extension.internal.runtime.DefaultObjectBuilder;
import org.mule.module.extension.internal.runtime.ObjectBuilder;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;

import com.google.common.collect.ImmutableList;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * An implementation of {@link ValueSetter} for parameter groups.
 * Parameter groups are a set of parameters defined inside a Pojo in order to reference them
 * as a group and avoid code repetition. The parameter groups are defined by applying
 * the {@link org.mule.extension.annotations.ParameterGroup} annotation to a field.
 * <p/>
 * This {@link ValueSetter} knows how to map a {@link ResolverSetResult} to an object
 * which acts as a group. Because group nesting is allowed, this class is a composite
 * with a {@link #childSetters} collection.
 *
 * @since 3.7.0
 */
public final class GroupValueSetter implements ValueSetter
{

    public static List<ValueSetter> settersFor(Capable capable)
    {
        ImmutableList.Builder<ValueSetter> groupValueSetters = ImmutableList.builder();
        ParameterGroupCapability parameterGroupCapability = getSingleCapability(capable, ParameterGroupCapability.class);

        if (parameterGroupCapability != null)
        {
            for (ParameterGroup group : parameterGroupCapability.getGroups())
            {
                groupValueSetters.add(new GroupValueSetter(group));
            }
        }

        return groupValueSetters.build();
    }

    private final org.mule.module.extension.internal.introspection.ParameterGroup group;
    private List<ValueSetter> childSetters;

    public GroupValueSetter(org.mule.module.extension.internal.introspection.ParameterGroup group)
    {
        this.group = group;
        childSetters = settersFor(group);
    }

    @Override
    public void set(Object target, ResolverSetResult result) throws MuleException
    {
        ObjectBuilder<?> groupBuilder = new DefaultObjectBuilder<>(group.getType());

        for (Map.Entry<String, Method> parameter : group.getParameters().entrySet())
        {
            groupBuilder.addPropertyValue(parameter.getValue(), result.get(parameter.getKey()));
        }

        Object object = groupBuilder.build(VoidMuleEvent.getInstance());
        invokeMethod(group.getSetter(), target, object);

        for (ValueSetter childSetter : childSetters)
        {
            childSetter.set(object, result);
        }
    }
}