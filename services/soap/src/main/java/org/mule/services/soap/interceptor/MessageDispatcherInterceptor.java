/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.interceptor;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static org.apache.cxf.message.Message.CONTENT_TYPE;
import static org.apache.cxf.message.Message.ENCODING;
import static org.apache.cxf.phase.Phase.SEND_ENDING;
import static org.mule.services.soap.client.SoapCxfClient.MULE_SOAP_ACTION;
import static org.mule.services.soap.client.SoapCxfClient.MULE_WSC_ENCODING;
import static org.mule.services.soap.client.SoapCxfClient.WSC_DISPATCHER;
import org.mule.services.soap.api.client.DispatcherResponse;
import org.mule.services.soap.api.client.MessageDispatcher;
import org.mule.services.soap.api.client.SoapClientConfiguration;

import com.google.common.collect.ImmutableMap;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxInEndingInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.transport.MessageObserver;

/**
 * CXF interceptor that uses a custom {@link MessageDispatcher}, specified in the {@link SoapClientConfiguration} to send a
 * SOAP message and inject the obtained response into the CXF <strong>in</strong> (response) interceptors lifecycle.
 *
 * @since 4.0
 */
public class MessageDispatcherInterceptor extends AbstractPhaseInterceptor<Message> {

  private final MessageObserver messageObserver;

  public MessageDispatcherInterceptor(MessageObserver messageObserver) {
    super(SEND_ENDING);
    this.messageObserver = messageObserver;
  }

  /**
   * Intercepts the SOAP message and performs the dispatch of it, receiving the response and
   * sending it to the IN intercepting processor chain.
   */
  @Override
  public void handleMessage(Message message) throws Fault {
    Exchange exchange = message.getExchange();

    String encoding = (String) message.getExchange().get(MULE_WSC_ENCODING);

    // Performs all the remaining interceptions before sending.
    message.getInterceptorChain().doIntercept(message);

    // Wipe the request attachment list, so don't get mixed with the response ones.
    message.setAttachments(emptyList());

    Map<String, String> props = buildProperties(message);
    MessageDispatcher dispatcher = (MessageDispatcher) exchange.get(WSC_DISPATCHER);
    OutputStream content = message.getContent(OutputStream.class);
    DispatcherResponse response = dispatcher.dispatch(new ByteArrayInputStream(content.toString().getBytes()), props);

    // This needs to be set because we want the wsc closes the final stream,
    // otherwise cxf will close it too early when handling message in the StaxInEndingInterceptor.
    exchange.put(StaxInEndingInterceptor.STAX_IN_NOCLOSE, TRUE);

    Message inMessage = new MessageImpl();
    // TODO make encoding policy
    inMessage.put(ENCODING, encoding);
    inMessage.put(CONTENT_TYPE, response.getContentType());
    inMessage.setContent(InputStream.class, response.getContent());
    inMessage.setExchange(exchange);
    messageObserver.onMessage(inMessage);
  }

  private Map<String, String> buildProperties(Message message) {
    ImmutableMap.Builder<String, String> headers = ImmutableMap.builder();
    headers.put(CONTENT_TYPE, (String) message.get(CONTENT_TYPE));
    String soapAction = (String) message.getExchange().get(MULE_SOAP_ACTION);
    if (soapAction != null) {
      headers.put(SoapActionInterceptor.SOAP_ACTION, soapAction);
    }
    return headers.build();
  }
}
