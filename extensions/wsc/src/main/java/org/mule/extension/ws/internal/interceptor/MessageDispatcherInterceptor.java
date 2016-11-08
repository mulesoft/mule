/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.interceptor;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.cxf.message.Message.CONTENT_TYPE;
import static org.apache.cxf.message.Message.ENCODING;
import static org.apache.cxf.phase.Phase.SEND_ENDING;
import static org.mule.extension.ws.internal.ConsumeOperation.MULE_WSC_ENCODING;
import org.mule.extension.ws.api.exception.WscException;
import org.mule.extension.ws.internal.connection.WscConnection;
import org.mule.extension.ws.internal.transport.HttpDispatcher;
import org.mule.runtime.core.util.IOUtils;

import com.squareup.okhttp.Response;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.cxf.endpoint.ClientImpl;
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

  private String address;
  private final MessageObserver messageObserver;

  public MessageDispatcherInterceptor(String address, MessageObserver messageObserver) {
    super(SEND_ENDING);
    this.address = address;
    this.messageObserver = messageObserver;
  }

  // TODO: MULE-10783
  @Override
  public void handleMessage(Message message) throws Fault {

    String encoding = (String) message.getExchange().get(MULE_WSC_ENCODING);

    // Performs all the remaining interceptions before sending.
    message.getInterceptorChain().doIntercept(message);

    Response response = new HttpDispatcher().dispatch(address, message);

    // We wipe the request attachment list, so don't get mixed with the response ones.
    message.setAttachments(emptyList());

    String body;
    try {
      body = IOUtils.toString(response.body().byteStream());
    } catch (IOException e) {
      throw new WscException("Error while getting body response content");
    }

    Exchange exchange = message.getExchange();
    if (isNotBlank(body)) {

      // This needs to be set because we want the wsc closes the final stream,
      // otherwise cxf will close it too early when handling message in the StaxInEndingInterceptor.
      exchange.put(StaxInEndingInterceptor.STAX_IN_NOCLOSE, TRUE);

      InputStream is = new ByteArrayInputStream(body.getBytes());
      Message inMessage = new MessageImpl();

      // TODO make encoding policy
      inMessage.put(ENCODING, encoding);

      String contentType = response.header(CONTENT_TYPE);
      inMessage.put(CONTENT_TYPE, contentType);
      inMessage.setContent(InputStream.class, is);
      inMessage.setExchange(exchange);
      messageObserver.onMessage(inMessage);
    } else {
      exchange.put(ClientImpl.FINISHED, TRUE);
      throw new WscException("Web Service Response is blank");
    }
  }
}
