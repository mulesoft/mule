/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.entity;

import static java.util.Collections.emptyList;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import org.mule.runtime.http.api.domain.entity.multipart.HttpPart;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import sun.misc.IOUtils;

/**
 * Representation of a stream HTTP body.
 *
 * @since 4.0
 */
public class InputStreamHttpEntity implements HttpEntity {

  private Integer contentLength;
  private InputStream inputStream;

  public InputStreamHttpEntity(InputStream inputStream) {
    checkNotNull(inputStream, "HTTP entity stream cannot be null.");
    this.inputStream = inputStream;
  }

  public InputStreamHttpEntity(Integer contentLength, InputStream inputStream) {
    this(inputStream);
    this.contentLength = contentLength;
  }

  public int getContentLength() {
    return this.contentLength;
  }

  public boolean hasContentLength() {
    return this.contentLength != null;
  }

  @Override
  public boolean isStreaming() {
    return true;
  }

  @Override
  public boolean isComposed() {
    return false;
  }

  @Override
  public InputStream getContent() {
    return this.inputStream;
  }

  @Override
  public byte[] getBytes() throws IOException {
    return IOUtils.readFully(this.inputStream, -1, true);
  }

  @Override
  public Collection<HttpPart> getParts() {
    return emptyList();
  }

}
