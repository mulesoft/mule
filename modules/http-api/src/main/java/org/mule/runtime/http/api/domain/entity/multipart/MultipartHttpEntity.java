/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.domain.entity.multipart;

import static java.util.Optional.empty;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;

import org.mule.runtime.http.api.domain.entity.HttpEntity;

import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Represents a multipart HTTP body.
 *
 * @since 4.0
 */
public final class MultipartHttpEntity implements HttpEntity {

  private final Collection<HttpPart> parts;

  public MultipartHttpEntity(final Collection<HttpPart> parts) {
    checkNotNull(parts, "HTTP entity parts cannot be null");
    this.parts = parts;
  }

  @Override
  public boolean isStreaming() {
    return false;
  }

  @Override
  public boolean isComposed() {
    return true;
  }

  @Override
  public InputStream getContent() {
    return null;
  }

  @Override
  public byte[] getBytes() {
    return null;
  }

  @Override
  public Collection<HttpPart> getParts() {
    return this.parts;
  }

  @Override
  public Optional<Long> getLength() {
    return empty();
  }

  @Override
  public OptionalLong getBytesLength() {
    return OptionalLong.empty();
  }

}
