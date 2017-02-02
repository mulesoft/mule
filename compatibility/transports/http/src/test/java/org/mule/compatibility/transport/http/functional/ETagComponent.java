/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import static org.mule.compatibility.transport.http.HttpConnector.HTTP_STATUS_PROPERTY;
import static org.mule.compatibility.transport.http.HttpConstants.HEADER_ETAG;
import static org.mule.compatibility.transport.http.HttpConstants.HEADER_IF_NONE_MATCH;
import static org.mule.compatibility.transport.http.HttpConstants.SC_NOT_MODIFIED;

import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.lifecycle.Callable;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.util.StringUtils;

public class ETagComponent implements Callable {

  private static String ETAG_VALUE = "0123456789";

  @Override
  public Object onCall(MuleEventContext eventContext) throws Exception {
    InternalMessage message = (InternalMessage) eventContext.getMessage();

    InternalMessage.Builder messageBuilder = InternalMessage.builder(message);
    String etag = message.getOutboundProperty(HEADER_IF_NONE_MATCH);
    if ((etag != null) && etag.equals(ETAG_VALUE)) {
      messageBuilder.payload(StringUtils.EMPTY);
      messageBuilder.addOutboundProperty(HTTP_STATUS_PROPERTY, SC_NOT_MODIFIED);
    }

    messageBuilder.addOutboundProperty(HEADER_ETAG, ETAG_VALUE);
    return messageBuilder.build();
  }
}


