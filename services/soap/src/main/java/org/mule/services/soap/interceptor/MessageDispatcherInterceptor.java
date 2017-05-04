/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.interceptor;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static org.apache.cxf.interceptor.StaxInEndingInterceptor.STAX_IN_NOCLOSE;
import static org.apache.cxf.message.Message.CONTENT_TYPE;
import static org.apache.cxf.message.Message.ENCODING;
import static org.apache.cxf.phase.Phase.SEND_ENDING;
import static org.mule.services.soap.client.SoapCxfClient.MULE_SOAP_ACTION;
import static org.mule.services.soap.client.SoapCxfClient.MULE_WSC_ADDRESS;
import static org.mule.services.soap.client.SoapCxfClient.MULE_WSC_ENCODING;
import static org.mule.services.soap.client.SoapCxfClient.WSC_DISPATCHER;
import static org.mule.services.soap.interceptor.SoapActionInterceptor.SOAP_ACTION;
import org.mule.runtime.extension.api.soap.message.DispatchingRequest;
import org.mule.runtime.extension.api.soap.message.DispatchingResponse;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;
import org.mule.services.soap.api.client.SoapClientConfiguration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.interceptor.Fault;
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
   * {@inheritDoc}
   * <p>
   * Intercepts the SOAP message and performs the dispatch of it, receiving the response and
   * sending it to the IN intercepting processor chain.
   */
  @Override
  public void handleMessage(Message message) throws Fault {
    Exchange exchange = message.getExchange();

    // Performs all the remaining interceptions before sending.
    message.getInterceptorChain().doIntercept(message);

    // Wipe the request attachment list, so don't get mixed with the response ones.
    message.setAttachments(emptyList());

    MessageDispatcher dispatcher = (MessageDispatcher) exchange.get(WSC_DISPATCHER);
    DispatchingResponse response = dispatcher.dispatch(getDispatchingRequest(message));

    // This needs to be set because we want the wsc closes the final stream,
    // otherwise cxf will close it too early when handling message in the StaxInEndingInterceptor.
    exchange.put(STAX_IN_NOCLOSE, TRUE);

    Message inMessage = new MessageImpl();
    // TODO make encoding policy
    inMessage.put(ENCODING, exchange.get(MULE_WSC_ENCODING));
    inMessage.put(CONTENT_TYPE, response.getContentType());
    inMessage.setContent(InputStream.class, response.getContent());
    inMessage.setExchange(exchange);
    messageObserver.onMessage(inMessage);
  }

  private DispatchingRequest getDispatchingRequest(Message message) {
    Exchange exchange = message.getExchange();
    String action = (String) exchange.get(MULE_SOAP_ACTION);
    Map<String, String> headers = new HashMap<>();
    if (action != null) {
      headers.put(SOAP_ACTION, action);
    }
    InputStream content = new ByteArrayInputStream(message.getContent(OutputStream.class).toString().getBytes());
    return new DispatchingRequest(content, (String) exchange.get(MULE_WSC_ADDRESS), (String) message.get(CONTENT_TYPE), headers);
  }
}
