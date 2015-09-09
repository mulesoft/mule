/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.config;

import static org.mule.module.extension.internal.util.IntrospectionUtils.getField;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.extension.introspection.ConfigurationModel;
import org.mule.module.extension.internal.runtime.BaseObjectBuilder;
import org.mule.module.extension.internal.runtime.ObjectBuilder;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.module.extension.internal.util.GroupValueSetter;
import org.mule.module.extension.internal.util.SingleValueSetter;
import org.mule.module.extension.internal.util.ValueSetter;
import org.mule.util.collection.ImmutableListCollector;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Implementation of {@link ObjectBuilder} to create instances that
 * match a given {@link ConfigurationModel}.
 * <p/>
 * The object instances are created through the {@link ConfigurationModel#getInstantiator()#instantiateObject()}
 * method. A {@link ResolverSet} is also used to automatically set this builders
 * properties. The name of the properties in the {@link ResolverSet} must match the
 * name of an actual property in the prototype class
 *
 * @since 3.7.0
 */
public final class ConfigurationObjectBuilder<T> extends BaseObjectBuilder<T>
{

    private final ConfigurationModel configurationModel;
    private final ResolverSet resolverSet;
    private final List<ValueSetter> groupValueSetters;
    private final List<ValueSetter> singleValueSetters;

    public ConfigurationObjectBuilder(ConfigurationModel configurationModel, ResolverSet resolverSet)
    {
        this.configurationModel = configurationModel;
        this.resolverSet = resolverSet;

        singleValueSetters = createSingleValueSetters(configurationModel, resolverSet);
        groupValueSetters = GroupValueSetter.settersFor(configurationModel);
    }

    private List<ValueSetter> createSingleValueSetters(ConfigurationModel configurationModel, ResolverSet resolverSet)
    {
        Class<?> prototypeClass = configurationModel.getInstantiator().getObjectType();

        return resolverSet.getResolvers().keySet().stream()
                .map(parameterModel -> {
                    Field field = getField(prototypeClass, parameterModel);

                    // if no field, then it means this is a group attribute
                    return field != null ? new SingleValueSetter(parameterModel, field) : null;
                })
                .filter(field -> field != null)
                .collect(new ImmutableListCollector<>());
    }

    @Override
    public T build(MuleEvent event) throws MuleException
    {
        return build(resolverSet.resolve(event));
    }

    public T build(ResolverSetResult result) throws MuleException
    {
        T configuration = instantiateObject();

        setValues(configuration, result, groupValueSetters);
        setValues(configuration, result, singleValueSetters);

        return configuration;
    }

    /**
     * Creates a new instance by calling {@link ConfigurationModel#getInstantiator()#instantiateObject()}
     * {@inheritDoc}
     */
    @Override
    protected T instantiateObject()
    {
        return (T) configurationModel.getInstantiator().newInstance();
    }

    private void setValues(Object target, ResolverSetResult result, List<ValueSetter> setters) throws MuleException
    {
        for (ValueSetter setter : setters)
        {
            setter.set(target, result);
        }
    }
}
