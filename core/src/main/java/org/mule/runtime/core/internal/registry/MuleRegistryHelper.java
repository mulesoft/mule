/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import static org.mule.runtime.api.metadata.DataType.builder;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.core.internal.registry.TransformerResolver.RegistryAction.ADDED;
import static org.mule.runtime.core.privileged.util.BeanUtils.getName;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.transformer.ResolverException;
import org.mule.runtime.core.privileged.registry.RegistrationException;

import com.google.common.collect.ImmutableList;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds lookup/register/unregister methods for Mule-specific entities to the standard Registry interface.
 */
public class MuleRegistryHelper implements MuleRegistry, RegistryProvider {

  protected transient Logger logger = LoggerFactory.getLogger(MuleRegistryHelper.class);

  /**
   * A reference to Mule's internal registry
   */
  private DefaultRegistryBroker registry;

  /**
   * We cache transformer searches so that we only search once
   */
  protected ConcurrentHashMap/* <String, Transformer> */ exactTransformerCache =
      new ConcurrentHashMap/* <String, Transformer> */(8);
  protected ConcurrentHashMap/* Map<String, List<Transformer>> */ transformerListCache =
      new ConcurrentHashMap/* <String, List<Transformer>> */(8);

  private MuleContext muleContext;

  private final ReadWriteLock transformerResolversLock = new ReentrantReadWriteLock();

  /**
   * Transformer transformerResolvers are registered on context start, then they are not unregistered.
   */
  private List<TransformerResolver> transformerResolvers = new ArrayList<>();

  private final ReadWriteLock transformersLock = new ReentrantReadWriteLock();

  private Map<Object, Object> postProcessedObjects = new HashMap<>();

  /**
   * Transformers are registered on context start, then they are usually not unregistered
   */
  private Collection<Transformer> transformers = new CopyOnWriteArrayList<>();

