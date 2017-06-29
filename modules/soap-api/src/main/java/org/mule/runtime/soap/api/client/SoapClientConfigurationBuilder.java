/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.soap.api.client;

import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import static org.mule.runtime.soap.api.SoapVersion.SOAP11;

import org.mule.runtime.extension.api.soap.message.MessageDispatcher;
import org.mule.runtime.extension.api.soap.security.DecryptSecurityStrategy;
import org.mule.runtime.extension.api.soap.security.EncryptSecurityStrategy;
import org.mule.runtime.extension.api.soap.security.SecurityStrategy;
import org.mule.runtime.extension.api.soap.security.SignSecurityStrategy;
import org.mule.runtime.extension.api.soap.security.TimestampSecurityStrategy;
import org.mule.runtime.extension.api.soap.security.UsernameTokenSecurityStrategy;
import org.mule.runtime.extension.api.soap.security.VerifySignatureSecurityStrategy;
import org.mule.runtime.soap.api.SoapVersion;
import org.mule.runtime.soap.api.message.SoapMessage;
import org.mule.runtime.soap.api.transport.NullTransportResourceLocator;
import org.mule.runtime.soap.api.transport.TransportResourceLocator;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder pattern implementation that creates {@link SoapClientConfiguration} instances.
 *
 * @since 4.0
 */
public class SoapClientConfigurationBuilder {

  private String wsdlLocation;
  private String address;
  private String service;
  private String port;
  private SoapVersion version = SOAP11;
  private boolean mtomEnabled;
  private List<SecurityStrategy> securities = new ArrayList<>();
  private MessageDispatcher dispatcher;
  private String encoding;
  private TransportResourceLocator locator = new NullTransportResourceLocator();

  SoapClientConfigurationBuilder() {}

  /**
   * Sets the location of the WSDL that describes the SOAP web service. This can be an URL to an
   * external resource (e.g http://somewsdl.com/hit?wsdl), just a reference to a local file or an application resource name.
   *
   * @return this builder.
   */
  public SoapClientConfigurationBuilder withWsdlLocation(String wsdlLocation) {
    this.wsdlLocation = wsdlLocation;
    return this;
  }

  /**
   * Sets the address of the Web Service, if none specified it will be fetched from the wsdl file, if possible.
   *
   * @return this builder.
   */
  public SoapClientConfigurationBuilder withAddress(String address) {
    this.address = address;
    return this;
  }

  /**
   * Sets the service of the WSDL we want to perform operations from.
   *
   * @return this builder.
   */
  public SoapClientConfigurationBuilder withService(String service) {
    this.service = service;
    return this;
  }

  /**
   * Sets the port of the service that describes the set of operations that can be performed.
   *
   * @return this builder.
   */
  public SoapClientConfigurationBuilder withPort(String port) {
    this.port = port;
    return this;
  }

  /**
   * Sets the encoding of the messages send and retrieved by the .
   *
   * @return this builder.
   */
  public SoapClientConfigurationBuilder withEncoding(String encoding) {
    this.encoding = encoding;
    return this;
  }

  /**
   * Sets the {@link SoapVersion} of the Web Service. defaults to SOAP 1.1
   *
   * @return this builder.
   */
  public SoapClientConfigurationBuilder withVersion(SoapVersion version) {
    this.version = version;
    return this;
  }

  /**
   * Specifies that the Web Service is MTOM enabled.
   *
   * @return this builder.
   */
  public SoapClientConfigurationBuilder enableMtom(boolean mtomEnabled) {
    this.mtomEnabled = mtomEnabled;
    return this;
  }

  /**
   * Sets a new {@link SecurityStrategy} to connect with a Secured Soap Web Service.
   * <p>
   * One of: {@link DecryptSecurityStrategy}, {@link EncryptSecurityStrategy}, {@link SignSecurityStrategy},
   * {@link TimestampSecurityStrategy}, {@link UsernameTokenSecurityStrategy} or {@link VerifySignatureSecurityStrategy}.
   * <p>
   * Multiple {@link SecurityStrategy Security Strategies} can be configured.
   *
   * @return this builder.
   */
  public SoapClientConfigurationBuilder withSecurity(SecurityStrategy security) {
    this.securities.add(security);
    return this;
  }

  /**
   * Sets a list of {@link SecurityStrategy SecurityStrategies} to connect with a Secured Soap Web Service.
   *
   * @return this builder.
   */
  public SoapClientConfigurationBuilder withSecurities(List<SecurityStrategy> security) {
    this.securities.addAll(security);
    return this;
  }

  /**
   * Sets a custom {@link MessageDispatcher} that enables the send and retrieve of {@link SoapMessage}s using a custom underlying
   * transport.
   *
   * @return this builder
   */
  public SoapClientConfigurationBuilder withDispatcher(MessageDispatcher dispatcher) {
    this.dispatcher = dispatcher;
    return this;
  }

  /**
   * Sets a {@link TransportResourceLocator} instance to fetch the wsdl resources.
   *
   * @return this builder
   */
  public SoapClientConfigurationBuilder withResourceLocator(TransportResourceLocator locator) {
    this.locator = locator;
    return this;
  }

  /**
   * @return a new {@link SoapClientConfiguration} instance with the attributes specified.
   */
  public SoapClientConfiguration build() {
    checkNotNull(wsdlLocation, "WSDL location cannot be null");
    checkNotNull(service, "Service cannot be null");
    checkNotNull(port, "Port cannot be null");
    checkNotNull(dispatcher, "Message Dispatcher cannot be null");
    return new SoapClientConfiguration(wsdlLocation, address, service, port, version, mtomEnabled, securities,
                                       dispatcher, locator, encoding);
  }
}
