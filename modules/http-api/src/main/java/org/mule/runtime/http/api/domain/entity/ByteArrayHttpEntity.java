/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
import java.util.OptionalLong;

/**
 * Represents a byte array HTTP body.
 *
 * @since 4.0
 */
public final class ByteArrayHttpEntity implements HttpEntity {

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

  @Override
  public OptionalLong getBytesLength() {
    return OptionalLong.of(content.length);
  }


}
