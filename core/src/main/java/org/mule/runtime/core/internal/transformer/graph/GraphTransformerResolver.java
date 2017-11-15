/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.graph;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.registry.TransformerResolver;
import org.mule.runtime.core.internal.transformer.ResolverException;
import org.mule.runtime.core.api.config.i18n.CoreMessages;

import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.collections.map.LRUMap;

public class GraphTransformerResolver implements TransformerResolver {

  private ReentrantReadWriteLock readWriteLock;
  private SynchronizedTransformationGraph graph;
  private CompositeConverterFilter converterFilter;
  private LRUMap cache;
  private TransformationGraphLookupStrategy lookupStrategyTransformation;

  public GraphTransformerResolver() {
    this.readWriteLock = new ReentrantReadWriteLock();
    this.graph = new SynchronizedTransformationGraph();
    lookupStrategyTransformation = new TransformationGraphLookupStrategy(graph);
    converterFilter = new CompositeConverterFilter(new TypeMatchingVertexesFilter(),
                                                   new TransformationLengthConverterFilter(),
                                                   new PriorityWeightingConverterFilter(),
                                                   new NameConverterFilter());
    cache = new LRUMap();
  }

  @Override
  public Transformer resolve(DataType source, DataType result) throws ResolverException {
    String cacheKey = getDataTypeSourceResultPairHash(source, result);

    readWriteLock.readLock().lock();
    try {
      if (cache.containsKey(cacheKey)) {
        return (Converter) cache.get(cacheKey);
      }
    } finally {
      readWriteLock.readLock().unlock();
    }

    List<Converter> converters =
        converterFilter.filter(lookupStrategyTransformation.lookupConverters(source, result), source, result);

    if (converters.size() > 1) {
      throw new ResolverException(CoreMessages.transformHasMultipleMatches(source.getType(), result.getType(), converters));
    }

    Transformer converter = (converters.size() == 0) ? null : converters.get(0);


    readWriteLock.writeLock().lock();
    try {
      cache.put(cacheKey, converter);
    } finally {
      readWriteLock.writeLock().unlock();
    }

    return converter;
  }

  private String getDataTypeSourceResultPairHash(DataType source, DataType result) {
    StringBuilder builder = new StringBuilder();
    builder.append(source.getClass().getName());
    builder.append(source.hashCode());
    builder.append(":");
    builder.append(result.getClass().getName());
    builder.append(result.hashCode());

    return builder.toString();
  }

  @Override
  public void transformerChange(Transformer transformer, RegistryAction registryAction) {
    readWriteLock.writeLock().lock();

    try {
      if (!(transformer instanceof Converter)) {
        return;
      }

      cache.clear();

      if (registryAction == RegistryAction.ADDED) {
        graph.addConverter((Converter) transformer);
      } else if (registryAction == RegistryAction.REMOVED) {
        graph.removeConverter((Converter) transformer);
      }
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }
}
