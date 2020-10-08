/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.message;

import static org.mule.runtime.api.metadata.DataType.builder;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.core.api.util.StreamingUtils.streamingContent;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.management.stats.CursorComponentDecoratorFactory;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.iterator.StreamingIterator;
import org.mule.runtime.core.internal.management.stats.InputDecoratedCursorStreamProvider;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.extension.api.runtime.operation.Result;

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
   * @param result a {@link Result} object
   * @param mediaType the {@link MediaType} for the message payload, overrides the described in the {@code result}
   * @return a {@link Message}
   */
  public static Message toMessage(Result result, MediaType mediaType) {
    return toMessage(result, mediaType, null, (BaseEventContext) null, null);
  }

  /**
   * Transforms the given {@code result} into a {@link Message}.
   *
   * @param result a {@link Result} object
   * @param cursorProviderFactory Factory that in case of finding a value which can create a cursor (eg.: {@link InputStream} or
   *        {@link Iterator}), will create a {@link CursorProvider}
   * @param eventContext Used for the case where a {@link CursorProvider} is created, register the one in it.
   * @return a {@link Message}
   */
  public static Message toMessage(Result result, CursorProviderFactory cursorProviderFactory, BaseEventContext eventContext,
                                  ComponentLocation originatingLocation) {
    return toMessage(result, ((Optional<MediaType>) result.getMediaType()).orElse(ANY), cursorProviderFactory, eventContext,
                     originatingLocation);
  }

  /**
   * Transforms the given {@code result} into a {@link Message}.
   *
   * @param result a {@link Result} object
   * @param cursorProviderFactory Factory that in case of finding a value which can create a cursor (eg.: {@link InputStream} or
   *        {@link Iterator}), will create a {@link CursorProvider}
   * @param event Used for the case where a {@link CursorProvider} is created, register the one in it.
   * @return a {@link Message}
   */
  public static Message toMessage(Result result, CursorProviderFactory cursorProviderFactory, CoreEvent event,
                                  ComponentLocation originatingLocation) {
    return toMessage(result, cursorProviderFactory, ((BaseEventContext) event.getContext()).getRootContext(),
                     originatingLocation);
  }

  /**
   * Transforms the given {@code result} into a {@link Message}.
   *
   * @param result a {@link Result} object
   * @param mediaType the {@link MediaType} for the message payload, overrides the described in the {@code result}
   * @param cursorProviderFactory Factory that in case of finding a value which can create a cursor (eg.: {@link InputStream} or
   *        {@link Iterator}), will create a {@link CursorProvider}
   * @param eventContext Used for the case where a {@link CursorProvider} is created, register the one in it.
   *
   * @return a {@link Message}
   */
  public static Message toMessage(Result<?, ?> result,
                                  MediaType mediaType,
                                  CursorComponentDecoratorFactory componentDecoratorFactory,
                                  CursorProviderFactory cursorProviderFactory,
                                  BaseEventContext eventContext,
                                  ComponentLocation originatingLocation,
                                  String correlationId) {
    final Object output;
    if (result.getOutput() instanceof InputStream) {
      output = componentDecoratorFactory.decorateOutput((InputStream) result.getOutput(), correlationId);
    } else if (result.getOutput() instanceof Collection) {
      output = componentDecoratorFactory.decorateOutputCollection((Collection) result.getOutput(), correlationId);
    } else if (result.getOutput() instanceof Iterator) {
      output = componentDecoratorFactory.decorateOutput((Iterator) result.getOutput(), correlationId);
    } else {
      output = result.getOutput();
    }

    Object value = streamingContent(output, cursorProviderFactory, eventContext, originatingLocation);
    return toMessage(result, builder().fromObject(value).mediaType(mediaType).build(), value);
  }

  /**
   * Transforms the given {@code result} into a {@link Message}.
   *
   * @param result a {@link Result} object
   * @param mediaType the {@link MediaType} for the message payload, overrides the described in the {@code result}
   * @param cursorProviderFactory Factory that in case of finding a value which can create a cursor (eg.: {@link InputStream} or
   *        {@link Iterator}), will create a {@link CursorProvider}
   * @param eventContext Used for the case where a {@link CursorProvider} is created, register the one in it.
   *
   * @return a {@link Message}
   */
  public static Message toMessage(Result<?, ?> result,
                                  MediaType mediaType,
                                  CursorProviderFactory cursorProviderFactory,
                                  BaseEventContext eventContext,
                                  ComponentLocation originatingLocation) {
    Object value = streamingContent(result.getOutput(), cursorProviderFactory, eventContext, originatingLocation);
    return toMessage(result, builder().fromObject(value).mediaType(mediaType).build(), value);
  }

  /**
   * Transforms the given {@code result} into a {@link Message}.
   *
   * @param result a {@link Result} object
   * @param mediaType the {@link MediaType} for the message payload, overrides the described in the {@code result}
   * @param cursorProviderFactory Factory that in case of finding a value which can create a cursor (eg.: {@link InputStream} or
   *        {@link Iterator}), will create a {@link CursorProvider}
   * @param eventContext Used for the case where a {@link CursorProvider} is created, register the one in it.
   *
   * @return a {@link Message}
   */
  public static Message toMessage(Result<?, ?> result,
                                  MediaType mediaType,
                                  CursorProviderFactory cursorProviderFactory,
                                  BaseEventContext eventContext,
                                  DataType dataType,
                                  ComponentLocation originatingLocation) {
    Object value = streamingContent(result.getOutput(), cursorProviderFactory, eventContext, originatingLocation);
    return toMessage(result, builder(dataType).mediaType(mediaType).build(), value);
  }


  /**
   * Transforms the given {@code result} into a {@link Message}.
   *
   * @param result a {@link Result} object
   * @param mediaType the {@link MediaType} for the message payload, overrides the described in the {@code result}
   * @param cursorProviderFactory Factory that in case of finding a value which can create a cursor (eg.: {@link InputStream} or
   *        {@link Iterator}), will create a {@link CursorProvider}
   * @param event Used for the case where a {@link CursorProvider} is created, register the one in it.
   *
   * @return a {@link Message}
   */
  public static Message toMessage(Result<?, ?> result,
                                  MediaType mediaType,
                                  CursorProviderFactory cursorProviderFactory,
                                  CoreEvent event,
                                  ComponentLocation originatingLocation) {
    return toMessage(result, mediaType, cursorProviderFactory, ((BaseEventContext) event.getContext()).getRootContext(),
                     originatingLocation);
  }

  /**
   * Transforms the given {@code result} into a {@link Message}.
   *
   * @param result a {@link Result} object
   * @param mediaType the {@link MediaType} for the message payload, overrides the described in the {@code result}
   * @param cursorProviderFactory Factory that in case of finding a value which can create a cursor (eg.: {@link InputStream} or
   *        {@link Iterator}), will create a {@link CursorProvider}
   * @param event Used for the case where a {@link CursorProvider} is created, register the one in it.
   *
   * @return a {@link Message}
   */
  public static Message toMessage(Result<?, ?> result,
                                  MediaType mediaType,
                                  CursorProviderFactory cursorProviderFactory,
                                  CoreEvent event,
                                  DataType dataType,
                                  ComponentLocation originatinLocation) {
    return toMessage(result, mediaType, cursorProviderFactory, ((BaseEventContext) event.getContext()).getRootContext(),
                     dataType, originatinLocation);
  }

  /**
   * Transforms the given {@code results} into a list of {@link Message} objects
   *
   * @param results a collection of {@link Result} items
   * @param cursorProviderFactory the {@link CursorProviderFactory} used to handle streaming cursors
   * @param eventContext the toot context of the {@link CoreEvent} which originated the results being transformed
   * @return a {@link List} of {@link Message}
   */
  public static List<Message> toMessageCollection(Collection<Result> results,
                                                  CursorProviderFactory cursorProviderFactory,
                                                  BaseEventContext eventContext, ComponentLocation originatingLocation) {
    if (!(results instanceof List)) {
      results = new ArrayList<>(results);
    }

    return new ResultsToMessageList((List) results, cursorProviderFactory, eventContext, originatingLocation);
  }

  /**
   * Transforms the given {@code results} into a similar collection of {@link Message} objects
   *
   * @param results a collection of {@link Result} items
   * @param cursorProviderFactory the {@link CursorProviderFactory} used to handle streaming cursors
   * @param eventContext the root context of the {@link CoreEvent} which originated the results being transformed
   * @return a similar collection of {@link Message}
   */
  public static Iterator<Message> toMessageIterator(Iterator<Result> results,
                                                    CursorProviderFactory cursorProviderFactory,
                                                    BaseEventContext eventContext, ComponentLocation originatingLocation) {
    if (results instanceof StreamingIterator) {
      return new ResultToMessageStreamingIterator((StreamingIterator<Result>) results, cursorProviderFactory, eventContext,
                                                  originatingLocation);
    } else {
      return new ResultToMessageIterator((Iterator) results, cursorProviderFactory, eventContext, originatingLocation);
    }
  }

  private static Message toMessage(Result<?, ?> result, DataType dataType, Object value) {
    Message.Builder builder = Message.builder().payload(new TypedValue<>(value, dataType, result.getByteLength()));

    if (result.getAttributes().isPresent()) {
      // Don't change: SonarQube detects this code as java:S3655 bug, but by using Optional#ifPresent(Consumer) introduces
      // performance issues.
      Object att = result.getAttributes().get();

      final Optional<MediaType> attributesMediaType = result.getAttributesMediaType();
      builder.attributes(new TypedValue<>(att, attributesMediaType.isPresent()
          ? builder().type(att.getClass()).mediaType(attributesMediaType.get()).build()
          : DataType.fromObject(att),
                                          OptionalLong.empty()));
    }

    return builder.build();
  }

  /**
   * Decorates input value.
   * 
   * @param v value to be decorated
   * @param eventCorrelationId the correlationId of the context involved
   * @param componentDecoratorFactory the component decorator factory
   * 
   * @return decorated value
   */
  public static Object decorateInput(Object v, String eventCorrelationId,
                                     CursorComponentDecoratorFactory componentDecoratorFactory) {
    if (v instanceof InputStream) {
      return componentDecoratorFactory.decorateInput((InputStream) v, eventCorrelationId);
    } else if (v instanceof Collection) {
      return componentDecoratorFactory.decorateInput((Collection) v, eventCorrelationId);
    } else if (v instanceof Iterator) {
      return componentDecoratorFactory.decorateInput((Iterator) v, eventCorrelationId);
    } else if (v instanceof CursorStreamProvider) {
      return new InputDecoratedCursorStreamProvider((CursorStreamProvider) v,
                                                    componentDecoratorFactory,
                                                    eventCorrelationId);
    } else {
      return v;
    }
  }
}
