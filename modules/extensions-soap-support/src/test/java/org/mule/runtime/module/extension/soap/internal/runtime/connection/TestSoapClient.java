/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.runtime.connection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;
import org.mule.runtime.soap.api.client.SoapClient;
import org.mule.runtime.soap.api.client.SoapClientConfiguration;
import org.mule.runtime.soap.api.client.metadata.SoapMetadataResolver;
import org.mule.runtime.soap.api.message.SoapRequest;
import org.mule.runtime.soap.api.message.SoapResponse;

import java.io.ByteArrayInputStream;

public class TestSoapClient implements SoapClient {

  private final MessageDispatcher dispatcher;
  private boolean disconnected = false;

  public TestSoapClient(SoapClientConfiguration configuration) {
    this.dispatcher = configuration.getDispatcher();
  }

  @Override
  public void stop() throws MuleException {
    disconnected = true;
  }

  @Override
  public void start() throws MuleException {

  }

  @Override
  public SoapResponse consume(SoapRequest request) {
    SoapResponse response = mock(SoapResponse.class);
    when(response.getContent()).thenReturn(new ByteArrayInputStream("Content".getBytes()));
    return response;
  }

  @Override
  public SoapMetadataResolver getMetadataResolver() {
    return null;
  }

  public MessageDispatcher getDispatcher() {
    return dispatcher;
  }

  public boolean isDisconnected() {
    return disconnected;
  }

  public void setDisconnected(boolean disconnected) {
    this.disconnected = disconnected;
  }
}
