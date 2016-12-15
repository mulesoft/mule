/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.connection;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.apache.cxf.message.Message.MTOM_ENABLED;
import static org.apache.ws.security.handler.WSHandlerConstants.ACTION;
import static org.apache.ws.security.handler.WSHandlerConstants.PW_CALLBACK_REF;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.extension.ws.api.SoapVersion.SOAP11;
import static org.mule.extension.ws.api.SoapVersion.SOAP12;
import static org.mule.extension.ws.internal.security.SecurityStrategyType.INCOMING;
import static org.mule.extension.ws.internal.security.SecurityStrategyType.OUTGOING;
import org.mule.extension.ws.WscUnitTestCase;
import org.mule.extension.ws.api.security.WssSignSecurityStrategy;
import org.mule.extension.ws.api.security.WssVerifySignatureSecurityStrategy;
import org.mule.extension.ws.internal.interceptor.NamespaceRestorerStaxInterceptor;
import org.mule.extension.ws.internal.interceptor.NamespaceSaverStaxInterceptor;
import org.mule.extension.ws.internal.interceptor.OutputMtomSoapAttachmentsInterceptor;
import org.mule.extension.ws.internal.interceptor.OutputSoapHeadersInterceptor;
import org.mule.extension.ws.internal.interceptor.SoapActionInterceptor;
import org.mule.extension.ws.internal.interceptor.StreamClosingInterceptor;
import org.mule.extension.ws.internal.security.callback.WSPasswordCallbackHandler;
import org.mule.runtime.api.connection.ConnectionException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.cxf.binding.soap.interceptor.CheckFaultInterceptor;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Web Service Consumer")
@Stories("Client Factory")
@RunWith(MockitoJUnitRunner.class)
public class ClientFactoryTestCase extends WscUnitTestCase {

  private static final String NAMESPACE_RESTORER = NamespaceRestorerStaxInterceptor.class.getSimpleName();
  private static final String NAMESPACE_SAVER = NamespaceSaverStaxInterceptor.class.getSimpleName();
  private static final String STREAM_CLOSING = StreamClosingInterceptor.class.getSimpleName();
  private static final String CHECK_FAULT = CheckFaultInterceptor.class.getSimpleName();
  private static final String OUT_SOAP_HEADERS = OutputSoapHeadersInterceptor.class.getSimpleName();
  private static final String SOAP_ACTION = SoapActionInterceptor.class.getSimpleName();
  private static final String MTOM = OutputMtomSoapAttachmentsInterceptor.class.getSimpleName();

  private static final String WSS_IN = WSS4JInInterceptor.class.getSimpleName();
  private static final String WSS_OUT = WSS4JOutInterceptor.class.getSimpleName();

  private static final ClientFactory factory = ClientFactory.getInstance();



  @Test
  @Description("Checks the creation of a basic client without security")
  public void basicClient() throws ConnectionException {
    Client client = factory.create(service.getAddress(), null, SOAP12, emptyList(), false);
    assertThat(client.getEndpoint().getBinding().getBindingInfo().getBindingId(), containsString("soap12"));
    assertThat(client.getEndpoint().get(MTOM_ENABLED), is(false));

    assertThat(inInterceptorNames(client),
               hasItems(NAMESPACE_RESTORER, NAMESPACE_SAVER, STREAM_CLOSING, CHECK_FAULT, OUT_SOAP_HEADERS, SOAP_ACTION));

    assertThat(inInterceptorNames(client), not(hasItems(MTOM, WSS_IN)));
  }

  @Test
  @Description("Checks the creation of a client that works with MTOM but with no security")
  public void basicClientMtom() throws ConnectionException {
    Client client = factory.create(service.getAddress(), null, SOAP11, emptyList(), true);
    assertThat(client.getEndpoint().get(MTOM_ENABLED), is(true));

    assertThat(inInterceptorNames(client),
               hasItems(NAMESPACE_RESTORER, NAMESPACE_SAVER, STREAM_CLOSING, CHECK_FAULT, OUT_SOAP_HEADERS, SOAP_ACTION, MTOM));

    assertThat(inInterceptorNames(client), not(hasItems(WSS_IN)));
  }

  @Test
  @Description("Checks the creation of a secured client")
  public void securedClient() throws ConnectionException {

    WssVerifySignatureSecurityStrategy verify = mock(WssVerifySignatureSecurityStrategy.class);
    WssSignSecurityStrategy sign = mock(WssSignSecurityStrategy.class);
    when(sign.buildSecurityProperties()).thenReturn(emptyMap());
    when(verify.buildSecurityProperties()).thenReturn(emptyMap());
    when(sign.securityAction()).thenReturn(WssSignSecurityStrategy.class.getName());
    when(verify.securityAction()).thenReturn(WssVerifySignatureSecurityStrategy.class.getName());
    when(sign.securityType()).thenReturn(OUTGOING);
    when(verify.securityType()).thenReturn(INCOMING);
    when(sign.buildPasswordCallbackHandler()).thenReturn(Optional.of(new WSPasswordCallbackHandler(1, ws -> {
    })));
    when(verify.buildPasswordCallbackHandler()).thenReturn(Optional.empty());

    Client client = factory.create(service.getAddress(), null, SOAP11, asList(sign, verify), false);

    assertThat(inInterceptorNames(client),
               hasItems(NAMESPACE_RESTORER, NAMESPACE_SAVER, STREAM_CLOSING, CHECK_FAULT, OUT_SOAP_HEADERS, SOAP_ACTION, WSS_IN));

    assertThat(outInterceptorNames(client), hasItems(WSS_OUT));

    WSS4JInInterceptor wssIn = (WSS4JInInterceptor) getInterceptorByName(client.getInInterceptors(), WSS_IN);
    Map<String, Object> inProps = wssIn.getProperties();
    assertThat(inProps.get(ACTION), is(verify.securityAction()));

    WSS4JOutInterceptor wssOut = (WSS4JOutInterceptor) getInterceptorByName(client.getOutInterceptors(), WSS_OUT);
    Map<String, Object> outProps = wssOut.getProperties();
    assertThat(outProps.get(ACTION), is(sign.securityAction()));
    assertThat(outProps.get(PW_CALLBACK_REF), notNullValue());
  }

  private List<String> inInterceptorNames(Client client) {
    return client.getInInterceptors().stream().map(i -> i.getClass().getSimpleName()).collect(toList());
  }

  private List<String> outInterceptorNames(Client client) {
    return client.getOutInterceptors().stream().map(i -> i.getClass().getSimpleName()).collect(toList());
  }

  private Interceptor getInterceptorByName(List<Interceptor<? extends Message>> interceptors, String name) {
    return interceptors.stream().filter(i -> i.getClass().getSimpleName().contains(name)).findAny().get();
  }
}
