/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.List;
import java.util.Optional;

/**
 * Contains the output of a message source
 *
 * @since 4.0
 */
public class SourceResultAdapter {

  private final Result<?, ?> result;
  private final CursorProviderFactory cursorProviderFactory;
  private final boolean isCollection;
  private final MediaType defaultMediaType;
  private final Optional<String> correlationId;

  /**
   * Creates a new instance
   *
   * @param result                the source result
   * @param cursorProviderFactory the {@link CursorStreamProviderFactory} used by the source
   * @param defaultMediaType      the {@link MediaType} to set in the message if the {@code result} doesn't specify any
   * @param isCollection          whether the {@code result} represents a {@link List} of messages.
   * @param correlationId         the correlationId of the message to be set
   */
  public SourceResultAdapter(Result<?, ?> result,
                             CursorProviderFactory cursorProviderFactory,
                             MediaType defaultMediaType,
                             boolean isCollection,
                             Optional<String> correlationId) {
    this.result = result;
    this.cursorProviderFactory = cursorProviderFactory;
    this.defaultMediaType = defaultMediaType;
    this.isCollection = isCollection;
    this.correlationId = correlationId;
  }

  /**
   * @return The source {@link Result}
   */
  public Result getResult() {
    return result;
  }

  /**
   * @return The {@link CursorStreamProviderFactory} used by the source
   */
  public CursorProviderFactory getCursorProviderFactory() {
    return cursorProviderFactory;
  }

  /**
   * @return Whether the {@link #getResult()} represents a {@link List} of messages.
   */
  public boolean isCollection() {
    return isCollection;
  }

  /**
   * @return Optionally return a correlationId
   * @since 4.1
   */
  public Optional<String> getCorrelationId() {
    return correlationId;
  }

  public MediaType getMediaType() {
    return result.getMediaType().orElse(defaultMediaType);
  }
}
