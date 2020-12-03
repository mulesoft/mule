package org.mule.runtime.module.extension.internal.runtime.source.legacy;

import org.mule.runtime.api.metadata.MediaType;
import org.mule.sdk.api.runtime.operation.Result;

import java.util.Optional;
import java.util.OptionalLong;

public class LegacySdkResultAdapter<T, A> extends org.mule.runtime.extension.api.runtime.operation.Result<T, A> {

  private final org.mule.runtime.extension.api.runtime.operation.Result<T, A> delegate;

  public static <T, A> org.mule.runtime.extension.api.runtime.operation.Result<T, A> from(Object value) {
    if (value instanceof org.mule.runtime.extension.api.runtime.operation.Result) {
      return (org.mule.runtime.extension.api.runtime.operation.Result<T, A>) value;
    } else if (value instanceof Result) {
      return new LegacySdkResultAdapter((org.mule.runtime.extension.api.runtime.operation.Result<T, A>) value);
    } else {
      throw new IllegalArgumentException("Unsupported type: " + value.getClass());
    }
  }

  public LegacySdkResultAdapter(org.mule.runtime.extension.api.runtime.operation.Result<T, A> delegate) {
    this.delegate = delegate;
  }

  @Override
  public Builder<T, A> copy() {
    final Builder<T, A> builder = org.mule.runtime.extension.api.runtime.operation.Result.<T, A>builder()
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
