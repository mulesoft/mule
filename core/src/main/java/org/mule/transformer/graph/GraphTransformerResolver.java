/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.graph;

import org.mule.api.registry.ResolverException;
import org.mule.api.registry.TransformerResolver;
import org.mule.api.transformer.Converter;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.config.i18n.CoreMessages;

import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.collections.map.LRUMap;

public class GraphTransformerResolver implements TransformerResolver
{

    private ReentrantReadWriteLock readWriteLock;
    private TransformationGraph graph;
    private CompositeConverterFilter converterFilter;
    private LRUMap cache;
    private TransformationGraphLookupStrategy lookupStrategyTransformation;

    public GraphTransformerResolver()
    {
        this.readWriteLock = new ReentrantReadWriteLock();
        this.graph = new TransformationGraph();
        lookupStrategyTransformation = new TransformationGraphLookupStrategy(graph);
        converterFilter = new CompositeConverterFilter(new TransformationLengthConverterFilter(), new PriorityWeightingConverterFilter(), new NameConverterFilter());
        cache = new LRUMap();
    }

    @Override
    public Transformer resolve(DataType<?> source, DataType<?> result) throws ResolverException
    {
        String cacheKey = getDataTypeSourceResultPairHash(source, result);

        readWriteLock.readLock().lock();
        try
        {
            if (cache.containsKey(cacheKey))
            {
                return (Converter) cache.get(cacheKey);
            }
        }
        finally
        {
            readWriteLock.readLock().unlock();
        }

        List<Converter> converters = converterFilter.filter(lookupStrategyTransformation.lookupConverters(source, result), source, result);

        if (converters.size() > 1)
        {
            throw new ResolverException(CoreMessages.transformHasMultipleMatches(source.getType(), result.getType(), converters.get(0), converters.get(1)));
        }

        Transformer converter = (converters.size() == 0) ? null : converters.get(0);


        readWriteLock.writeLock().lock();
        try
        {
            cache.put(cacheKey, converter);
        }
        finally
        {
            readWriteLock.writeLock().unlock();
        }

        return converter;
    }

    private String getDataTypeSourceResultPairHash(DataType<?> source, DataType<?> result)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(source.getClass().getName());
        builder.append(source.hashCode());
        builder.append(":");
        builder.append(result.getClass().getName());
        builder.append(result.hashCode());

        return builder.toString();
    }

    @Override
    public void transformerChange(Transformer transformer, RegistryAction registryAction)
    {
        readWriteLock.writeLock().lock();

        try
        {
            if (!(transformer instanceof Converter))
            {
                return;
            }

            cache.clear();

            if (registryAction == RegistryAction.ADDED)
            {
                graph.addConverter((Converter) transformer);
            }
            else if (registryAction == RegistryAction.REMOVED)
            {
                graph.removeConverter((Converter) transformer);
            }
        }
        finally
        {
            readWriteLock.writeLock().unlock();
        }
    }
}
