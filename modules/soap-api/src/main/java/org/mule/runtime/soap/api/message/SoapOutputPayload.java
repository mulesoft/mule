/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.soap.api.message;

import static com.google.common.collect.ImmutableMap.copyOf;
import static org.mule.runtime.api.metadata.MediaType.XML;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;
import org.mule.runtime.extension.api.soap.SoapAttachment;
import com.google.common.collect.ImmutableMap;
import java.io.InputStream;
import java.util.Map;

/**
 * A simple container object that carries the SOAP body and the attachments bounded to the response.
 *
 * @since 4.0
 */
public class SoapOutputPayload {

  private final TypedValue<InputStream> body;
  private final Map<String, TypedValue<InputStream>> attachments;
  private final Map<String, String> headers;

  public SoapOutputPayload(InputStream body, Map<String, SoapAttachment> attachments, Map<String, String> headers,
                           StreamingHelper helper) {
    this.body = wrapBody(body, helper);
    this.headers = copyOf(headers);
    this.attachments = wrapAttachments(attachments, helper);
  }

  private TypedValue<InputStream> wrapBody(InputStream body, StreamingHelper helper) {
    return new TypedValue(helper.resolveCursorProvider(body), DataType.builder().type(InputStream.class).mediaType(XML).build());
  }

  private Map<String, TypedValue<InputStream>> wrapAttachments(Map<String, SoapAttachment> attachments, StreamingHelper helper) {
    ImmutableMap.Builder<String, TypedValue<InputStream>> wrapped = ImmutableMap.builder();
    attachments.forEach((k, v) -> wrapped.put(k, new TypedValue(helper.resolveCursorProvider(v.getContent()),
                                                                DataType.builder().type(InputStream.class)
                                                                    .mediaType(v.getContentType()).build())));
    return wrapped.build();
  }

  public TypedValue<InputStream> getBody() {
    return body;
  }

  public Map<String, TypedValue<InputStream>> getAttachments() {
    return attachments;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }
}
