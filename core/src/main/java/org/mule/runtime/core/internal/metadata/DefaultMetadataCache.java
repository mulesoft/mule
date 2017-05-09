/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.metadata;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataCache;
import org.mule.runtime.api.metadata.MetadataResolvingException;

import com.google.common.collect.ImmutableMap;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default immutable implementation of {@link MetadataCache}
 *
 * @since 4.0
 */
public final class DefaultMetadataCache implements MetadataCache {

  private final Map<Serializable, Serializable> cache = new ConcurrentHashMap<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public void put(Serializable key, Serializable value) {
    cache.put(key, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void putAll(Map<? extends Serializable, ? extends Serializable> values) {
    cache.putAll(values);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Serializable> Optional<T> get(Serializable key) {
    return Optional.ofNullable((T) cache.get(key));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Serializable> T computeIfAbsent(Serializable key, MetadataCacheValueResolver mappingFunction)
      throws MetadataResolvingException, ConnectionException {

    Serializable value = cache.get(key);
    if (value == null) {
      value = mappingFunction.compute(key);
      if (value != null) {
        cache.putIfAbsent(key, value);
      }
    }

    return (T) value;
  }

  public Map<Serializable, Serializable> asMap() {
    return ImmutableMap.copyOf(cache);
  }

}
