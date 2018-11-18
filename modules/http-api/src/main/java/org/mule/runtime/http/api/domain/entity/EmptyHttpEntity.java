/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.entity;

import static java.util.Collections.emptyList;
import static java.util.Optional.of;

import org.mule.runtime.http.api.domain.entity.multipart.HttpPart;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Represents an HTTP empty body.
 *
 * @since 4.0
 */
public final class EmptyHttpEntity implements HttpEntity {

  private static final byte[] NO_BYTES = new byte[0];

  @Override
  public boolean isStreaming() {
    return false;
  }

  @Override
  public boolean isComposed() {
    return false;
  }

  @Override
  public InputStream getContent() {
    return new ByteArrayInputStream(NO_BYTES);
  }

  @Override
  public byte[] getBytes() {
    return NO_BYTES;
  }

  @Override
  public Collection<HttpPart> getParts() {
    return emptyList();
  }

  @Override
  public Optional<Long> getLength() {
    return of(0L);
  }

  @Override
  public OptionalLong getBytesLength() {
    return OptionalLong.of(0L);
  }
}
