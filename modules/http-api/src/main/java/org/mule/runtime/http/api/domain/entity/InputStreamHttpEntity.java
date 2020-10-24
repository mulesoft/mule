/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.entity;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.util.IOUtils;
import org.mule.runtime.http.api.domain.entity.multipart.HttpPart;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;

/**
 * Representation of a stream HTTP body.
 *
 * @since 4.0
 */
@NoExtend
public class InputStreamHttpEntity implements HttpEntity {

  private Long contentLength;
  private InputStream inputStream;

  public InputStreamHttpEntity(InputStream inputStream) {
    requireNonNull(inputStream, "HTTP entity stream cannot be null.");
    this.inputStream = inputStream;
  }

  public InputStreamHttpEntity(InputStream inputStream, Long contentLength) {
    this(inputStream);
    this.contentLength = contentLength;
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
    return IOUtils.toByteArray(this.inputStream);
  }

  @Override
  public Collection<HttpPart> getParts() {
    return emptyList();
  }

  @Override
  public Optional<Long> getLength() {
    return ofNullable(contentLength);
  }

}
