/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal;

import org.mule.extensions.introspection.spi.Builder;
import org.mule.extensions.introspection.api.Described;
import org.mule.util.Preconditions;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;

abstract class MuleExtensionUtils
{

    static void checkNullOrRepeatedNames(Collection<? extends Described> describedCollection, String describedEntityName)
    {
        Preconditions.checkState(describedCollection != null && !describedCollection.isEmpty(), String.format("Must provide at least one %s", describedEntityName));

        Multiset<String> names = LinkedHashMultiset.create();

        for (Described described : describedCollection)
        {
            Preconditions.checkArgument(described != null, String.format("A null %s was provided", describedEntityName));
            names.add(described.getName());
        }


        names = Multisets.copyHighestCountFirst(names);
        Set<String> repeatedNames = new HashSet<String>();
        for (String name : names)
        {
            if (names.count(name) == 1)
            {
                break;
            }

            repeatedNames.add(name);
        }

        if (!repeatedNames.isEmpty())
        {
            throw new IllegalArgumentException(
                    String.format("The following %s were declared multiple times: [%s]",
                                  describedEntityName,
                                  Joiner.on(", ").join(repeatedNames))
            );
        }
    }

    static <T> List<T> build(Collection<? extends Builder<T>> builders)
    {
        if (CollectionUtils.isEmpty(builders))
        {
            return Collections.emptyList();
        }

        List<T> built = new ArrayList<T>(builders.size());
        for (Builder<T> builder : builders)
        {
            built.add(builder.build());
        }

        return built;
    }

    static <T> List<T> immutableList(Collection<T> collection)
    {
        return ImmutableList.copyOf(collection != null ? collection : Collections.<T>emptyList());
    }

    static <T extends Described> Map<String, T> toMap(List<T> objects)
    {
        ImmutableMap.Builder<String, T> map = ImmutableMap.builder();
        for (T object : objects)
        {
            map.put(object.getName(), object);
        }

        return map.build();
    }

}
