/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.message;

import static java.util.Collections.unmodifiableMap;
import static org.mule.runtime.api.message.Message.builder;
import static org.mule.runtime.core.message.DefaultMultiPartPayload.BODY_ATTRIBUTES;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.message.PartAttributes;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.soap.SoapAttachment;
import org.mule.services.soap.api.message.SoapAttributes;
import org.mule.services.soap.api.message.SoapMultipartPayload;
import org.mule.services.soap.api.message.SoapResponse;

import com.google.common.collect.ImmutableList;

import java.io.InputStream;
import java.util.Map;

/**
 * Immutable {@link SoapResponse} implementation.
 *
 * @since 4.0
 */
public final class ImmutableSoapResponse implements SoapResponse {

  private final InputStream content;
  private final Map<String, String> soapHeaders;
  private final Map<String, String> transportHeaders;
  private final Map<String, SoapAttachment> attachments;
  private final MediaType contentType;

  public ImmutableSoapResponse(InputStream content,
                               Map<String, String> soapHeaders,
                               Map<String, String> transportHeaders,
                               Map<String, SoapAttachment> attachments,
                               MediaType contentType) {
    this.content = content;
    this.soapHeaders = unmodifiableMap(soapHeaders);
    this.transportHeaders = unmodifiableMap(transportHeaders);
    this.attachments = unmodifiableMap(attachments);
    this.contentType = contentType;
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
  public Result<?, SoapAttributes> getAsResult() {
    SoapAttributes attributes = new SoapAttributes(soapHeaders, transportHeaders);
    if (attachments.isEmpty()) {
      return Result.<InputStream, SoapAttributes>builder().output(content).attributes(attributes).mediaType(contentType).build();
    } else {
      ImmutableList.Builder<Message> parts = ImmutableList.builder();
      parts.add(builder().payload(content).mediaType(contentType).attributes(BODY_ATTRIBUTES).build());
      attachments.forEach((name, attachment) -> parts.add(builder()
          .payload(attachment.getContent())
          .mediaType(attachment.getContentType())
          .attributes(new PartAttributes(name))
          .build()));
      return Result.<SoapMultipartPayload, SoapAttributes>builder()
          .output(new SoapMultipartPayload(parts.build()))
          .attributes(attributes)
          .build();
    }
  }
}
