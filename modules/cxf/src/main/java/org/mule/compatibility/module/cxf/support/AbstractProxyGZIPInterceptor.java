/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.compatibility.module.cxf.support;

import static com.google.common.net.HttpHeaders.CONTENT_ENCODING;

import org.mule.extension.http.api.HttpAttributes;
import org.mule.runtime.core.api.message.InternalMessage;

import java.util.Arrays;
import java.util.List;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPOutInterceptor;

public abstract class AbstractProxyGZIPInterceptor extends AbstractPhaseInterceptor<Message> {

  public AbstractProxyGZIPInterceptor(String phase) {
    super(phase);
  }

  protected boolean isEncoded(InternalMessage message) {
    boolean isEncoded = false;

    String contentEncoding = message.getInboundProperty(CONTENT_ENCODING);
    if (contentEncoding == null && message.getAttributes() instanceof HttpAttributes) {
      // TODO MULE-9857 Make message properties case sensitive
      contentEncoding = ((HttpAttributes) message.getAttributes()).getHeaders().get(CONTENT_ENCODING.toLowerCase());
    }
    if (contentEncoding == null) {
      contentEncoding = message.getInboundProperty(GZIPOutInterceptor.SOAP_JMS_CONTENTENCODING);
    }
    if (contentEncoding != null) {
      List<String> encodings = Arrays.asList(GZIPOutInterceptor.ENCODINGS.split(contentEncoding.trim()));
      isEncoded = encodings.contains("gzip") || encodings.contains("x-gzip");
    }

    return isEncoded;
  }
}
