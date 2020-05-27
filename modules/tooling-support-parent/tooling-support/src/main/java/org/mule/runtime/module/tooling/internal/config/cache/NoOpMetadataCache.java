/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.config.cache;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataCache;
import org.mule.runtime.api.metadata.MetadataResolvingException;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

public class NoOpMetadataCache implements MetadataCache {

  private static final MetadataCache INSTANCE = new NoOpMetadataCache();

  public static final MetadataCache getNoOpCache() {
    return INSTANCE;
  }

  private NoOpMetadataCache() {}

  @Override
  public void put(Serializable key, Serializable value) {}

  @Override
  public void putAll(Map<? extends Serializable, ? extends Serializable> values) {}

  @Override
  public <T extends Serializable> Optional<T> get(Serializable key) {
    return Optional.empty();
  }

  @Override
  public <T extends Serializable> T computeIfAbsent(Serializable key, MetadataCacheValueResolver mappingFunction)
      throws MetadataResolvingException, ConnectionException {
    return (T) mappingFunction.compute(key);
  }
}
