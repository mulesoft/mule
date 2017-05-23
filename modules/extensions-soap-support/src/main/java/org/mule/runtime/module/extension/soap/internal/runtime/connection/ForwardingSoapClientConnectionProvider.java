/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.runtime.connection;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.extension.api.soap.MessageDispatcherProvider;
import org.mule.runtime.extension.api.soap.WebServiceDefinition;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.soap.api.SoapService;

import java.util.List;

import javax.inject.Inject;

/**
 * {@link ConnectionProvider} implementation that handles {@link ForwardingSoapClient} connections,
 * which are created from a {@link List} of {@link WebServiceDefinition}s. Each {@link WebServiceDefinition} describe
 * one service with which instances created by this provider will be capable to connect to.
 * <p>
 * This Provider centralize the logic and polling mechanism to provision and release
 * connection for each of the clients that required to be created, while remaining abstracted
 * from the concerns of actually manage those connections.
 *
 * @since 4.0
 */
public class ForwardingSoapClientConnectionProvider implements PoolingConnectionProvider<ForwardingSoapClient> {

  @Inject
  private SoapService soapService;

  @Inject
  private HttpService httpService;

  /**
   * The {@link SoapServiceProvider} that knows which services will this connection connect to.
   */
  private final SoapServiceProvider serviceProvider;

  /**
   * The {@link MessageDispatcherProvider} used to get {@link MessageDispatcher} instances.
   */
  private final MessageDispatcherProvider<MessageDispatcher> transportProvider;

  ForwardingSoapClientConnectionProvider(SoapServiceProvider serviceProvider,
                                         MessageDispatcherProvider<? extends MessageDispatcher> transportProvider) {
    this.serviceProvider = serviceProvider;
    this.transportProvider = (MessageDispatcherProvider<MessageDispatcher>) transportProvider;
  }

  /**
   * @return a new {@link ForwardingSoapClient} instance.
   * @throws ConnectionException in any error case.
   */
  @Override
  public ForwardingSoapClient connect() throws ConnectionException {
    return new ForwardingSoapClient(soapService, serviceProvider, transportProvider);
  }

  /**
   * Disconnects a {@link ForwardingSoapClient} connection, by shutting down each one of the services
   * that the provided instance manages.
   *
   * @param connection a {@link ForwardingSoapClient} instance to disconnect,
   */
  @Override
  public void disconnect(ForwardingSoapClient connection) {
    connection.disconnect();
  }

  // TODO: MULE-12207 connectivity testing for Soap Connect extensions.
  @Override
  public ConnectionValidationResult validate(ForwardingSoapClient connection) {
    return success();
  }
}
