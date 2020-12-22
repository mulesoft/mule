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
import org.mule.runtime.core.internal.util.mediatype.PayloadMediaTypeResolver;
import org.mule.sdk.api.runtime.operation.Result;

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
  private final MediaType mediaType;
  private final Optional<String> correlationId;
  private final PayloadMediaTypeResolver payloadMediaTypeResolver;

  /**
   * Creates a new instance
   *
   * @param result                the source result
   * @param cursorProviderFactory the {@link CursorStreamProviderFactory} used by the source
   * @param mediaType             the {@link MediaType} to set in the message
   * @param isCollection          whether the {@code result} represents a {@link List} of messages.
   * @param correlationId         the correlationId of the message to be set
   */
  @Deprecated
  public SourceResultAdapter(Result<?, ?> result,
                             CursorProviderFactory cursorProviderFactory,
                             MediaType mediaType,
                             boolean isCollection,
                             Optional<String> correlationId) {
    this(result, cursorProviderFactory, mediaType, isCollection, correlationId, null);
  }

  /**
   * Creates a new instance
   *
   * @param result the source result
   * @param cursorProviderFactory the {@link CursorStreamProviderFactory} used by the source
   * @param mediaType the {@link MediaType} to set in the message
   * @param isCollection whether the {@code result} represents a {@link List} of messages.
   * @param correlationId the correlationId of the message to be set
   * @param payloadMediaTypeResolver resolver used in case result is a {@link List} of results.
   */
  public SourceResultAdapter(Result<?, ?> result,
                             CursorProviderFactory cursorProviderFactory,
                             MediaType mediaType,
                             boolean isCollection,
                             Optional<String> correlationId,
                             PayloadMediaTypeResolver payloadMediaTypeResolver) {
    this.result = result;
    this.cursorProviderFactory = cursorProviderFactory;
    this.mediaType = mediaType;
    this.isCollection = isCollection;
    this.correlationId = correlationId;
    this.payloadMediaTypeResolver = payloadMediaTypeResolver;
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
    return mediaType;
  }

  /**
   * @since 4.2
   */
  public PayloadMediaTypeResolver getPayloadMediaTypeResolver() {
    return payloadMediaTypeResolver;
  }
}
