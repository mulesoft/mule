/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.transport;

import org.mule.services.soap.client.SoapCxfClient;
import org.mule.services.soap.interceptor.MessageDispatcherInterceptor;

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
import org.apache.cxf.ws.addressing.EndpointReferenceType;

/**
 *
 * @since 4.0
 */
final class SoapServiceConduit extends AbstractConduit {

  private static final Logger LOGGER = Logger.getLogger(SoapServiceConduit.class.getSimpleName());

  SoapServiceConduit(EndpointReferenceType t) {
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
    message.getInterceptorChain().add(new MessageDispatcherInterceptor(getMessageObserver()));
  }

  private void addAttachments(Message message) {
    List<Attachment> soapAttachments = (List<Attachment>) message.getExchange().get(SoapCxfClient.MULE_ATTACHMENTS_KEY);
    message.setAttachments(soapAttachments);
  }

  private void addHeaders(Message message) {
    List<SoapHeader> soapHeaders = (List<SoapHeader>) message.getExchange().get(SoapCxfClient.MULE_HEADERS_KEY);
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
