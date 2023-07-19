/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.metadata;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.metadata.MetadataContext;

import java.util.Optional;

/**
 * A {@link MetadataContext} which is aware of the {@link ConnectionProvider} used to provide its connection.
 *
 * @since 4.4.0
 */
public interface ConnectionProviderAwareMetadataContext extends MetadataContext {

  /**
   * Retrieves the connection provider for the related a component and configuration
   *
   * @return Optional connection instance of {@link ConnectionProvider} type for the component.
   */
  Optional<ConnectionProvider> getConnectionProvider();

}
