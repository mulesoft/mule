/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.api.message;

import static org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import org.mule.runtime.api.metadata.MediaType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

public class SoapRequestBuilder {

  private InputStream content;
  private ImmutableList.Builder<SoapHeader> soapHeaders = ImmutableList.builder();
  private ImmutableMap.Builder<String, String> transportHeaders = ImmutableMap.builder();
  private ImmutableList.Builder<SoapAttachment> attachments = ImmutableList.builder();
  private MediaType contentType = APPLICATION_XML;
  private String operation;

  SoapRequestBuilder() {}

  public SoapRequestBuilder withContent(InputStream content) {
    this.content = content;
    return this;
  }

  public SoapRequestBuilder withContent(String content) {
    this.content = new ByteArrayInputStream(content.getBytes());
    return this;
  }

  public SoapRequestBuilder withSoapHeaders(List<SoapHeader> soapHeaders) {
    this.soapHeaders.addAll(soapHeaders);
    return this;
  }

  public SoapRequestBuilder withSoapHeader(SoapHeader soapHeader) {
    this.soapHeaders.add(soapHeader);
    return this;
  }

  public SoapRequestBuilder withTransportHeader(String key, String value) {
    this.transportHeaders.put(key, value);
    return this;
  }

  public SoapRequestBuilder withAttachment(SoapAttachment attachments) {
    this.attachments.add(attachments);
    return this;
  }

  public SoapRequestBuilder ofContentType(MediaType contentType) {
    this.contentType = contentType;
    return this;
  }

  public SoapRequestBuilder withOperation(String operation) {
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
