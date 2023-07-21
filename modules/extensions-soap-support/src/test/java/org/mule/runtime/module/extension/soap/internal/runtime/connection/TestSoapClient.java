/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

  public SoapResponse consume(SoapRequest request) {
    return consume(request, null);
  }

  @Override
  public SoapResponse consume(SoapRequest request, MessageDispatcher dispatcher) {
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
