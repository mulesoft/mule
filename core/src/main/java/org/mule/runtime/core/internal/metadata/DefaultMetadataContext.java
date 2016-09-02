/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.metadata;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataCache;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;

import java.util.Optional;

/**
 * Default immutable implementation of {@link MetadataContext}, it provides access to the extension configuration and connection
 * in the metadata fetch invocation.
 *
 * @since 4.0
 */
public class DefaultMetadataContext implements MetadataContext {

  private final Optional<ConfigurationInstance> configInstance;
  private final ConnectionManager connectionManager;
  private final MetadataCache cache;
  private final ClassTypeLoader typeLoader;

  /**
   * Retrieves the configuration for the related component
   *
   * @param configInstance optional configuration of a component
   * @param connectionManager {@link ConnectionManager} which is able to find a connection for the component using the
   *        {@param configInstance}
   * @param cache instance of the {@link MetadataCache} for this context
   * @param typeLoader instance of a {@link TypeLoader} in the context of this extension
   */
  public DefaultMetadataContext(Optional<ConfigurationInstance> configInstance,
                                ConnectionManager connectionManager,
                                MetadataCache cache, ClassTypeLoader typeLoader) {
    this.configInstance = configInstance;
    this.connectionManager = connectionManager;
    this.cache = cache;
    this.typeLoader = typeLoader;
  }

  /**
   * @param <C> Configuration type
   * @return optional configuration of a component
   */
  @Override
  public <C> Optional<C> getConfig() {
    return (Optional<C>) configInstance.map(Optional::of);
  }

  /**
   * Retrieves the connection for the related component and configuration
   *
   * @param <C> Connection type
   * @return A connection instance of {@param <C>} type for the component. If the related configuration does not require a
   *         connection {@link Optional#empty()} will be returned
   * @throws ConnectionException when no valid connection is found for the related component and configuration
   */
  @Override
  public <C> Optional<C> getConnection() throws ConnectionException {
    ConfigurationInstance config = configInstance.orElse(null);
    if (config == null || !config.getConnectionProvider().isPresent()) {
      return Optional.empty();
    }

    return Optional.of((C) connectionManager.getConnection(config.getValue()).getConnection());
  }

  @Override
  public MetadataCache getCache() {
    return cache;
  }

  @Override
  public ClassTypeLoader getTypeLoader() {
    return typeLoader;
  }

}
