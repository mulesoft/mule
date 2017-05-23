/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.runtime.connection.transport;

import static java.util.Objects.isNull;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.soap.HttpMessageDispatcherProvider;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.soap.api.message.dispatcher.DefaultHttpMessageDispatcher;
import org.mule.runtime.soap.api.message.dispatcher.HttpConfigBasedMessageDispatcher;

import javax.inject.Inject;

/**
 * Default implementation of {@link HttpMessageDispatcherProvider} sends a soap message over http using a default configuration or
 * using an http requester configuration if configured.
 *
 * @since 4.0
 */
public class DefaultHttpMessageDispatcherProvider implements HttpMessageDispatcherProvider {

  @Inject
  private HttpService httpService;

  @Inject
  private ExtensionsClient client;

  @Parameter
  @Optional
  private String configRef;

  @Override
  public MessageDispatcher connect() throws ConnectionException {
    return isNull(configRef) ? new DefaultHttpMessageDispatcher(httpService)
        : new HttpConfigBasedMessageDispatcher(configRef, client);
  }

  @Override
  public void disconnect(MessageDispatcher connection) {
    connection.dispose();
  }

  @Override
  public ConnectionValidationResult validate(MessageDispatcher connection) {
    return success();
  }

}
