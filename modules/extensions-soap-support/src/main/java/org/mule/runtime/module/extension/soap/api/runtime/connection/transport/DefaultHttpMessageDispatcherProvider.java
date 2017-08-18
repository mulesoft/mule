/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.api.runtime.connection.transport;

import static java.util.Objects.isNull;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.soap.HttpMessageDispatcherProvider;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.client.HttpClientConfiguration;
import org.mule.runtime.soap.api.message.dispatcher.DefaultHttpMessageDispatcher;
import org.mule.runtime.soap.api.message.dispatcher.HttpConfigBasedMessageDispatcher;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link HttpMessageDispatcherProvider} sends a soap message over http using a default configuration or
 * using an http requester configuration if configured.
 *
 * @since 4.0
 */
public class DefaultHttpMessageDispatcherProvider implements HttpMessageDispatcherProvider, Lifecycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultHttpMessageDispatcher.class);

  @Inject
  private HttpService httpService;

  @Inject
  private ExtensionsClient extensionsClient;

  @Parameter
  @Optional
  private String requesterConfig;

  private HttpClient httpClient;

  @Override
  public MessageDispatcher connect() throws ConnectionException {
    return isNull(requesterConfig) ? new DefaultHttpMessageDispatcher(httpClient)
        : new HttpConfigBasedMessageDispatcher(requesterConfig, extensionsClient);
  }

  @Override
  public void disconnect(MessageDispatcher connection) {
    disposeIfNeeded(connection, LOGGER);
  }

  @Override
  public ConnectionValidationResult validate(MessageDispatcher connection) {
    return success();
  }


  @Override
  public void dispose() {
    // Do nothing
  }

  @Override
  public void initialise() throws InitialisationException {
    httpClient = httpService.getClientFactory().create(new HttpClientConfiguration.Builder()
        .setName("soap-ext-dispatcher")
        .build());
  }

  @Override
  public void stop() throws MuleException {
    httpClient.stop();
  }

  @Override
  public void start() throws MuleException {
    httpClient.start();
  }
}
