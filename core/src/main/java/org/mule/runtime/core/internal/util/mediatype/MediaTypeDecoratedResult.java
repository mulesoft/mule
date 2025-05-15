/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.mediatype;

import org.mule.runtime.api.metadata.MediaType;
import org.mule.sdk.api.runtime.operation.Result;

import java.util.Optional;
import java.util.OptionalLong;

import static java.util.Optional.of;

/**
 * Decorates an existing {@link Result} with a {@link MediaType}
 *
 * @since 4.2
 */
public class MediaTypeDecoratedResult extends Result {

  private Result delegate;
  private MediaType mediaType;

  /**
   * Creates a new instance
   *
   * @param delegate  a {@link Result} whose {@link MediaType} will be decorated
   * @param mediaType {@link MediaType} to be used in the {@link Result}
   */
  public MediaTypeDecoratedResult(Result delegate, MediaType mediaType) {
    this.delegate = delegate;
    this.mediaType = mediaType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Builder copy() {
    return delegate.copy().mediaType(mediaType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getOutput() {
    return delegate.getOutput();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional getAttributes() {
    return delegate.getAttributes();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MediaType> getMediaType() {
    return of(mediaType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OptionalLong getByteLength() {
    return delegate.getByteLength();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MediaType> getAttributesMediaType() {
    return delegate.getAttributesMediaType();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof MediaTypeDecoratedResult) {
      MediaTypeDecoratedResult other = (MediaTypeDecoratedResult) obj;
      return delegate.equals(other.delegate) && mediaType.equals(other.mediaType);
    }
    return delegate.equals(obj);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }
}
