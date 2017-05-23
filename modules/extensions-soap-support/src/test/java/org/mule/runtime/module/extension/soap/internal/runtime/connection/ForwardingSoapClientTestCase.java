/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.runtime.connection;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.extension.api.soap.WebServiceDefinition;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;
import org.mule.runtime.soap.api.SoapService;
import org.mule.runtime.soap.api.client.SoapClient;
import org.mule.runtime.soap.api.client.SoapClientFactory;
import org.mule.runtime.soap.api.client.metadata.SoapMetadataResolver;
import org.mule.runtime.soap.api.message.SoapRequest;
import org.mule.runtime.soap.api.message.SoapResponse;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ForwardingSoapClientTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private SoapService service;

  @Before
  public void setup() throws ConnectionException {
    SoapClientFactory factory = mock(SoapClientFactory.class);
    service = mock(SoapService.class);
    when(service.getClientFactory()).thenReturn(factory);
    when(factory.create(anyObject())).thenReturn(new TestSoapClient());

  }

  @Test
  public void loadClient() throws Exception {
    ForwardingSoapClient client = new ForwardingSoapClient(service, new TestServiceProvider(), mock(MessageDispatcher.class));
    SoapClient sc = client.getSoapClient("uno");
    SoapResponse response = sc.consume(SoapRequest.empty("no-op"));
    assertThat(IOUtils.toString(response.getContent()), is("Content"));
  }

  @Test
  public void invalidService() throws MuleException, ConnectionException {
    expectedException.expectMessage("Could not find a soap client id [invalid]");
    expectedException.expect(IllegalArgumentException.class);
    ForwardingSoapClient client = new ForwardingSoapClient(service, new TestServiceProvider(), mock(MessageDispatcher.class));
    client.getSoapClient("invalid");
  }

  @Test
  public void disconnect() throws Exception {
    ForwardingSoapClient client = new ForwardingSoapClient(service, new TestServiceProvider(), mock(MessageDispatcher.class));
    TestSoapClient sc1 = (TestSoapClient) client.getSoapClient("uno");
    TestSoapClient sc2 = (TestSoapClient) client.getSoapClient("dos");
    client.disconnect();
    assertThat(sc1.disconnected, is(true));
    assertThat(sc2.disconnected, is(true));
  }

  private class TestServiceProvider implements SoapServiceProvider {

    @Override
    public List<WebServiceDefinition> getWebServiceDefinitions() {
      try {
        return asList(
                      WebServiceDefinition.builder().withId("uno").withFriendlyName("Service Name")
                          .withWsdlUrl(new URL("http://localhost.com/uno")).withService("Service").withPort("Port1").build(),
                      WebServiceDefinition.builder().withId("dos").withFriendlyName("Another Service Name")
                          .withWsdlUrl(new URL("http://localhost.com/dos")).withService("Service").withPort("Port2").build());
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private class TestSoapClient implements SoapClient {

    private boolean disconnected = false;

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
  }

}

