/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.soap.api.client;

import org.mule.runtime.extension.api.soap.message.MessageDispatcher;
import org.mule.runtime.extension.api.soap.security.SecurityStrategy;
import org.mule.runtime.soap.api.SoapVersion;
import org.mule.runtime.soap.api.transport.TransportResourceLocator;

import java.util.List;

/**
 * Represents a Soap Client Configuration with all the required attributes for stablishing a connection with the Web Service.
 *
 * @since 4.0
 */
public final class SoapClientConfiguration {

  private final String wsdlLocation;
  private final String address;
  private final String service;
  private final String port;
  private final SoapVersion version;
  private final boolean mtomEnabled;
  private final List<SecurityStrategy> securities;
  private final MessageDispatcher dispatcher;
  private final TransportResourceLocator locator;
  private final String encoding;

  SoapClientConfiguration(String wsdlLocation,
                          String address,
                          String service,
                          String port,
                          SoapVersion version,
                          boolean mtomEnabled,
                          List<SecurityStrategy> securities,
                          MessageDispatcher dispatcher,
                          TransportResourceLocator locator,
                          String encoding) {
    this.wsdlLocation = wsdlLocation;
    this.address = address;
    this.service = service;
    this.port = port;
    this.version = version;
    this.mtomEnabled = mtomEnabled;
    this.securities = securities;
    this.dispatcher = dispatcher;
    this.locator = locator;
    this.encoding = encoding;
  }

  public static SoapClientConfigurationBuilder builder() {
    return new SoapClientConfigurationBuilder();
  }

  public String getWsdlLocation() {
    return wsdlLocation;
  }

  public String getAddress() {
    return address;
  }

  public String getService() {
    return service;
  }

  public String getPort() {
    return port;
  }

  public SoapVersion getVersion() {
    return version;
  }

  public boolean isMtomEnabled() {
    return mtomEnabled;
  }

  public List<SecurityStrategy> getSecurities() {
    return securities;
  }

  public MessageDispatcher getDispatcher() {
    return dispatcher;
  }

  public String getEncoding() {
    return encoding;
  }

  public TransportResourceLocator getLocator() {
    return locator;
  }
}
