/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.message;

import static org.mule.runtime.api.message.NullAttributes.NULL_ATTRIBUTES;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.streaming.CursorStreamProvider;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Utility methods for handling {@link Message messages}
 *
 * @since 4.0
 */
public final class MessageUtils {


  private MessageUtils() {}

  /**
   * Transforms the given {@code result} into a {@link Message}
   *
   * @param result a {@link Result} object
   * @return a {@link Message}
   */
  public static Message toMessage(Result result) {
    return toMessage(result, (MediaType) result.getMediaType().orElse(ANY));
  }

  /**
   * Transforms the given {@code result} into a {@link Message}
   *
   * @param result    a {@link Result} object
   * @param mediaType the {@link MediaType} for the message payload
   * @return a {@link Message}
   */
  public static Message toMessage(Result result, MediaType mediaType) {
    return toMessage(result, mediaType, null, null);
  }

  public static Message toMessage(Result result,
                                  MediaType mediaType,
                                  CursorStreamProviderFactory cursorStreamProviderFactory,
                                  Event event) {
    return Message.builder()
        .payload(valueOrStreamProvider(result.getOutput(), cursorStreamProviderFactory, event).getValue().orElse(null))
        .mediaType(mediaType)
        .attributes((Attributes) result.getAttributes().orElse(NULL_ATTRIBUTES))
        .build();
  }

  public static <T> Either<CursorStreamProvider, T> valueOrStreamProvider(T value,
                                                                          CursorStreamProviderFactory cursorStreamProviderFactory,
                                                                          Event event) {
    if (cursorStreamProviderFactory != null && value instanceof InputStream) {
      return (Either<CursorStreamProvider, T>) cursorStreamProviderFactory.of(event, (InputStream) value);
    } else {
      return Either.right(value);
    }
  }

  /**
   * Transforms the given {@code results} into a similar collection of {@link Message}
   * objects
   *
   * @param results   a collection of {@link Result} items
   * @param mediaType the {@link MediaType} of the generated {@link Message} instances
   * @return a similar collection of {@link Message}
   */
  public static Collection<Message> toMessageCollection(Collection<Result> results,
                                                        MediaType mediaType,
                                                        CursorStreamProviderFactory cursorStreamProviderFactory,
                                                        Event event) {
    if (results instanceof List) {
      return new ResultsToMessageList((List<Result>) results, mediaType, cursorStreamProviderFactory, event);
    } else if (results instanceof Set) {
      return new ResultsToMessageSet((Set<Result>) results, mediaType, cursorStreamProviderFactory, event);
    } else {
      return new ResultsToMessageCollection(results, mediaType, cursorStreamProviderFactory, event);
    }
  }
}
