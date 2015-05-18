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
import org.mule.extension.introspection.Operation;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.extension.runtime.OperationContext;
import org.mule.module.extension.internal.runtime.ConfigurationObjectBuilder;
import org.mule.module.extension.internal.runtime.DynamicConfigurationInstanceProvider;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.module.extension.internal.runtime.StaticConfigurationInstanceProvider;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.util.TemplateParser;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
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

public final class MuleExtensionUtils
{

    public static void checkNullOrRepeatedNames(Collection<? extends Described> describedCollection, String describedEntityName)
    {
        Set<String> repeatedNames = collectRepeatedNames(describedCollection, describedEntityName);

        if (!repeatedNames.isEmpty())
        {
            throw new IllegalArgumentException(
                    String.format("The following %s were declared multiple times: [%s]",
                                  describedEntityName,
                                  Joiner.on(", ").join(repeatedNames))
            );
        }
    }

    /**
     * Verifies that no operation has the same name as a configuration. This
     * method assumes that the configurations and operations provided have
     * already been verified through {@link #checkNullOrRepeatedNames(java.util.Collection, String)}
     * which means that name clashes can only occur against each other and not within the
     * inner elements of each collection
     */
    public static void checkNamesClashes(Collection<Configuration> configurations, Collection<Operation> operations)
    {
        List<Described> all = new ArrayList<>(configurations.size() + operations.size());
        all.addAll(configurations);
        all.addAll(operations);

        Set<String> clashes = collectRepeatedNames(all, "operations");
        if (!clashes.isEmpty())
        {
            throw new IllegalArgumentException(
                    String.format("The following operations have the same name as a declared configuration: [%s]",
                                  Joiner.on(", ").join(clashes))
            );
        }
    }

    private static Set<String> collectRepeatedNames(Collection<? extends Described> describedCollection, String describedEntityName)
    {
        if (CollectionUtils.isEmpty(describedCollection))
        {
            return ImmutableSet.of();
        }

        Multiset<String> names = LinkedHashMultiset.create();

        for (Described described : describedCollection)
        {
            checkArgument(described != null, String.format("A null %s was provided", describedEntityName));
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

    public static <T> List<T> immutableList(Collection<T> collection)
    {
        return collection != null ? ImmutableList.copyOf(collection) : ImmutableList.<T>of();
    }

    public static <T extends Described> Map<String, T> toMap(List<T> objects)
    {
        ImmutableMap.Builder<String, T> map = ImmutableMap.builder();
        for (T object : objects)
        {
            map.put(object.getName(), object);
        }

        return map.build();
    }

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

    public static boolean isSimpleExpression(String expression, TemplateParser parser)
    {
        TemplateParser.PatternInfo style = parser.getStyle();
        return expression.startsWith(style.getPrefix()) && expression.endsWith(style.getSuffix());
    }

    public static boolean containsExpression(String expression, TemplateParser parser)
    {
        return parser.isContainsTemplate(expression);
    }

    public static boolean isExpression(Object value, TemplateParser parser)
    {
        if (value instanceof String)
        {
            String maybeExpression = (String) value;
            return isSimpleExpression(maybeExpression, parser) || containsExpression(maybeExpression, parser);
        }

        return false;
    }

    public static <T extends Described> List<T> alphaSortDescribedList(List<T> list)
    {
        if (CollectionUtils.isEmpty(list))
        {
            return list;
        }

        Collections.sort(list, new DescribedComparator());
        return list;
    }

    public static <T> ConfigurationInstanceProvider<T> createConfigurationInstanceProvider(String name,
                                                                                           Configuration configuration,
                                                                                           ResolverSet resolverSet,
                                                                                           MuleContext muleContext) throws Exception
    {
        ConfigurationObjectBuilder configurationObjectBuilder = new ConfigurationObjectBuilder(configuration, resolverSet);

        if (resolverSet.isDynamic())
        {
            return new DynamicConfigurationInstanceProvider<>(name, configuration, configurationObjectBuilder, resolverSet);
        }
        else
        {
            MuleEvent event = new DefaultMuleEvent(new DefaultMuleMessage(null, muleContext), REQUEST_RESPONSE, (FlowConstruct) null);
            return new StaticConfigurationInstanceProvider<>(name, configuration, (T) configurationObjectBuilder.build(event));
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

    private static class DescribedComparator implements Comparator<Described>
    {

        @Override
        public int compare(Described o1, Described o2)
        {
            return o1.getName().compareTo(o2.getName());
        }
    }
}
