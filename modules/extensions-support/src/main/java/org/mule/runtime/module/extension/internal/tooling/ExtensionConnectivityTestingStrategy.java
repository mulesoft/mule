/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.tooling;

import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import org.mule.runtime.api.connection.ConnectionExceptionCode;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderResolver;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingStrategy;

import javax.inject.Inject;

/**
 * Implementation of {@code ConnectivityTestingStrategy} that can do connectivity testing over components creates with extensions
 * API.
 *
 * @since 4.0
 */
public class ExtensionConnectivityTestingStrategy implements ConnectivityTestingStrategy {

  @Inject
  private MuleContext muleContext;

  /**
   * Used for testing purposes
   *
   * @param muleContext the {@code MuleContext}.
   */
  ExtensionConnectivityTestingStrategy(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  /**
   * Constructor used for creation using SPI.
   */
  public ExtensionConnectivityTestingStrategy() {}

  void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectionValidationResult testConnectivity(Object connectivityTestingObject) {
    try {
      ConnectionProvider connectionProvider =
          ((ConnectionProviderResolver) connectivityTestingObject).resolve(getInitialiserEvent(muleContext));
      Object connection = connectionProvider.connect();
      return connectionProvider.validate(connection);
    } catch (Exception e) {
      return failure(e.getMessage(), ConnectionExceptionCode.UNKNOWN, e);
    }
  }

  /**
   * @return true whenever there's a {@code ConfigurationProvider} in the configuration, false otherwise.
   */
  @Override
  public boolean accepts(Object connectivityTestingObject) {
    return connectivityTestingObject instanceof ConnectionProviderResolver;
  }

}
