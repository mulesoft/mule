/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.util;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.extensions.introspection.Configuration;
import org.mule.extensions.introspection.Described;
import org.mule.extensions.introspection.Operation;
import org.mule.extensions.introspection.Parameter;
import org.mule.module.extensions.internal.runtime.resolver.ValueResolver;
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

    public static void checkSetters(Class<?> declaringClass, Collection<Parameter> parameters)
    {
        Set<Parameter> faultParameters = new HashSet<>(parameters.size());
        for (Parameter parameter : parameters)
        {
            if (!IntrospectionUtils.hasSetter(declaringClass, parameter))
            {
                faultParameters.add(parameter);
            }
        }

        if (!faultParameters.isEmpty())
        {
            StringBuilder message = new StringBuilder("The following attributes don't have a valid setter on class ")
                    .append(declaringClass.getName()).append(":\n");

            for (Parameter parameter : faultParameters)
            {
                message.append(parameter.getName()).append("\n");
            }

            throw new IllegalArgumentException(message.toString());
        }
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

    private static class DescribedComparator implements Comparator<Described>
    {

        @Override
        public int compare(Described o1, Described o2)
        {
            return o1.getName().compareTo(o2.getName());
        }
    }
}
