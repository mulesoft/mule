/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.metadata.MetadataContext;

import java.util.Optional;

public interface ConnectionProviderAwareMetadataContext extends MetadataContext {

  /**
   * Retrieves the connection provider for the related a component and configuration
   *
   * @return Optional connection instance of {@link ConnectionProvider} type for the component.
   */
  Optional<ConnectionProvider> getConnectionProvider();

}
