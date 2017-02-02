/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.runtime.connectivity.ConnectionProviderFactory;

/**
 * A {@link ModelProperty} meant to be used on {@link ConnectionProviderModel connection provider models},
 * which provides access to a {@link ConnectionProviderFactory} used to create such providers
 *
 * @since 4.0
 */
public final class ConnectionProviderFactoryModelProperty implements ModelProperty {

  private final ConnectionProviderFactory connectionProviderFactory;

  /**
   * Creates a new instance
   * @param connectionProviderFactory a {@link ConnectionProviderFactory}
   */
  public ConnectionProviderFactoryModelProperty(
                                                ConnectionProviderFactory connectionProviderFactory) {
    this.connectionProviderFactory = connectionProviderFactory;
  }

  /**
   * @return a {@link ConnectionProviderFactory}
   */
  public ConnectionProviderFactory getConnectionProviderFactory() {
    return connectionProviderFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "connectionProviderFactory";
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isPublic() {
    return false;
  }
}
