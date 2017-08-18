/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.soap.api.message;

import static org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.soap.SoapAttachment;

import com.google.common.collect.ImmutableMap;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

public class SoapRequestBuilder {

  private InputStream content;
  private ImmutableMap.Builder<String, String> soapHeaders = ImmutableMap.builder();
  private ImmutableMap.Builder<String, String> transportHeaders = ImmutableMap.builder();
  private ImmutableMap.Builder<String, SoapAttachment> attachments = ImmutableMap.builder();
  private MediaType contentType = APPLICATION_XML;
  private String operation;

  SoapRequestBuilder() {}

  public SoapRequestBuilder content(InputStream content) {
    this.content = content;
    return this;
  }

  public SoapRequestBuilder content(String content) {
    this.content = new ByteArrayInputStream(content.getBytes());
    return this;
  }

  public SoapRequestBuilder soapHeaders(Map<String, String> soapHeaders) {
    this.soapHeaders.putAll(soapHeaders);
    return this;
  }

  public SoapRequestBuilder transportHeader(String key, String value) {
    this.transportHeaders.put(key, value);
    return this;
  }

  public SoapRequestBuilder transportHeaders(Map<String, String> headers) {
    this.transportHeaders.putAll(headers);
    return this;
  }

  public SoapRequestBuilder attachment(String name, SoapAttachment attachment) {
    this.attachments.put(name, attachment);
    return this;
  }

  public SoapRequestBuilder attachments(Map<String, SoapAttachment> attachments) {
    this.attachments.putAll(attachments);
    return this;
  }

  public SoapRequestBuilder contentType(MediaType contentType) {
    this.contentType = contentType;
    return this;
  }

  public SoapRequestBuilder operation(String operation) {
    this.operation = operation;
    return this;
  }

  public ImmutableSoapRequest build() {
    checkNotNull(operation, "Missing executing operation");
    return new ImmutableSoapRequest(content,
                                    soapHeaders.build(),
                                    transportHeaders.build(),
                                    attachments.build(),
                                    contentType,
                                    operation);
  }
}
