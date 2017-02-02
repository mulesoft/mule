/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.interceptor;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static org.apache.cxf.message.Message.CONTENT_TYPE;
import static org.apache.cxf.message.Message.ENCODING;
import static org.apache.cxf.phase.Phase.SEND_ENDING;
import static org.mule.extension.ws.internal.connection.WscClient.MULE_WSC_ENCODING;
import static org.mule.extension.ws.internal.connection.WscClient.WSC_DISPATCHER;
import org.mule.extension.ws.internal.connection.WscConnection;
import org.mule.extension.ws.internal.transport.WscDispatcher;
import org.mule.extension.ws.internal.transport.WscResponse;

import java.io.InputStream;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxInEndingInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.transport.MessageObserver;

/**
 * CXF interceptor that uses an underlying transport configuration, specified in the {@link WscConnection}, to send the SOAP
 * message and inject the obtained response into the CXF <strong>in</strong> (response) interceptors lifecycle.
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

    WscDispatcher dispatcher = (WscDispatcher) exchange.get(WSC_DISPATCHER);
    WscResponse response = dispatcher.dispatch(message);

    // This needs to be set because we want the wsc closes the final stream,
    // otherwise cxf will close it too early when handling message in the StaxInEndingInterceptor.
    exchange.put(StaxInEndingInterceptor.STAX_IN_NOCLOSE, TRUE);

    Message inMessage = new MessageImpl();
    // TODO make encoding policy
    inMessage.put(ENCODING, encoding);
    inMessage.put(CONTENT_TYPE, response.getContentType());
    inMessage.setContent(InputStream.class, response.getBody());
    inMessage.setExchange(exchange);
    messageObserver.onMessage(inMessage);
  }
}