  public MuleRegistryHelper(DefaultRegistryBroker registry, MuleContext muleContext) {
    this.registry = registry;
    this.muleContext = muleContext;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialise() throws InitialisationException {
    // no-op

    // This is called when the MuleContext starts up, and should only do initialisation for any state on this class, the lifecycle
    // for the registries will be handled by the LifecycleManager on the registry that this class wraps
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose() {
    transformerListCache.clear();
    exactTransformerCache.clear();
    registry.dispose();
  }

  @Override
  public void fireLifecycle(String phase) throws LifecycleException {
    if (Initialisable.PHASE_NAME.equals(phase)) {
      registry.initialise();
    } else if (Disposable.PHASE_NAME.equals(phase)) {
      registry.dispose();
    } else {
      registry.fireLifecycle(phase);
    }
  }

  public MuleContext getMuleContext() {
    return muleContext;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Transformer lookupTransformer(String name) {
    return (Transformer) registry.lookupObject(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Transformer lookupTransformer(DataType source, DataType result) throws TransformerException {
    //To maintain the previous behaviour, we don't want to consider the result mimeType when resolving a transformer
    //and only find transformers with a targetType the same as or a super class of the expected one.
    //The same could be done for the source but since if the source expected by the transformer is more generic that
    //the provided, it will be found.
    result = builder(result).mediaType(ANY).charset((Charset) null).build();

    final String dataTypePairHash = getDataTypeSourceResultPairHash(source, result);
    Transformer cachedTransformer = (Transformer) exactTransformerCache.get(dataTypePairHash);
    if (cachedTransformer != null) {
      return cachedTransformer;
    }

    Transformer trans = resolveTransformer(source, result);

    if (trans != null) {
      Transformer concurrentlyAddedTransformer = (Transformer) exactTransformerCache.putIfAbsent(dataTypePairHash, trans);
      if (concurrentlyAddedTransformer != null) {
        return concurrentlyAddedTransformer;
      } else {
        return trans;
      }
    } else {
      throw new TransformerException(CoreMessages.noTransformerFoundForMessage(source, result));
    }
  }

  protected Transformer resolveTransformer(DataType source, DataType result) throws TransformerException {
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
          throw new TransformerException(CoreMessages.noTransformerFoundForMessage(source, result), e);
        }
      }
    } finally {
      readLock.unlock();
    }

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Transformer> lookupTransformers(DataType source, DataType result) {
    //To maintain the previous behaviour, we don't want to consider the result mimeType when resolving a transformer
    //and only find transformers with a targetType the same as or a super class of the expected one.
    //The same could be done for the source but since if the source expected by the transformer is more generic that
    //the provided, it will be found.
    result = builder(result).mediaType(ANY).charset((Charset) null).build();

    final String dataTypePairHash = getDataTypeSourceResultPairHash(source, result);

    List<Transformer> results = (List<Transformer>) transformerListCache.get(dataTypePairHash);
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

    List<Transformer> concurrentlyAddedTransformers =
        (List<Transformer>) transformerListCache.putIfAbsent(dataTypePairHash, results);
    if (concurrentlyAddedTransformers != null) {
      return concurrentlyAddedTransformers;
    } else {
      return results;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FlowConstruct lookupFlowConstruct(String name) {
    return (FlowConstruct) registry.lookupObject(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<FlowConstruct> lookupFlowConstructs() {
    return lookupObjects(FlowConstruct.class);
  }

  @Override
  public boolean isSingleton(String key) {
    return registry.isSingleton(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void registerTransformer(Transformer transformer) throws MuleException {
    registerObject(getName(transformer), transformer, Transformer.class);
  }

  public void notifyTransformerResolvers(Transformer t, TransformerResolver.RegistryAction action) {
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

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerFlowConstruct(FlowConstruct flowConstruct) throws MuleException {
    registry.registerObject(getName(flowConstruct), flowConstruct, FlowConstruct.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregisterTransformer(String transformerName) throws MuleException {
    Transformer transformer = lookupTransformer(transformerName);
    notifyTransformerResolvers(transformer, TransformerResolver.RegistryAction.REMOVED);
    registry.unregisterObject(transformerName, Transformer.class);

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object applyProcessorsAndLifecycle(Object object) throws MuleException {
    object = applyProcessors(object);
    object = applyLifecycle(object);
    return object;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object applyProcessors(Object object) throws MuleException {
    return muleContext.getInjector().inject(object);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object applyLifecycle(Object object) throws MuleException {
    return applyLifecycle(object, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object applyLifecycle(Object object, String phase) throws MuleException {
    return withLifecycleRegistry(object, registry -> registry.applyLifecycle(object, phase));
  }

  @Override
  public void applyLifecycle(Object object, String startPhase, String toPhase) throws MuleException {
    withLifecycleRegistry(object, registry -> {
      registry.applyLifecycle(object, startPhase, toPhase);
      return object;
    });
  }

  private Object withLifecycleRegistry(Object object, LifecycleDelegate delegate) throws MuleException {
    LifecycleRegistry lifecycleRegistry = registry.getLifecycleRegistry();
    if (lifecycleRegistry != null) {
      return delegate.apply(lifecycleRegistry);
    }

    return object;
  }

  @FunctionalInterface
  private interface LifecycleDelegate {

    Object apply(LifecycleRegistry registry) throws MuleException;
  }

  ////////////////////////////////////////////////////////////////////////////
  // Delegate to internal registry
  ////////////////////////////////////////////////////////////////////////////

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T lookupObject(Class<T> type) throws RegistrationException {
    return registry.lookupObject(type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> T lookupObject(String key) {
    return (T) registry.lookupObject(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T lookupObject(String key, boolean applyLifecycle) {
    return (T) registry.lookupObject(key, applyLifecycle);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Collection<T> lookupObjects(Class<T> type) {
    return registry.lookupObjects(type);
  }

  @Override
  public <T> Collection<T> lookupLocalObjects(Class<T> type) {
    return registry.lookupLocalObjects(type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Collection<T> lookupObjectsForLifecycle(Class<T> type) {
    return registry.lookupObjectsForLifecycle(type);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T get(String key) {
    return (T) registry.get(key);
  }

  @Override
  public <T> Map<String, T> lookupByType(Class<T> type) {
    return registry.lookupByType(type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerObject(String key, Object value, Object metadata) throws RegistrationException {
    registry.registerObject(key, value, metadata);

    postObjectRegistrationActions(value);
  }

  public void postObjectRegistrationActions(Object value) {
    // TODO MULE-10238 - Remove this check once SimpleRegistry gets removed
    if (!postProcessedObjects.containsKey(value)) {
      postProcessedObjects.put(value, value);
      if (value instanceof TransformerResolver) {
        registerTransformerResolver((TransformerResolver) value);
      }

      if (value instanceof Converter) {
        notifyTransformerResolvers((Converter) value, ADDED);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerObject(String key, Object value) throws RegistrationException {
    registry.registerObject(key, value);


    postObjectRegistrationActions(value);
  }

  public void registerTransformerResolver(TransformerResolver value) {
    Lock lock = transformerResolversLock.writeLock();
    lock.lock();
    try {
      transformerResolvers.add(value);
      Collections.sort(transformerResolvers, new TransformerResolverComparator());
    } finally {
      lock.unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerObjects(Map objects) throws RegistrationException {
    registry.registerObjects(objects);

    for (Object value : objects.values()) {
      postObjectRegistrationActions(value);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object unregisterObject(String key, Object metadata) throws RegistrationException {
    return registry.unregisterObject(key, metadata);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object unregisterObject(String key) throws RegistrationException {
    return registry.unregisterObject(key);
  }

  @Override
  public Collection<Registry> getRegistries() {
    return ImmutableList.copyOf(registry.getRegistries());
  }

  ////////////////////////////////////////////////////////////////////////////
  // Registry Metadata
  ////////////////////////////////////////////////////////////////////////////

  /**
   * {@inheritDoc}
   */
  @Override
  public String getRegistryId() {
    return this.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isReadOnly() {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isRemote() {
    return false;
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


