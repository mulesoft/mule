/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.message;

import static org.mule.runtime.api.message.NullAttributes.NULL_ATTRIBUTES;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.util.StreamingUtils;
import org.mule.runtime.core.internal.streaming.object.iterator.StreamingIterator;
import org.mule.runtime.core.streaming.CursorProviderFactory;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
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
   * @param mediaType the {@link MediaType} for the message payload, overrides the described in the {@code result}
   * @return a {@link Message}
   */
  public static Message toMessage(Result result, MediaType mediaType) {
    return toMessage(result, mediaType, null, null);
  }

  /**
   * Transforms the given {@code result} into a {@link Message}.
   *
   * @param result                a {@link Result} object
   * @param cursorProviderFactory Factory that in case of finding a value which can create a cursor (eg.: {@link InputStream}
   *                              or {@link Iterator}), will create a {@link CursorProvider}
   * @param event                 Used for the case where a {@link CursorProvider} is created, register the one in it.
   * @return a {@link Message}
   */
  public static Message toMessage(Result result, CursorProviderFactory cursorProviderFactory, Event event) {
    return toMessage(result, ((Optional<MediaType>) result.getMediaType()).orElse(ANY), cursorProviderFactory, event);
  }

  /**
   * Transforms the given {@code result} into a {@link Message}.
   *
   * @param result                a {@link Result} object
   * @param mediaType             the {@link MediaType} for the message payload, overrides the described in the {@code result}
   * @param cursorProviderFactory Factory that in case of finding a value which can create a cursor (eg.: {@link InputStream}
   *                              or {@link Iterator}), will create a {@link CursorProvider}
   * @param event                 Used for the case where a {@link CursorProvider} is created, register the one in it.
   *
   * @return a {@link Message}
   */
  public static Message toMessage(Result result,
                                  MediaType mediaType,
                                  CursorProviderFactory cursorProviderFactory,
                                  Event event) {
    return Message.builder()
        .payload(StreamingUtils.streamingContent(result.getOutput(), cursorProviderFactory, event))
        .mediaType(mediaType)
        .attributes(result.getAttributes().orElse(NULL_ATTRIBUTES))
        .build();
  }

  /**
   * Transforms the given {@code results} into a similar collection of {@link Message}
   * objects
   *
   * @param results               a collection of {@link Result} items
   * @param cursorProviderFactory the {@link CursorProviderFactory} used to handle streaming cursors
   * @param event                 the {@link Event} which originated the results being transformed
   * @return a similar collection of {@link Message}
   */
  public static Collection<Message> toMessageCollection(Collection<Result> results,
                                                        CursorProviderFactory cursorProviderFactory,
                                                        Event event) {
    if (results instanceof List) {
      return new ResultsToMessageList((List<Result>) results, cursorProviderFactory, event);
    } else if (results instanceof Set) {
      return new ResultsToMessageSet((Set<Result>) results, cursorProviderFactory, event);
    } else {
      return new ResultsToMessageCollection(results, cursorProviderFactory, event);
    }
  }

  /**
   * Transforms the given {@code results} into a similar collection of {@link Message} objects
   *
   * @param results a collection of {@link Result} items
   * @param cursorProviderFactory the {@link CursorProviderFactory} used to handle streaming cursors
   * @param event the {@link Event} which originated the results being transformed
   * @return a similar collection of {@link Message}
   */
  public static Iterator<Message> toMessageIterator(Iterator<Result> results,
                                                    CursorProviderFactory cursorProviderFactory,
                                                    Event event) {
    if (results instanceof StreamingIterator) {
      return new ResultToMessageStreamingIterator((StreamingIterator<Result>) results, cursorProviderFactory, event);
    } else {
      return new ResultToMessageIterator(results, cursorProviderFactory, event);
    }
  }
}
