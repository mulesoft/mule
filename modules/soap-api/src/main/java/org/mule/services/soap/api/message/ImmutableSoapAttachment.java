/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.api.message;

import org.mule.runtime.api.metadata.MediaType;

import java.io.InputStream;

/**
 * Immutable implementation of a {@link SoapAttachment}.
 *
 * @since 4.0
 */
public class ImmutableSoapAttachment implements SoapAttachment {

  private final MediaType contentType;
  private final String id;
  private final InputStream content;

  public ImmutableSoapAttachment(String id, MediaType contentType, InputStream content) {
    this.contentType = contentType;
    this.id = id;
    this.content = content;
  }

  public ImmutableSoapAttachment(String id, String contentType, InputStream content) {
    this.contentType = MediaType.parse(contentType);
    this.id = id;
    this.content = content;
  }

  @Override
  public MediaType getContentType() {
    return contentType;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public InputStream getContent() {
    return content;
  }
}
