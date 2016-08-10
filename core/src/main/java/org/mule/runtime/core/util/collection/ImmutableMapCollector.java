/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.collection;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * {@link Collector} which returns an {@link ImmutableMap}
 *
 * @param <T> the generic type of input elements
 * @param <K> the output map's key type
 * @param <V> the output map's values type
 * @since 4.0
 */
public class ImmutableMapCollector<T, K, V> implements Collector<T, ImmutableMap.Builder<K, V>, Map<K, V>>
{

    private final Function<T, K> keyMapper;
    private final Function<T, V> valueMapper;

    /**
     * Creates a new instance
     *
     * @param keyMapper   a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     */
    public ImmutableMapCollector(Function<T, K> keyMapper, Function<T, V> valueMapper)
    {
        this.keyMapper = keyMapper;
        this.valueMapper = valueMapper;
    }

    @Override
    public Supplier<ImmutableMap.Builder<K, V>> supplier()
    {
        return ImmutableMap::builder;
    }

    @Override
    public BiConsumer<ImmutableMap.Builder<K, V>, T> accumulator()
    {
        return (builder, value) -> builder.put(keyMapper.apply(value), valueMapper.apply(value));
    }

    @Override
    public BinaryOperator<ImmutableMap.Builder<K, V>> combiner()
    {
        return (left, right) -> left.putAll(right.build());
    }

    @Override
    public Function<ImmutableMap.Builder<K, V>, Map<K, V>> finisher()
    {
        return ImmutableMap.Builder::build;
    }

    @Override
    public Set<Characteristics> characteristics()
    {
        return ImmutableSet.of();
    }
}
