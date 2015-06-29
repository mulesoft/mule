/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.resolver;

import static org.mule.module.extension.internal.util.IntrospectionUtils.checkInstantiable;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.module.extension.internal.util.MuleExtensionUtils;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A {@link ValueResolver} that takes a list of {@link ValueResolver}s
 * and upon invocation of {@link #resolve(MuleEvent)} it return a
 * {@link Collection} of values with the outcome of each original resolver.
 * <p/>
 * This class implements {@link Lifecycle} and propagates those events to each
 * of the {@code resolvers}
 *
 * @param <T> the generic type for the items of the returned {@link Collection}
 * @since 3.7.0
 */
public final class CollectionValueResolver<T> implements ValueResolver<Collection<T>>
{

    private final List<ValueResolver<T>> resolvers;
    private final Class<? extends Collection> collectionType;

    public static <T> CollectionValueResolver<T> of(Class<? extends Collection> collectionType, List<ValueResolver<T>> resolvers)
    {
        if (List.class.equals(collectionType) || Collection.class.equals(collectionType) || Iterable.class.equals(collectionType))
        {
            return new CollectionValueResolver<>(ArrayList.class, resolvers);
        }
        else if (Set.class.equals(collectionType))
        {
            return new CollectionValueResolver<>(HashSet.class, resolvers);
        }
        else
        {
            return new CollectionValueResolver<>(collectionType, resolvers);
        }
    }

    /**
     * Creates a new instance
     *
     * @param collectionType the {@link Class} for a concrete {@link Collection} type with a default constructor
     * @param resolvers      a not {@code null} {@link List} of resolvers
     */
    public CollectionValueResolver(Class<? extends Collection> collectionType, List<ValueResolver<T>> resolvers)
    {
        checkInstantiable(collectionType);
        checkArgument(resolvers != null, "resolvers cannot be null");

        this.collectionType = collectionType;
        this.resolvers = ImmutableList.copyOf(resolvers);
    }

    /**
     * Passes the given {@code event} to each resolvers and outputs
     * a collection of type {@code collectionType} with each result
     *
     * @param event a {@link MuleEvent} the event to evaluate
     * @return a {@link Collection} of type {@code collectionType}
     * @throws MuleException
     */
    @Override
    public Collection<T> resolve(MuleEvent event) throws MuleException
    {
        Collection<T> collection = instantiateCollection();
        for (ValueResolver<T> resolver : resolvers)
        {
            collection.add(resolver.resolve(event));
        }

        return collection;
    }

    /**
     * @return {@code true} if at least one of the {@code resolvers} are dynamic
     */
    @Override
    public boolean isDynamic()
    {
        return MuleExtensionUtils.hasAnyDynamic(resolvers);
    }

    private Collection<T> instantiateCollection()
    {
        try
        {
            return collectionType.newInstance();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Could not create instance of " + collectionType.getName(), e);
        }
    }
}
