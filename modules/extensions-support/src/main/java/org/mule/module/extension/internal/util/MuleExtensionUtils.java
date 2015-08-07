/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.util;

import static org.mule.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.construct.FlowConstruct;
import org.mule.extension.annotations.param.Optional;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.Described;
import org.mule.extension.introspection.Extension;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.extension.runtime.ConfigurationInstanceRegistrationCallback;
import org.mule.extension.runtime.OperationContext;
import org.mule.module.extension.internal.runtime.ConfigurationObjectBuilder;
import org.mule.module.extension.internal.runtime.DynamicConfigurationInstanceProvider;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.module.extension.internal.runtime.StaticConfigurationInstanceProvider;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.util.ArrayUtils;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;

/**
 * Utilities for handling {@link Extension extensions}
 *
 * @since 3.7.0
 */
public class MuleExtensionUtils
{

    private MuleExtensionUtils()
    {
    }

    /**
     * Verifies that none of the {@link Described} items in {@code describedCollection} have an
     * equivalent value for {@link Described#getName()}
     *
     * @param describedCollections an array of {@link Collection}s with instances of {@link Described}
     * @throws IllegalArgumentException if the validation fails
     */
    public static void validateRepeatedNames(Collection<? extends Described>... describedCollections)
    {
        if (ArrayUtils.isEmpty(describedCollections))
        {
            return;
        }

        List<Described> all = new ArrayList<>();
        for (Collection<? extends Described> describedCollection : describedCollections)
        {
            all.addAll(describedCollection);
        }

        Set<String> clashes = collectRepeatedNames(all);
        if (!clashes.isEmpty())
        {
            throw new IllegalArgumentException("The following names have been assigned to multiple components: " + Joiner.on(", ").join(clashes));
        }
    }

    /**
     * Returns a {@link Map} in which the keys are the {@link Described#getName() names} of the items
     * in the {@code objects} {@link List}, and the values are the items themselves.
     *
     * @param objects a {@link List} with items that implement the {@link Described} interface
     * @param <T>     the generic type of the items in {@code objects}
     * @return a {@link Map} in which the keys are the item's names and the values are the items
     */
    public static <T extends Described> Map<String, T> toMap(List<T> objects)
    {
        ImmutableMap.Builder<String, T> map = ImmutableMap.builder();
        for (T object : objects)
        {
            map.put(object.getName(), object);
        }

        return map.build();
    }

    /**
     * Returns {@code true} if any of the items in {@code resolvers} return true for the
     * {@link ValueResolver#isDynamic()} method
     *
     * @param resolvers a {@link Iterable} with instances of {@link ValueResolver}
     * @param <T>       the generic type of the {@link ValueResolver} items
     * @return {@code true} if at least one {@link ValueResolver} is dynamic, {@code false} otherwise
     */
    public static <T extends Object> boolean hasAnyDynamic(Iterable<ValueResolver<T>> resolvers)
    {
        for (ValueResolver resolver : resolvers)
        {
            if (resolver.isDynamic())
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Sorts the given {@code list} in ascending alphabetic order, using {@link Described#getName()}
     * as the sorting criteria
     *
     * @param list a {@link List} with instances of {@link Described}
     * @param <T>  the generic type of the items in the {@code list}
     * @return the sorted {@code list}
     */
    public static <T extends Described> List<T> alphaSortDescribedList(List<T> list)
    {
        if (CollectionUtils.isEmpty(list))
        {
            return list;
        }

        Collections.sort(list, new DescribedComparator());
        return list;
    }

    public static <T> ConfigurationInstanceProvider<T> createConfigurationInstanceProvider(
            String name,
            Extension extension,
            Configuration configuration,
            ResolverSet resolverSet,
            MuleContext muleContext,
            ConfigurationInstanceRegistrationCallback registrationCallback) throws Exception
    {
        ConfigurationObjectBuilder configurationObjectBuilder = new ConfigurationObjectBuilder(name, extension, configuration, resolverSet, registrationCallback);

        if (resolverSet.isDynamic())
        {
            return new DynamicConfigurationInstanceProvider<>(configurationObjectBuilder, resolverSet);
        }
        else
        {
            MuleEvent event = new DefaultMuleEvent(new DefaultMuleMessage(null, muleContext), REQUEST_RESPONSE, (FlowConstruct) null);
            return new StaticConfigurationInstanceProvider<>((T) configurationObjectBuilder.build(event));
        }
    }

    public static OperationContextAdapter asOperationContextAdapter(OperationContext operationContext)
    {
        checkArgument(operationContext != null, "operationContext cannot be null");
        if (!(operationContext instanceof OperationContextAdapter))
        {
            throw new IllegalArgumentException(String.format("operationContext was expected to be an instance of %s but got %s instead",
                                                             OperationContextAdapter.class.getName(), operationContext.getClass().getName()));
        }

        return (OperationContextAdapter) operationContext;
    }

    public static String getDefaultValue(Optional optional)
    {
        if (optional == null)
        {
            return null;
        }

        String defaultValue = optional.defaultValue();
        return Optional.NULL.equals(defaultValue) ? null : defaultValue;
    }

    public static MuleEvent getInitialiserEvent(MuleContext muleContext)
    {
        return new DefaultMuleEvent(new DefaultMuleMessage(null, muleContext), REQUEST_RESPONSE, (FlowConstruct) null);
    }

    private static Set<String> collectRepeatedNames(Collection<? extends Described> describedCollection)
    {
        if (CollectionUtils.isEmpty(describedCollection))
        {
            return ImmutableSet.of();
        }

        Multiset<String> names = LinkedHashMultiset.create();

        for (Described described : describedCollection)
        {
            if (described == null)
            {
                throw new IllegalArgumentException("A null described was provided");
            }
            names.add(described.getName());
        }

        names = Multisets.copyHighestCountFirst(names);
        Set<String> repeatedNames = new HashSet<>();
        for (String name : names)
        {
            if (names.count(name) == 1)
            {
                break;
            }

            repeatedNames.add(name);
        }

        return repeatedNames;
    }

    private static class DescribedComparator implements Comparator<Described>
    {

        @Override
        public int compare(Described o1, Described o2)
        {
            return o1.getName().compareTo(o2.getName());
        }
    }
}
