/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer;

import static org.mule.runtime.api.metadata.DataType.builder;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.noTransformerFoundForMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.internal.registry.TransformerResolver.RegistryAction.ADDED;

import static java.util.Collections.emptyList;
import static java.util.Collections.sort;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.registry.TransformerResolver;
import org.mule.runtime.core.internal.registry.TypeBasedTransformerResolver;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Inject;

/**
 * Implementation of {@link TransformersRegistry} with resolution caches.
 *
 * @since 4.5
 */
public class DefaultTransformersRegistry implements TransformersRegistry, Initialisable, Disposable {

  @Inject
  private MuleContext muleContext;

  /**
   * We cache transformer searches so that we only search once
   */
  private final Map<String, Transformer> exactTransformerCache = new ConcurrentHashMap<>(8);
  private final Map<String, List<Transformer>> transformerListCache = new ConcurrentHashMap<>(8);

  /**
   * Transformer transformerResolvers are registered on context start, then they are not unregistered.
   */
  private List<TransformerResolver> transformerResolvers = emptyList();

  private Collection<Transformer> transformers = emptyList();

  @Override
  public void initialise() throws InitialisationException {
    transformers.stream()
        .filter(t -> t instanceof Converter)
        .forEach(t -> notifyTransformerResolvers(t));
    sort(transformerResolvers, new TransformerResolverComparator());

    clearCaches();
  }

  @Override
  public void dispose() {
    clearCaches();
  }

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

    List<Transformer> concurrentlyAddedTransformers = transformerListCache.putIfAbsent(dataTypePairHash, results);
    if (concurrentlyAddedTransformers != null) {
      return concurrentlyAddedTransformers;
    }

    return results;
  }

  @Override
  public void registerTransformer(Transformer transformer) throws MuleException {
    initialiseIfNeeded(transformer, muleContext);
    if (transformer instanceof Converter) {
      notifyTransformerResolvers(transformer);
    }

    clearCaches();
  }

  protected void clearCaches() {
    transformerListCache.clear();
    exactTransformerCache.clear();
  }

  public void notifyTransformerResolvers(Transformer t) {
    if (t instanceof Converter) {
      for (TransformerResolver resolver : transformerResolvers) {
        resolver.transformerChange(t, ADDED);
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

  @Inject
  public void setTransformerResolvers(List<TransformerResolver> transformerResolvers) {
    this.transformerResolvers = transformerResolvers;
  }

  @Inject
  public void setTransformers(Collection<Transformer> transformers) {
    this.transformers = transformers;
  }
}
