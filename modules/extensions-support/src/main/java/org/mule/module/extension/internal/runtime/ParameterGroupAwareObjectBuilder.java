/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import static org.mule.module.extension.internal.util.IntrospectionUtils.getField;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.extension.api.introspection.EnrichableModel;
import org.mule.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.module.extension.internal.util.GroupValueSetter;
import org.mule.module.extension.internal.util.SingleValueSetter;
import org.mule.module.extension.internal.util.ValueSetter;
import org.mule.util.collection.ImmutableListCollector;

import java.lang.reflect.Field;
import java.util.List;

/**
 * A specialization of {@link BaseObjectBuilder} which generates object based on
 * an {@link EnrichableModel} for with parameter groups have been defined based on
 * a {@link ParameterGroupModelProperty}
 *
 * @param <T> the generic type of the instances to be produced
 * @since 4.0
 */
public abstract class ParameterGroupAwareObjectBuilder<T> extends BaseObjectBuilder<T>
{

    protected final ResolverSet resolverSet;
    private final List<ValueSetter> singleValueSetters;
    private final List<ValueSetter> groupValueSetters;

    public ParameterGroupAwareObjectBuilder(Class<?> prototypeClass, EnrichableModel model, ResolverSet resolverSet)
    {
        this.resolverSet = resolverSet;
        singleValueSetters = createSingleValueSetters(prototypeClass, resolverSet);
        groupValueSetters = GroupValueSetter.settersFor(model);
    }

    @Override
    public final T build(MuleEvent event) throws MuleException
    {
        return build(resolverSet.resolve(event));
    }

    public T build(ResolverSetResult result) throws MuleException
    {
        T object = instantiateObject();

        setValues(object, result, groupValueSetters);
        setValues(object, result, singleValueSetters);

        return object;
    }

    private List<ValueSetter> createSingleValueSetters(Class<?> prototypeClass, ResolverSet resolverSet)
    {
        return resolverSet.getResolvers().keySet().stream()
                .map(parameterModel -> {
                    Field field = getField(prototypeClass, parameterModel);

                    // if no field, then it means this is a group attribute
                    return field != null ? new SingleValueSetter(parameterModel, field) : null;
                })
                .filter(field -> field != null)
                .collect(new ImmutableListCollector<>());
    }

    private void setValues(Object target, ResolverSetResult result, List<ValueSetter> setters) throws MuleException
    {
        for (ValueSetter setter : setters)
        {
            setter.set(target, result);
        }
    }
}
