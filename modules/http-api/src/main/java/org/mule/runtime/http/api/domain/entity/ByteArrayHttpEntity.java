/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.entity;

import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import org.mule.runtime.http.api.domain.entity.multipart.HttpPart;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;

/**
 * Represents a byte array HTTP body.
 *
 * @since 4.0
 */
public class ByteArrayHttpEntity implements HttpEntity {

  private byte[] content;

  public ByteArrayHttpEntity(byte[] content) {
    checkNotNull(content, "HTTP entity content cannot be null.");
    this.content = content;
  }

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
    return new ByteArrayInputStream(content);
  }

  @Override
  public byte[] getBytes() {
    return this.content;
  }

  @Override
  public Collection<HttpPart> getParts() {
    return emptyList();
  }

  @Override
  public Optional<Long> getLength() {
    return of((long) content.length);
  }

}
