/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.transport;

import static org.mule.extension.ws.internal.ConsumeOperation.MULE_ATTACHMENTS_KEY;
import static org.mule.extension.ws.internal.ConsumeOperation.MULE_HEADERS_KEY;
import org.mule.extension.ws.internal.ConsumeOperation;
import org.mule.extension.ws.internal.interceptor.MessageDispatcherInterceptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Logger;

import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

/**
 * Custom Web Service Consumer {@link Conduit} implementation to prepare the soap message with the data received in the
 * {@link ConsumeOperation} use a custom underlying transport to send the SOAP requests and process the response by injecting
 * the {@link MessageDispatcherInterceptor} into the interceptors lifecycle.
 *
 * @since 4.0
 */
final class WscConduit extends AbstractConduit {

  private static final Logger LOGGER = Logger.getLogger(WscConduit.class.getSimpleName());

  WscConduit(EndpointReferenceType t) {
    super(t);
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void prepare(Message message) throws IOException {
    // Set a new OutputStream where the message is going to be handled.
    message.setContent(OutputStream.class, new ByteArrayOutputStream());
    addHeaders(message);
    addAttachments(message);
    addMessageDispatcher(message);
  }

  private void addMessageDispatcher(Message message) {
    message.getInterceptorChain().add(new MessageDispatcherInterceptor(target.getAddress().getValue(), getMessageObserver()));
  }

  private void addAttachments(Message message) {
    List<Attachment> soapAttachments = (List<Attachment>) message.getExchange().get(MULE_ATTACHMENTS_KEY);
    message.setAttachments(soapAttachments);
  }

  private void addHeaders(Message message) {
    List<SoapHeader> soapHeaders = (List<SoapHeader>) message.getExchange().get(MULE_HEADERS_KEY);
    soapHeaders.forEach(header -> ((SoapMessage) message).getHeaders().add(header));
  }

  @Override
  public void close(Message msg) throws IOException {
    OutputStream os = msg.getContent(OutputStream.class);
    if (os != null) {
      os.close();
    }
    InputStream is = msg.getContent(InputStream.class);
    if (is != null) {
      is.close();
    }
  }
}
