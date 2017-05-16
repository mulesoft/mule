/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.checkInstantiable;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.hasAnyDynamic;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A {@link ValueResolver} that takes a list of {@link ValueResolver}s and upon invocation of {@link #resolve(ValueResolvingContext)} it return a
 * {@link Map} of values with the outcome of each original resolver.
 * <p/>
 * This class implements {@link Lifecycle} and propagates those events to each of the {@code resolvers}
 *
 * @param <K,V> the generic type for the items of the returned {@link Map}
 * @since 3.7.0
 */
public final class MapValueResolver<K, V> implements ValueResolver<Map<K, V>>, Initialisable {

  private final Class<? extends Map> mapType;
  private final List<ValueResolver<V>> valueResolvers;
  private final List<ValueResolver<K>> keyResolvers;
  private final MuleContext muleContext;

  /**
   * Creates a new instance
   * 
   * @param mapType the {@link Class} for a concrete {@link Map} type with a default constructor
   * @param keyResolvers a not {@code null} {@link List} of resolvers for map key params
   * @param valueResolvers a not {@code null} {@link List} of resolvers for map value params
   * @param muleContext the artifact {@link MuleContext} that will be used for initialisation of resolvers
   */
  public MapValueResolver(Class<? extends Map> mapType, List<ValueResolver<K>> keyResolvers,
                          List<ValueResolver<V>> valueResolvers, MuleContext muleContext) {
    checkInstantiable(mapType);
    checkArgument(keyResolvers != null && valueResolvers != null, "resolvers cannot be null");
    checkArgument(keyResolvers.size() == valueResolvers.size(), "exactly one valueResolver for each keyResolver is required");

    this.mapType = mapType;
    this.keyResolvers = keyResolvers;
    this.valueResolvers = valueResolvers;
    this.muleContext = muleContext;
  }

  public static <K, V> MapValueResolver<K, V> of(Class<? extends Map> mapType, List<ValueResolver<K>> keyResolvers,
                                                 List<ValueResolver<V>> valueResolvers, MuleContext muleContext) {

    if (ConcurrentMap.class.equals(mapType)) {
      return new MapValueResolver<>(ConcurrentHashMap.class, keyResolvers, valueResolvers, muleContext);
    } else if (Map.class.equals(mapType)) {
      return new MapValueResolver<>(HashMap.class, keyResolvers, valueResolvers, muleContext);
    } else {
      return new MapValueResolver<>(mapType, keyResolvers, valueResolvers, muleContext);
    }
  }

  /**
   * Passes the given {@code context} to each resolvers and outputs a map of type {@code mapType} with each result
   *
   * @param context a {@link ValueResolvingContext} the context to evaluate
   * @return a {@link Map} of type {@code mapType}
   * @throws MuleException
   */
  @Override
  public Map<K, V> resolve(ValueResolvingContext context) throws MuleException {
    Map<K, V> map = instantiateMap();

    Iterator<ValueResolver<K>> keyIt = keyResolvers.iterator();
    Iterator<ValueResolver<V>> valueIt = valueResolvers.iterator();
    while (keyIt.hasNext() && valueIt.hasNext()) {
      try {
        map.put(keyIt.next().resolve(context), valueIt.next().resolve(context));

      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return map;
  }

  /**
   * @return {@code true} if at least one of the {@code resolvers} are dynamic
   */
  @Override
  public boolean isDynamic() {
    try {
      return hasAnyDynamic(keyResolvers) || hasAnyDynamic(valueResolvers);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Map<K, V> instantiateMap() {
    try {
      return mapType.newInstance();
    } catch (Exception e) {
      throw new RuntimeException("Could not create instance of " + mapType.getName(), e);
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    for (ValueResolver<V> valueResolver : valueResolvers) {
      initialiseIfNeeded(valueResolver, true, muleContext);
    }
  }

  public List<ValueResolver<K>> getKeyResolvers() {
    return keyResolvers;
  }

  public List<ValueResolver<V>> getValueResolvers() {
    return valueResolvers;
  }
}
