/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.tooling;

import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.core.config.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import org.mule.runtime.api.connection.ConnectionExceptionCode;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderResolver;
import org.mule.runtime.core.api.connectivity.ConnectivityTestingStrategy;

import javax.inject.Inject;

/**
 * Implementation of {@code ConnectivityTestingStrategy} that can do connectivity testing over components creates with extensions
 * API.
 *
 * @since 4.0
 */
public class ExtensionConnectivityTestingStrategy implements ConnectivityTestingStrategy, MuleContextAware {

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

  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectionValidationResult testConnectivity(Object connectivityTestingObject) {
    try {
      if (connectivityTestingObject instanceof ConnectionProviderResolver) {
        ConnectionProvider connectionProvider =
            ((ConnectionProviderResolver) connectivityTestingObject).resolve(getInitialiserEvent(muleContext));
        return validateConnectionOverConnectionProvider(connectionProvider);
      } else {
        ConfigurationProvider configurationProvider = (ConfigurationProvider) connectivityTestingObject;
        ConfigurationInstance configurationInstance = configurationProvider.get(getInitialiserEvent(muleContext));
        configurationInstance.getConnectionProvider();
        if (configurationInstance.getConnectionProvider().isPresent()) {
          return validateConnectionOverConnectionProvider(configurationInstance.getConnectionProvider().get());
        } else {
          throw new MuleRuntimeException(createStaticMessage("The component does not support connectivity testing"));
        }
      }
    } catch (Exception e) {
      return failure(e.getMessage(), ConnectionExceptionCode.UNKNOWN, e);
    }
  }

  private ConnectionValidationResult validateConnectionOverConnectionProvider(ConnectionProvider connectionProvider)
      throws org.mule.runtime.api.connection.ConnectionException {
    Object connection = connectionProvider.connect();
    return connectionProvider.validate(connection);
  }

  /**
   * @return true whenever there's a {@code ConfigurationProvider} in the configuration, false otherwise.
   */
  @Override
  public boolean accepts(Object connectivityTestingObject) {
    return connectivityTestingObject instanceof ConnectionProviderResolver
        || connectivityTestingObject instanceof ConfigurationProvider;
  }

}
