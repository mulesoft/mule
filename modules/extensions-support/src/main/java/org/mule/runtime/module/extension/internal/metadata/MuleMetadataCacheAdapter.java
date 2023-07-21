/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.metadata;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.sdk.api.metadata.MetadataCache;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

/**
 * Adapter of {@link org.mule.runtime.api.metadata.MetadataCache} to {@link MuleMetadataCacheAdapter}
 *
 * @since 4.5.0
 */
public class MuleMetadataCacheAdapter implements org.mule.runtime.api.metadata.MetadataCache {

  private final MetadataCache delegate;

  public MuleMetadataCacheAdapter(MetadataCache delegate) {
    this.delegate = delegate;
  }

  @Override
  public void put(Serializable key, Serializable value) {
    delegate.put(key, value);
  }

  @Override
  public void putAll(Map<? extends Serializable, ? extends Serializable> values) {
    delegate.putAll(values);
  }

  @Override
  public <T extends Serializable> Optional<T> get(Serializable key) {
    return delegate.get(key);
  }

  @Override
  public <T extends Serializable> T computeIfAbsent(Serializable key, MetadataCacheValueResolver mappingFunction)
      throws MetadataResolvingException, ConnectionException {
    return delegate.computeIfAbsent(key, new SdkMetadataCacheValueResolverAdapter(mappingFunction));
  }
}
