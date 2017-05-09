/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.runtime.connection.transport;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.soap.SoapTransportProvider;
import org.mule.services.soap.api.message.dispatcher.HttpConfigBasedMessageDispatcher;

import javax.inject.Inject;

/**
 * {@link SoapTransportProvider} implementation that provides instances of {@link HttpConfigBasedMessageDispatcher}.
 *
 * This {@link SoapTransportProvider} is added to the extensions declared {@link SoapTransportProvider}s by using the
 * {@link org.mule.runtime.extension.api.soap.annotation.HttpConfigTransportProvider} annotation at extension level.
 *
 * @since 4.0
 */
public class HttpConfigTransportProvider implements SoapTransportProvider<HttpConfigBasedMessageDispatcher> {

  @Inject
  private ExtensionsClient client;

  @Parameter
  private String configName;

  @Override
  public HttpConfigBasedMessageDispatcher connect() throws ConnectionException {
    return new HttpConfigBasedMessageDispatcher(configName, client);
  }

  @Override
  public void disconnect(HttpConfigBasedMessageDispatcher connection) {
    connection.dispose();
  }

  @Override
  public ConnectionValidationResult validate(HttpConfigBasedMessageDispatcher connection) {
    return success();
  }

}
