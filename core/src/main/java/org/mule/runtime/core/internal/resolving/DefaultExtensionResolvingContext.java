/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.resolving;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.resolving.ExtensionResolvingContext;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

import java.util.Optional;

/**
 * Default immutable implementation of {@link ExtensionResolvingContext}, it provides access to the extension
 * configuration and connection.
 *
 * @since 4.0
 */
public class DefaultExtensionResolvingContext implements ExtensionResolvingContext {

  private final Optional<ConfigurationInstance> configInstance;
  private final ClassTypeLoader typeLoader;
  private final ConnectionHandler connectionHandler;

  /**
   * Retrieves the configuration for the related component
   *
   * @param configInstance    optional configuration of a component
   * @param connectionManager {@link ConnectionManager} which is able to find a connection for the component using the
   *                          {@param configInstance}
   * @param typeLoader        instance of a {@link ClassTypeLoader} in the context of this extension
   */
  public DefaultExtensionResolvingContext(Optional<ConfigurationInstance> configInstance,
                                          ConnectionManager connectionManager, ClassTypeLoader typeLoader)
      throws ConnectionException {
    this.configInstance = configInstance;
    this.typeLoader = typeLoader;

    if (configInstance.isPresent() && configInstance.get().getConnectionProvider().isPresent()) {
      this.connectionHandler = connectionManager.getConnection(configInstance.get().getValue());
    } else {
      this.connectionHandler = null;
    }
  }

  /**
   * @param <C> Configuration type
   * @return optional configuration of a component
   */
  @Override
  public <C> Optional<C> getConfig() {
    return (Optional<C>) configInstance.map(ConfigurationInstance::getValue);
  }

  /**
   * Retrieves the connection for the related component and configuration
   *
   * @param <C> Connection type
   * @return A connection instance of {@param <C>} type for the component. If the related configuration does not require a
   * connection {@link Optional#empty()} will be returned
   * @throws ConnectionException when no valid connection is found for the related component and configuration
   */
  @Override
  public <C> Optional<C> getConnection() throws ConnectionException {
    return connectionHandler != null ? of((C) connectionHandler.getConnection()) : empty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ClassTypeLoader getTypeLoader() {
    return typeLoader;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BaseTypeBuilder getTypeBuilder() {
    return BaseTypeBuilder.create(JAVA);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose() {
    if (connectionHandler != null) {
      connectionHandler.release();
    }
  }
}
