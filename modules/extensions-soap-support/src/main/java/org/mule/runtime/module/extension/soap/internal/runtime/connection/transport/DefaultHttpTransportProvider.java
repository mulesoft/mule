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
import org.mule.runtime.extension.api.soap.SoapTransportProvider;
import org.mule.service.http.api.HttpService;
import org.mule.services.soap.api.message.dispatcher.DefaultHttpMessageDispatcher;

import javax.inject.Inject;

/**
 * {@link SoapTransportProvider} implementation that provides instances of {@link DefaultHttpMessageDispatcher}.
 * <p>
 * This is the default {@link SoapTransportProvider} that is used if no {@link SoapTransportProvider} was provided for the
 * extension.
 * <p>
 * If the extension declares at least one {@link SoapTransportProvider} this one can be included by annotating the extension
 * class with {@link org.mule.runtime.extension.api.soap.annotation.HttpConfigTransportProvider}.
 *
 * @since 4.0
 */
public class DefaultHttpTransportProvider implements SoapTransportProvider<DefaultHttpMessageDispatcher> {

  @Inject
  private HttpService service;

  @Override
  public DefaultHttpMessageDispatcher connect() throws ConnectionException {
    return new DefaultHttpMessageDispatcher(service);
  }

  @Override
  public void disconnect(DefaultHttpMessageDispatcher connection) {
    connection.dispose();
  }

  @Override
  public ConnectionValidationResult validate(DefaultHttpMessageDispatcher connection) {
    return success();
  }
}
