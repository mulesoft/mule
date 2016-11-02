/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.Optional;

/**
 * Default implementation of {@link Result.Builder}
 *
 * @param <Output> the generic type of the output value
 * @param <A> the generic type of the message attributes
 * @since 4.0
 */
final class DefaultOperationResultBuilder<Output, A extends Attributes> implements Result.Builder<Output, A> {

  private final DefaultResult<Output, A> operationResult = new DefaultResult<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public Result.Builder<Output, A> output(Output output) {
    operationResult.output = output;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Result.Builder<Output, A> attributes(A attributes) {
    operationResult.attributes = ofNullable(attributes);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Result.Builder<Output, A> mediaType(MediaType dataType) {
    operationResult.mediaType = ofNullable(dataType);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Result<Output, A> build() {
    return operationResult;
  }

  private final class DefaultResult<Output, A extends Attributes> implements Result<Output, A> {

    private Output output;
    private Optional<A> attributes = empty();
    private Optional<MediaType> mediaType = empty();

    public Output getOutput() {
      return output;
    }

    public Optional<A> getAttributes() {
      return attributes;
    }

    @Override
    public Optional<MediaType> getMediaType() {
      return mediaType;
    }
  }
}
