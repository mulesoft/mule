/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.module.extension.api.runtime.privileged.EventedResult;
import org.mule.runtime.module.extension.api.runtime.privileged.EventedSdkResult;

import java.util.Optional;
import java.util.OptionalLong;

public class EventedSdkResultAdapter<T, A> extends EventedSdkResult<T, A> {

  private final EventedResult<T, A> delegate;

  public EventedSdkResultAdapter(EventedResult<T, A> delegate) {
    super(delegate.getEvent());
    this.delegate = delegate;
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
  public Optional<MediaType> getAttributesMediaType() {
    return delegate.getAttributesMediaType();
  }

  @Override
  public OptionalLong getByteLength() {
    return delegate.getByteLength();
  }
}
