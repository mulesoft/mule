/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.runtime.connection;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.soap.MessageDispatcherProvider;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.extension.api.soap.WebServiceDefinition;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;
import org.mule.runtime.soap.api.SoapService;
import org.mule.runtime.soap.api.client.SoapClient;
import org.mule.runtime.soap.api.client.SoapClientConfiguration;
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
  private ForwardingSoapClient client;
  private MessageDispatcherProvider<MessageDispatcher> dispatcherProvider = new TestDispatcherProvider();

  @Before
  public void setup() throws ConnectionException {
    service = mock(SoapService.class);
    when(service.getClientFactory()).thenReturn(new TestClientFactory());
    client = new ForwardingSoapClient(service, new TestServiceProvider(), dispatcherProvider);
  }

  @Test
  public void loadClient() throws Exception {
    SoapClient sc = client.getSoapClient("uno");
    SoapResponse response = sc.consume(SoapRequest.empty("no-op"));
    assertThat(IOUtils.toString(response.getContent()), is("Content"));
  }

  @Test
  public void invalidService() throws MuleException {
    expectedException.expectMessage("Could not find a soap client id [invalid]");
    expectedException.expect(IllegalArgumentException.class);
    client.getSoapClient("invalid");
  }

  @Test
  public void disconnect() throws Exception {
    TestSoapClient sc1 = (TestSoapClient) client.getSoapClient("uno");
    TestSoapClient sc2 = (TestSoapClient) client.getSoapClient("dos");
    client.disconnect();
    assertThat(sc1.isDisconnected(), is(true));
    assertThat(sc2.isDisconnected(), is(true));
  }

  @Test
  public void differentDispatcherInstances() {
    TestSoapClient sc1 = (TestSoapClient) client.getSoapClient("uno");
    TestSoapClient sc2 = (TestSoapClient) client.getSoapClient("dos");
    assertThat(sc1.getDispatcher(), is(not(sameInstance(sc2.getDispatcher()))));
  }

  @Test
  public void connectAndDisconnectDispatcher() throws ConnectionException {
    MessageDispatcherProvider<MessageDispatcher> spyProvider = spy(dispatcherProvider);
    client = new ForwardingSoapClient(service, new TestServiceProvider(), spyProvider);
    TestSoapClient sc1 = (TestSoapClient) client.getSoapClient("uno");
    TestSoapClient sc2 = (TestSoapClient) client.getSoapClient("dos");
    verify(spyProvider, times(2)).connect();
    assertThat(((TestDispatcherProvider.TestMessageDispatcher) sc1.getDispatcher()).isDisconnected(), is(true));
    assertThat(((TestDispatcherProvider.TestMessageDispatcher) sc2.getDispatcher()).isDisconnected(), is(true));
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

  public class TestClientFactory implements SoapClientFactory {

    @Override
    public SoapClient create(SoapClientConfiguration configuration) throws ConnectionException {
      return new TestSoapClient(configuration);
    }
  }
}

