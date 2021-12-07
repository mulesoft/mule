/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer;

import static org.mule.runtime.api.metadata.DataType.builder;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.noTransformerFoundForMessage;
import static org.mule.runtime.core.internal.registry.TransformerResolver.RegistryAction.ADDED;

import static java.util.Collections.sort;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.registry.TransformerResolver;
import org.mule.runtime.core.internal.registry.TypeBasedTransformerResolver;
import org.mule.runtime.core.privileged.transformer.TransformersRegistry;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;

public class DefaultTransformersRegistry implements TransformersRegistry {

  /**
   * We cache transformer searches so that we only search once
   */
  private final Map<String, Transformer> exactTransformerCache = new ConcurrentHashMap<>(8);
  private final Map<String, List<Transformer>> transformerListCache = new ConcurrentHashMap<>(8);

  private final ReadWriteLock transformerResolversLock = new ReentrantReadWriteLock();

  /**
   * Transformer transformerResolvers are registered on context start, then they are not unregistered.
   */
  @Inject
  private final List<TransformerResolver> transformerResolvers = new ArrayList<>();

  private final ReadWriteLock transformersLock = new ReentrantReadWriteLock();

  @Inject
  private final Collection<Transformer> transformers = new CopyOnWriteArrayList<>();

  @Override
  public Transformer lookupTransformer(DataType source, DataType result) throws TransformerException {
    // To maintain the previous behaviour, we don't want to consider the result mimeType when resolving a transformer
    // and only find transformers with a targetType the same as or a super class of the expected one.
    // The same could be done for the source but since if the source expected by the transformer is more generic that
    // the provided, it will be found.
    result = builder(result).mediaType(ANY).charset((Charset) null).build();

    final String dataTypePairHash = getDataTypeSourceResultPairHash(source, result);
    Transformer cachedTransformer = exactTransformerCache.get(dataTypePairHash);
    if (cachedTransformer != null) {
      return cachedTransformer;
    }

    Transformer trans = resolveTransformer(source, result);

    if (trans != null) {
      Transformer concurrentlyAddedTransformer = exactTransformerCache.putIfAbsent(dataTypePairHash, trans);
      if (concurrentlyAddedTransformer != null) {
        return concurrentlyAddedTransformer;
      } else {
        return trans;
      }
    } else {
      throw new TransformerException(noTransformerFoundForMessage(source, result));
    }
  }

  private Transformer resolveTransformer(DataType source, DataType result) throws TransformerException {
    Lock readLock = transformerResolversLock.readLock();
    readLock.lock();

    try {
      for (TransformerResolver resolver : transformerResolvers) {
        try {
          Transformer trans = resolver.resolve(source, result);
          if (trans != null) {
            return trans;
          }
        } catch (ResolverException e) {
          throw new TransformerException(noTransformerFoundForMessage(source, result), e);
        }
      }
    } finally {
      readLock.unlock();
    }

    return null;
  }

  @Override
  public List<Transformer> lookupTransformers(DataType source, DataType result) {
    // To maintain the previous behaviour, we don't want to consider the result mimeType when resolving a transformer
    // and only find transformers with a targetType the same as or a super class of the expected one.
    // The same could be done for the source but since if the source expected by the transformer is more generic that
    // the provided, it will be found.
    result = builder(result).mediaType(ANY).charset((Charset) null).build();

    final String dataTypePairHash = getDataTypeSourceResultPairHash(source, result);

    List<Transformer> results = transformerListCache.get(dataTypePairHash);
    if (results != null) {
      return results;
    }

    results = new ArrayList<>(2);

    Lock readLock = transformersLock.readLock();
    readLock.lock();
    try {
      for (Transformer transformer : transformers) {
        // The transformer must have the DiscoveryTransformer interface if we are
        // going to find it here
        if (!(transformer instanceof Converter)) {
          continue;
        }
        if (result.isCompatibleWith(transformer.getReturnDataType()) && transformer.isSourceDataTypeSupported(source)) {
          results.add(transformer);
        }
      }
    } finally {
      readLock.unlock();
    }

    List<Transformer> concurrentlyAddedTransformers = transformerListCache.putIfAbsent(dataTypePairHash, results);
    if (concurrentlyAddedTransformers != null) {
      return concurrentlyAddedTransformers;
    }

    return results;
  }

  @Override
  public void registerTransformer(Transformer transformer) throws MuleException {
    if (transformer instanceof Converter) {
      notifyTransformerResolvers(transformer, ADDED);
    }
  }

  public void registerTransformerResolver(TransformerResolver value) {
    Lock lock = transformerResolversLock.writeLock();
    lock.lock();
    try {
      transformerResolvers.add(value);
      sort(transformerResolvers, new TransformerResolverComparator());
    } finally {
      lock.unlock();
    }
  }

  private void notifyTransformerResolvers(Transformer t, TransformerResolver.RegistryAction action) {
    if (t instanceof Converter) {
      Lock transformerResolversReadLock = transformerResolversLock.readLock();
      transformerResolversReadLock.lock();
      try {

        for (TransformerResolver resolver : transformerResolvers) {
          resolver.transformerChange(t, action);
        }
      } finally {
        transformerResolversReadLock.unlock();
      }

      transformerListCache.clear();
      exactTransformerCache.clear();

      Lock transformersWriteLock = transformersLock.writeLock();
      transformersWriteLock.lock();
      try {
        if (action == ADDED) {
          transformers.add(t);
        } else {
          transformers.remove(t);
        }
      } finally {
        transformersWriteLock.unlock();
      }
    }
  }

  private String getDataTypeSourceResultPairHash(DataType source, DataType result) {
    return source.getClass().getName() + source.hashCode() + ":" + result.getClass().getName() + result.hashCode();
  }

  private class TransformerResolverComparator implements Comparator<TransformerResolver> {

    @Override
    public int compare(TransformerResolver transformerResolver, TransformerResolver transformerResolver1) {
      if (transformerResolver.getClass().equals(TypeBasedTransformerResolver.class)) {
        return 1;
      }

      if (transformerResolver1.getClass().equals(TypeBasedTransformerResolver.class)) {
        return -1;
      }
      return 0;
    }
  }
}
