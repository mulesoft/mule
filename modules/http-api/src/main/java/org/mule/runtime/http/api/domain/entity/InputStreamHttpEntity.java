/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.entity;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.OptionalLong.empty;
import static java.util.OptionalLong.of;
import static org.mule.runtime.api.util.IOUtils.toByteArray;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.http.api.domain.entity.multipart.HttpPart;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Representation of a stream HTTP body.
 *
 * @since 4.0
 */
@NoExtend
public class InputStreamHttpEntity implements HttpEntity {

  private OptionalLong contentLength;
  private InputStream inputStream;

  public InputStreamHttpEntity(InputStream inputStream) {
    requireNonNull(inputStream, "HTTP entity stream cannot be null.");
    this.inputStream = inputStream;
    this.contentLength = empty();
  }

  /**
   * @deprecated Use {@link #InputStreamHttpEntity(InputStream, OptionalLong)} or
   *             {@link #InputStreamHttpEntity(InputStream, long)} instead.
   */
  @Deprecated
  public InputStreamHttpEntity(InputStream inputStream, Long contentLength) {
    this(inputStream);
    this.contentLength = contentLength == null ? empty() : of(contentLength);
  }

  /**
   * @since 4.2
   */
  public InputStreamHttpEntity(InputStream inputStream, long contentLength) {
    this(inputStream);
    this.contentLength = of(contentLength);
  }

  /**
   * @since 4.2
   */
  public InputStreamHttpEntity(InputStream inputStream, OptionalLong contentLength) {
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
    return toByteArray(this.inputStream);
  }

  @Override
  public Collection<HttpPart> getParts() {
    return emptyList();
  }

  @Override
  public Optional<Long> getLength() {
    return contentLength.isPresent() ? Optional.of(contentLength.getAsLong()) : Optional.empty();
  }

  @Override
  public OptionalLong getBytesLength() {
    return contentLength;
  }

}
