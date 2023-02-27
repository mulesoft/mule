/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static org.mule.runtime.tracer.api.context.getter.DistributedTraceContextGetter.emptyTraceContextMapGetter;

import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.core.internal.util.mediatype.PayloadMediaTypeResolver;
import org.mule.runtime.tracer.api.context.getter.DistributedTraceContextGetter;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;

import java.util.List;
import java.util.Map;
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
  private final Optional<PollItemInformation> itemInformation;
  private final DistributedTraceContextManager distributedTraceContextManager;
  private final String spanName;
  private final Map<String, String> spanRootAttributes;

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
    this(result, cursorProviderFactory, mediaType, isCollection, correlationId, null, empty());
  }

  /**
   * Creates a new instance
   *
   * @param result                   the source result
   * @param cursorProviderFactory    the {@link CursorStreamProviderFactory} used by the source
   * @param mediaType                the {@link MediaType} to set in the message
   * @param isCollection             whether the {@code result} represents a {@link List} of messages.
   * @param correlationId            the correlationId of the message to be set
   * @param payloadMediaTypeResolver resolver used in case result is a {@link List} of results.
   * @param pollItemInformation      additional information about the poll that originated the message
   */
  public SourceResultAdapter(Result<?, ?> result,
                             CursorProviderFactory cursorProviderFactory,
                             MediaType mediaType,
                             boolean isCollection,
                             Optional<String> correlationId,
                             PayloadMediaTypeResolver payloadMediaTypeResolver,
                             Optional<PollItemInformation> pollItemInformation) {
    this(result, cursorProviderFactory, mediaType, isCollection, correlationId, payloadMediaTypeResolver,
         null, null, emptyMap(), pollItemInformation);
  }

  public SourceResultAdapter(Result<?, ?> result,
                             CursorProviderFactory cursorProviderFactory,
                             MediaType mediaType,
                             boolean isCollection,
                             Optional<String> correlationId,
                             PayloadMediaTypeResolver payloadMediaTypeResolver,
                             DistributedTraceContextManager distributedTraceContextManager,
                             String spanName,
                             Map<String, String> spanAttributes,
                             Optional<PollItemInformation> pollItemInformation) {
    this.result = result;
    this.cursorProviderFactory = cursorProviderFactory;
    this.mediaType = mediaType;
    this.isCollection = isCollection;
    this.correlationId = correlationId;
    this.payloadMediaTypeResolver = payloadMediaTypeResolver;
    this.itemInformation = pollItemInformation;
    this.distributedTraceContextManager = distributedTraceContextManager;
    this.spanName = spanName;
    this.spanRootAttributes = spanAttributes;
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

  /**
   * @return if the current message originated from a polling source, this will return information about it.
   *
   * @since 4.5.0
   */
  public Optional<PollItemInformation> getPollItemInformation() {
    return itemInformation;
  }

  /**
   * @return the {@link DistributedTraceContextGetter} used to retrieve the distributed trace context.
   *
   * @since 4.5.0
   */
  public DistributedTraceContextManager getDistributedTraceContextGetter() {
    return distributedTraceContextManager;
  }

  /**
   * @return the root name. This will override the name of the span associated to the source.
   *
   * @since 4.5.0
   */
  public String getRootSpanName() {
    return spanName;
  }

  /**
   * @return the span root attributes. This will be added to the span associated to this source.
   *
   * @since 4.5.0
   */
  public Map<String, String> getSpanRootAttributes() {
    return spanRootAttributes;
  }
}
