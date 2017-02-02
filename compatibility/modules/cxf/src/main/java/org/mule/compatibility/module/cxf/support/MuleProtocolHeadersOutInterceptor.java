/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf.support;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.cxf.message.Message.CONTENT_TYPE;
import static org.apache.cxf.message.Message.PROTOCOL_HEADERS;
import static org.mule.service.http.api.HttpConstants.Method.POST;
import static org.mule.runtime.module.http.api.HttpConstants.RequestProperties.HTTP_METHOD_PROPERTY;
import static org.mule.runtime.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;

import org.mule.compatibility.module.cxf.CxfConstants;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MuleProtocolHeadersOutInterceptor extends AbstractPhaseInterceptor<Message> {

  private static final Logger logger = LoggerFactory.getLogger(MuleProtocolHeadersOutInterceptor.class);

  public MuleProtocolHeadersOutInterceptor() {
    super(Phase.PRE_STREAM);
    getAfter().add(AttachmentOutInterceptor.class.getName());
  }

  @Override
  public void handleMessage(Message message) throws Fault {
    Event event = (Event) message.getExchange().get(CxfConstants.MULE_EVENT);

    if (event == null) {
      return;
    }

    if (event.getMessage() == null) {
      return;
    }

    InternalMessage.Builder messageBuilder = InternalMessage.builder(event.getMessage());

    extractAndSetContentType(message, messageBuilder);
    extractAndSet(message, messageBuilder, Message.RESPONSE_CODE, HTTP_STATUS_PROPERTY);

    String method = (String) message.get(Message.HTTP_REQUEST_METHOD);
    final String finalMethod = method != null ? method : POST.name();
    messageBuilder.addOutboundProperty(HTTP_METHOD_PROPERTY, finalMethod);

    Map<String, List<String>> reqHeaders = CastUtils.cast((Map<?, ?>) message.get(PROTOCOL_HEADERS));
    if (reqHeaders != null) {
      for (Map.Entry<String, List<String>> e : reqHeaders.entrySet()) {
        String key = e.getKey();
        String val = format(e.getValue());
        messageBuilder.addOutboundProperty(key, val);
      }
    }
    event = Event.builder(event).message(messageBuilder.build()).build();
    message.getExchange().put(CxfConstants.MULE_EVENT, event);

    if (!Boolean.TRUE.equals(message.containsKey(Message.REQUESTOR_ROLE))) {
      message.getInterceptorChain().pause();
    }
  }

  private void extractAndSet(Message message, InternalMessage.Builder builder, String cxfHeader, String muleHeader) {
    if (message.get(cxfHeader) instanceof Serializable) {
      Serializable val = (Serializable) message.get(cxfHeader);
      if (val != null) {
        builder.addOutboundProperty(muleHeader, val);
      }
    } else {
      logger.warn("The header " + cxfHeader + "is not serializable and will not be propagated by Mule");
    }
  }

  private void extractAndSetContentType(Message message, InternalMessage.Builder builder) {
    String ct = (String) message.get(CONTENT_TYPE);
    message.getExchange().put(CONTENT_TYPE, ct);
    if (ct != null) {
      builder.mediaType(MediaType.parse(ct).withCharset(getEncoding(message)));
    }
  }

  private Charset getEncoding(Message message) {
    Exchange ex = message.getExchange();
    String encoding = (String) message.get(Message.ENCODING);
    if (encoding == null && ex.getInMessage() != null) {
      encoding = (String) ex.getInMessage().get(Message.ENCODING);
      message.put(Message.ENCODING, encoding);
    }

    if (encoding == null) {
      message.put(Message.ENCODING, UTF_8.name());
      return UTF_8;
    } else {
      return Charset.forName(encoding);
    }
  }

  private String format(List<String> value) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;

    for (String s : value) {
      if (!first) {
        sb.append(", ");
        first = false;
      } else {
        first = false;
      }

      sb.append(s);
    }
    return sb.toString();
  }
}


