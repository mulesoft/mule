/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.module.extension.internal.runtime.ObjectBuilder;

import com.google.common.collect.ImmutableMap;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A {@link ValueResolver} which is based on associating a set of
 * {@link ParameterModel parameterModels} -&gt; {@link ValueResolver} pairs. The result
 * of evaluating this resolver is a {@link ResolverSetResult}.
 * <p/>
 * The general purpose of this class is to repeatedly evaluate a set of {@link ValueResolver}s
 * which results are to be used in the construction of an object, so that the structure
 * of such can be described only once (by the set of {@link ParameterModel parameterModels} and {@link ValueResolver}s
 * but evaluated many times. With this goal in mind is that the return value of this resolver
 * will always be a {@link ResolverSetResult} which then can be used by a {@link ObjectBuilder}
 * to generate an actual object.
 * <p/>
 * Instances of this class are to be considered thread safe and reusable
 *
 * @since 3.7.0
 */
public class ResolverSet implements ValueResolver<ResolverSetResult>
{

    private Map<ParameterModel, ValueResolver> resolvers = new LinkedHashMap<>();
    private boolean dynamic = false;

    /**
     * Links the given {@link ValueResolver} to the given {@link ParameterModel}.
     * If such {@code parameter} was already added, then the associated {@code resolver}
     * is replaced.
     *
     * @param parameterModel a not {@code null} {@link ParameterModel}
     * @param resolver  a not {@code null} {@link ValueResolver}
     * @return this resolver set to allow chaining
     * @throws IllegalArgumentException is either {@code parameter} or {@code resolver} are {@code null}
     */
    public ResolverSet add(ParameterModel parameterModel, ValueResolver resolver)
    {
        checkArgument(parameterModel != null, "parameter cannot be null");
        checkArgument(resolver != null, "resolver cannot be null");

        if (resolvers.put(parameterModel, resolver) != null)
        {
            throw new IllegalStateException("A value was already given for parameter " + parameterModel.getName());
        }

        if (resolver.isDynamic())
        {
            dynamic = true;
        }
        return this;
    }

    /**
     * Whether at least one of the given {@link ValueResolver} are dynamic
     *
     * @return {@code true} if at least one resolver is dynamic. {@code false} otherwise
     */
    @Override
    public boolean isDynamic()
    {
        return dynamic;
    }

    /**
     * Evaluates all the added {@link ValueResolver}s and returns the results into
     * a {@link ResolverSetResult}
     *
     * @param event a not {@code null} {@link MuleEvent}
     * @return a {@link ResolverSetResult}
     * @throws Exception
     */
    @Override
    public ResolverSetResult resolve(MuleEvent event) throws MuleException
    {
        ResolverSetResult.Builder builder = ResolverSetResult.newBuilder();
        for (Map.Entry<ParameterModel, ValueResolver> entry : resolvers.entrySet())
        {
            builder.add(entry.getKey(), entry.getValue().resolve(event));
        }

        return builder.build();
    }

    public Map<ParameterModel, ValueResolver> getResolvers()
    {
        return ImmutableMap.copyOf(resolvers);
    }
}
