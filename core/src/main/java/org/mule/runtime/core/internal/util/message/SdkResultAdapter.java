/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.message;


import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * Adapts a legacy {@link Result} into a {@link org.mule.sdk.api.runtime.operation.Result}
 *
 * @param <T> the generic type of the output value
 * @param <A> the generic type of the message attributes
 * @since 4.4.0
 */
public class SdkResultAdapter<T, A> extends org.mule.sdk.api.runtime.operation.Result<T, A> {

  private final Result<T, A> delegate;

  public static <T, A> org.mule.sdk.api.runtime.operation.Result<T, A> from(Object value) {
    if (value instanceof org.mule.sdk.api.runtime.operation.Result) {
      return (org.mule.sdk.api.runtime.operation.Result<T, A>) value;
    } else if (value instanceof Result) {
      return new SdkResultAdapter<>((Result) value);
    } else {
      throw new IllegalArgumentException("Unsupported type: " + value.getClass());
    }
  }

  public SdkResultAdapter(Result<T, A> delegate) {
    this.delegate = delegate;
  }

  @Override
  public Builder<T, A> copy() {
    final Builder<T, A> builder = org.mule.sdk.api.runtime.operation.Result.<T, A>builder()
        .output(getOutput())
        .attributes(getAttributes().orElse(null))
        .mediaType(getMediaType().orElse(null))
        .attributesMediaType(getAttributesMediaType().orElse(null))
        .attributesMediaType(getAttributesMediaType().orElse(null));
    getByteLength().ifPresent(builder::length);
    return builder;
  }

  @Override
  public T getOutput() {
    return delegate.getOutput();
  }

  @Override
  public Optional<A> getAttributes() {
    return delegate.getAttributes();
  }

  @Override
  public Optional<MediaType> getMediaType() {
    return delegate.getMediaType();
  }

  @Override
  public OptionalLong getByteLength() {
    return delegate.getByteLength();
  }

  @Override
  public Optional<MediaType> getAttributesMediaType() {
    return delegate.getAttributesMediaType();
  }
}
