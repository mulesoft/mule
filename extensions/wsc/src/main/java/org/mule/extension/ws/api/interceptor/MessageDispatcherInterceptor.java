/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api.interceptor;

import static org.apache.cxf.phase.Phase.SEND_ENDING;
import static org.apache.logging.log4j.util.Strings.isNotBlank;
import org.mule.extension.ws.api.transport.HttpDispatcher;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxInEndingInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.transport.MessageObserver;

public class MessageDispatcherInterceptor extends AbstractPhaseInterceptor<Message> {

  private String address;
  private final MessageObserver messageObserver;

  public MessageDispatcherInterceptor(String address, MessageObserver messageObserver) {
    super(SEND_ENDING);
    this.address = address;
    this.messageObserver = messageObserver;
  }

  @Override
  public void handleMessage(Message msg) throws Fault {

    // Performs all the remaining interceptions before sending.
    msg.getInterceptorChain().doIntercept(msg);

    OutputStream content = msg.getContent(OutputStream.class);
    String dispatched = new HttpDispatcher().dispatch(address, content);

    Exchange exchange = msg.getExchange();
    if (isNotBlank(dispatched)) {

      // This needs to be set because we want the wsc closes the final stream,
      // otherwise cxf will close it too early when handling message in the StaxInEndingInterceptor.
      exchange.put(StaxInEndingInterceptor.STAX_IN_NOCLOSE, Boolean.TRUE);

      InputStream is = new ByteArrayInputStream(dispatched.getBytes());
      Message inMessage = new MessageImpl();

      String encoding = "UTF-8";
      inMessage.put(Message.ENCODING, encoding);

      String contentType = "text/xml";
      if (!contentType.contains("charset")) {
        contentType += "; charset=" + encoding;
      }

      inMessage.put(Message.CONTENT_TYPE, contentType);
      inMessage.setContent(InputStream.class, is);
      inMessage.setExchange(exchange);
      messageObserver.onMessage(inMessage);
    } else {
      exchange.put(ClientImpl.FINISHED, Boolean.TRUE);
    }
  }
}
