/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.connection;

import static java.lang.Thread.currentThread;
import org.mule.extension.ws.api.message.CustomTransportConfiguration;
import org.mule.extension.ws.internal.security.SecurityStrategyAdapter;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.service.http.api.HttpService;
import org.mule.services.soap.api.SoapService;
import org.mule.services.soap.api.SoapVersion;
import org.mule.services.soap.api.client.SoapClient;
import org.mule.services.soap.api.client.SoapClientConfiguration;
import org.mule.services.soap.api.client.SoapClientConfigurationBuilder;
import org.mule.services.soap.api.message.dispatcher.DefaultHttpMessageDispatcher;

import java.net.URL;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;

/**
 * {@link ConnectionProvider} that returns instances of {@link SoapClient}.
 *
 * @since 4.0
 */
public class SoapClientConnectionProvider implements PoolingConnectionProvider<SoapClient> {

  private final Logger LOGGER = Logger.getLogger(SoapClientConnectionProvider.class);

  @Inject
  private SoapService soapService;

  @Inject
  private HttpService httpService;

  @Inject
  private ExtensionsClient extensionsClient;

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
  private List<SecurityStrategyAdapter> securityStrategies;

  @Parameter
  @Optional
  private CustomTransportConfiguration customTransportConfiguration;

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
  public SoapClient connect() throws ConnectionException {
    return soapService.getClientFactory().create(getConfiguration());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void disconnect(SoapClient client) {
    try {
      client.stop();
    } catch (Exception e) {
      LOGGER.error("Error disconnecting soap client [" + client.toString() + "]: " + e.getMessage(), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectionValidationResult validate(SoapClient client) {
    // TODO MULE-12036
    return ConnectionValidationResult.success();
  }

  private SoapClientConfiguration getConfiguration() {
    SoapClientConfigurationBuilder configuration = SoapClientConfiguration.builder()
        .withService(service)
        .withPort(port)
        .withWsdlLocation(getWsdlLocation(wsdlLocation))
        .withAddress(address)
        .withVersion(soapVersion);

    securityStrategies.stream().map(SecurityStrategyAdapter::getSecurityStrategy).forEach(configuration::withSecurity);

    if (mtomEnabled) {
      configuration.enableMtom();
    }

    if (customTransportConfiguration != null) {
      configuration.withDispatcher(customTransportConfiguration.buildDispatcher(extensionsClient));
    } else {
      configuration.withDispatcher(new DefaultHttpMessageDispatcher(httpService));
    }

    return configuration.build();
  }

  private String getWsdlLocation(String wsdlLocation) {
    URL resource = currentThread().getContextClassLoader().getResource(wsdlLocation);
    return resource != null ? resource.getPath() : wsdlLocation;
  }
}
