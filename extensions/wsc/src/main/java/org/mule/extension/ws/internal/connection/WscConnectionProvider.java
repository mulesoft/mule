/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.connection;

import org.mule.extension.ws.api.SoapVersion;
import org.mule.extension.ws.api.security.SecurityStrategy;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.List;

/**
 * {@link ConnectionProvider} that returns instances of {@link WscConnection}.
 *
 * @since 4.0
 */
public class WscConnectionProvider implements PoolingConnectionProvider<WscConnection> {

  /**
   * The WSDL file URL remote or local.
   */
  @Parameter
  private String wsdlLocation;

  /**
   * The service name.
   */
  @Parameter
  private String service;

  /**
   * The port name.
   */
  @Parameter
  private String port;

  /**
   * The address of the web service.
   */
  @Parameter
  @Optional
  private String address;

  /**
   * The security strategies configured to protect the SOAP messages.
   */
  @Parameter
  @Optional
  @NullSafe
  private List<SecurityStrategy> securityStrategies;

  /**
   * The soap version of the WSDL.
   */
  @Parameter
  @Optional(defaultValue = "SOAP11")
  private SoapVersion soapVersion;

  /**
   * If should use the MTOM protocol to manage the attachments or not.
   */
  @Parameter
  @Optional(defaultValue = "false")
  private boolean mtomEnabled;

  /**
   * {@inheritDoc}
   */
  @Override
  public WscConnection connect() throws ConnectionException {
    return new WscConnection(wsdlLocation, address, service, port, soapVersion, securityStrategies, mtomEnabled);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void disconnect(WscConnection client) {
    client.disconnect();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectionValidationResult validate(WscConnection client) {
    return client.validateConnection();
  }
}
