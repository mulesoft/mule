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
import org.mule.runtime.core.internal.resolving.DefaultExtensionResolvingContext;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

import java.util.Optional;

/**
 * Default immutable implementation of {@link MetadataContext}, it provides access to the extension configuration and connection
 * in the metadata fetch invocation.
 *
 * @since 4.0
 */
public class DefaultMetadataContext extends DefaultExtensionResolvingContext implements MetadataContext {

  private final MetadataCache cache;

  /**
   * Retrieves the configuration for the related component
   *
   * @param configInstance optional configuration of a component
   * @param connectionManager {@link ConnectionManager} which is able to find a connection for the component using the
   *        {@param configInstance}
   * @param cache instance of the {@link MetadataCache} for this context
   * @param typeLoader instance of a {@link ClassTypeLoader} in the context of this extension
   */
  public DefaultMetadataContext(Optional<ConfigurationInstance> configInstance,
                                ConnectionManager connectionManager,
                                MetadataCache cache, ClassTypeLoader typeLoader)
      throws ConnectionException {
    super(configInstance, connectionManager, typeLoader);
    this.cache = cache;
  }

  @Override
  public MetadataCache getCache() {
    return cache;
  }

}
