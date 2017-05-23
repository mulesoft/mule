/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.soap.api.message;

import static java.util.Collections.unmodifiableMap;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.soap.SoapAttachment;

import java.io.InputStream;
import java.util.Map;

/**
 * Immutable implementation of a {@link SoapRequest}.
 *
 * @since 4.0
 */
public final class ImmutableSoapRequest implements SoapRequest {

  private final InputStream content;
  private final Map<String, String> soapHeaders;
  private final Map<String, SoapAttachment> attachments;
  private final Map<String, String> transportHeaders;
  private final MediaType contentType;
  private final String operation;

  ImmutableSoapRequest(InputStream content,
                       Map<String, String> soapHeaders,
                       Map<String, String> transportHeaders,
                       Map<String, SoapAttachment> attachments,
                       MediaType contentType,
                       String operation) {
    this.content = content;
    this.soapHeaders = unmodifiableMap(soapHeaders);
    this.transportHeaders = unmodifiableMap(transportHeaders);
    this.attachments = unmodifiableMap(attachments);
    this.contentType = contentType;
    this.operation = operation;
  }

  @Override
  public InputStream getContent() {
    return content;
  }

  @Override
  public Map<String, String> getSoapHeaders() {
    return soapHeaders;
  }

  @Override
  public Map<String, String> getTransportHeaders() {
    return transportHeaders;
  }

  @Override
  public Map<String, SoapAttachment> getAttachments() {
    return attachments;
  }

  @Override
  public MediaType getContentType() {
    return contentType;
  }

  @Override
  public String getOperation() {
    return operation;
  }
}
