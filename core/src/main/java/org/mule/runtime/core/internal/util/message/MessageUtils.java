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

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.internal.util.collection.TransformingIterator;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.sdk.api.runtime.operation.Result;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Function;

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
   * @since 4.4.0
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
   * @since 4.4.0
   */
  public static Message toMessage(Result result, MediaType mediaType) {
    return toMessage(result, mediaType, null, (BaseEventContext) null, null);
  }

  /**
   * Transforms the given {@code result} into a {@link Message}.
   *
   * @param result                a {@link Result} object
   * @param cursorProviderFactory Factory that in case of finding a value which can create a cursor (eg.: {@link InputStream} or
   *                              {@link Iterator}), will create a {@link CursorProvider}
   * @param eventContext          Used for the case where a {@link CursorProvider} is created, register the one in it.
   * @return a {@link Message}
   * @since 4.4.0
   */
  public static Message toMessage(Result result, CursorProviderFactory cursorProviderFactory, BaseEventContext eventContext,
                                  ComponentLocation originatingLocation) {
    return toMessage(result, ((Optional<MediaType>) result.getMediaType()).orElse(ANY), cursorProviderFactory, eventContext,
                     originatingLocation);
  }

  /**
   * Transforms the given {@code result} into a {@link Message}.
   *
   * @param result                a {@link Result} object
   * @param cursorProviderFactory Factory that in case of finding a value which can create a cursor (eg.: {@link InputStream} or
   *                              {@link Iterator}), will create a {@link CursorProvider}
   * @param event                 Used for the case where a {@link CursorProvider} is created, register the one in it.
   * @return a {@link Message}
   * @since 4.4.0
   */
  public static Message toMessage(Result result, CursorProviderFactory cursorProviderFactory, CoreEvent event,
                                  ComponentLocation originatingLocation) {
    return toMessage(result, cursorProviderFactory, ((BaseEventContext) event.getContext()).getRootContext(),
                     originatingLocation);
  }

  /**
   * Transforms the given {@code result} into a {@link Message}.
   *
   * @param result                a {@link Result} object
   * @param mediaType             the {@link MediaType} for the message payload, overrides the described in the {@code result}
   * @param cursorProviderFactory Factory that in case of finding a value which can create a cursor (eg.: {@link InputStream} or
   *                              {@link Iterator}), will create a {@link CursorProvider}
   * @param eventContext          Used for the case where a {@link CursorProvider} is created, register the one in it.
   *
   * @return a {@link Message}
   * @since 4.4.0
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
   * @param result                a {@link Result} object
   * @param mediaType             the {@link MediaType} for the message payload, overrides the described in the {@code result}
   * @param cursorProviderFactory Factory that in case of finding a value which can create a cursor (eg.: {@link InputStream} or
   *                              {@link Iterator}), will create a {@link CursorProvider}
   * @param eventContext          Used for the case where a {@link CursorProvider} is created, register the one in it.
   *
   * @return a {@link Message}
   * @since 4.4.0
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
   * @param result                a {@link Result} object
   * @param mediaType             the {@link MediaType} for the message payload, overrides the described in the {@code result}
   * @param cursorProviderFactory Factory that in case of finding a value which can create a cursor (eg.: {@link InputStream} or
   *                              {@link Iterator}), will create a {@link CursorProvider}
   * @param event                 Used for the case where a {@link CursorProvider} is created, register the one in it.
   *
   * @return a {@link Message}
   * @since 4.4.0
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
   * @param result                a {@link Result} object
   * @param mediaType             the {@link MediaType} for the message payload, overrides the described in the {@code result}
   * @param cursorProviderFactory Factory that in case of finding a value which can create a cursor (eg.: {@link InputStream} or
   *                              {@link Iterator}), will create a {@link CursorProvider}
   * @param event                 Used for the case where a {@link CursorProvider} is created, register the one in it.
   *
   * @return a {@link Message}
   * @since 4.4.0
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
   * @param results               a collection of {@link Result} items
   * @param cursorProviderFactory the {@link CursorProviderFactory} used to handle streaming cursors
   * @param eventContext          the toot context of the {@link CoreEvent} which originated the results being transformed
   * @return a {@link List} of {@link Message}
   * @since 4.4.0
   */
  public static List<Message> messageCollection(Collection<Result> results,
                                                CursorProviderFactory cursorProviderFactory,
                                                BaseEventContext eventContext, ComponentLocation originatingLocation) {
    if (!(results instanceof List)) {
      results = new ArrayList<>(results);
    }

    return new TransformingMessageList((List) results,
                                       resultTransformer(cursorProviderFactory, eventContext, originatingLocation));
  }

  /**
   * Transforms the given {@code results} into a similar collection of {@link Message} objects
   *
   * @param results               a collection of {@link Result} items
   * @param cursorProviderFactory the {@link CursorProviderFactory} used to handle streaming cursors
   * @param eventContext          the root context of the {@link CoreEvent} which originated the results being transformed
   * @return a similar collection of {@link Message}
   * @since 4.4.0
   */
  public static Iterator<Message> messageIterator(Iterator<Result> results,
                                                  CursorProviderFactory cursorProviderFactory,
                                                  BaseEventContext eventContext,
                                                  ComponentLocation originatingLocation) {

    return TransformingIterator.from(results, resultTransformer(cursorProviderFactory, eventContext, originatingLocation));
  }

  /**
   * Transforms the given {@code result} into a {@link Message}
   *
   * @param result a {@link Result} object
   * @return a {@link Message}
   * @deprecated since 4.4.0. Use {@link #toMessage(Result)} instead
   */
  @Deprecated
  public static Message toMessage(org.mule.runtime.extension.api.runtime.operation.Result result) {
    return toMessage(result, (MediaType) result.getMediaType().orElse(ANY));
  }

  /**
   * Transforms the given {@code result} into a {@link Message}
   *
   * @param result    a {@link Result} object
   * @param mediaType the {@link MediaType} for the message payload, overrides the described in the {@code result}
   * @return a {@link Message}
   * @deprecated since 4.4.0. Use {@link #toMessage(Result, MediaType)} instead
   */
  @Deprecated
  public static Message toMessage(org.mule.runtime.extension.api.runtime.operation.Result result,
                                  MediaType mediaType) {
    return toMessage(result, mediaType, null, (BaseEventContext) null, null);
  }

  /**
   * Transforms the given {@code result} into a {@link Message}.
   *
   * @param result                a {@link Result} object
   * @param cursorProviderFactory Factory that in case of finding a value which can create a cursor (eg.: {@link InputStream} or
   *                              {@link Iterator}), will create a {@link CursorProvider}
   * @param eventContext          Used for the case where a {@link CursorProvider} is created, register the one in it.
   * @return a {@link Message}
   * @deprecated since 4.4.0. Use {@link #toMessage(Result, CursorProviderFactory, BaseEventContext, ComponentLocation)} instead
   */
  @Deprecated
  public static Message toMessage(org.mule.runtime.extension.api.runtime.operation.Result result,
                                  CursorProviderFactory cursorProviderFactory,
                                  BaseEventContext eventContext,
                                  ComponentLocation originatingLocation) {
    return toMessage(result, ((Optional<MediaType>) result.getMediaType()).orElse(ANY), cursorProviderFactory, eventContext,
                     originatingLocation);
  }

  /**
   * Transforms the given {@code result} into a {@link Message}.
   *
   * @param result                a {@link Result} object
   * @param cursorProviderFactory Factory that in case of finding a value which can create a cursor (eg.: {@link InputStream} or
   *                              {@link Iterator}), will create a {@link CursorProvider}
   * @param event                 Used for the case where a {@link CursorProvider} is created, register the one in it.
   * @return a {@link Message}
   * @deprecated since 4.4.0. Use {@link #toMessage(Result, CursorProviderFactory, CoreEvent, ComponentLocation)} instead
   */
  @Deprecated
  public static Message toMessage(org.mule.runtime.extension.api.runtime.operation.Result result,
                                  CursorProviderFactory cursorProviderFactory,
                                  CoreEvent event,
                                  ComponentLocation originatingLocation) {
    return toMessage(result, cursorProviderFactory, ((BaseEventContext) event.getContext()).getRootContext(),
                     originatingLocation);
  }

  /**
   * Transforms the given {@code result} into a {@link Message}.
   *
   * @param result                a {@link Result} object
   * @param mediaType             the {@link MediaType} for the message payload, overrides the described in the {@code result}
   * @param cursorProviderFactory Factory that in case of finding a value which can create a cursor (eg.: {@link InputStream} or
   *                              {@link Iterator}), will create a {@link CursorProvider}
   * @param eventContext          Used for the case where a {@link CursorProvider} is created, register the one in it.
   *
   * @return a {@link Message}
   * @deprecated since 4.4.0. Use
   *             {@link #toMessage(Result, MediaType, CursorProviderFactory, BaseEventContext, ComponentLocation)} instead
   */
  @Deprecated
  public static Message toMessage(org.mule.runtime.extension.api.runtime.operation.Result<?, ?> result,
                                  MediaType mediaType,
                                  CursorProviderFactory cursorProviderFactory,
                                  BaseEventContext eventContext,
                                  ComponentLocation originatingLocation) {
    Object value = streamingContent(result.getOutput(), cursorProviderFactory, eventContext, originatingLocation);
    return toMessage(new SdkResultAdapter<>(result), builder().fromObject(value).mediaType(mediaType).build(), value);
  }

  /**
   * Transforms the given {@code result} into a {@link Message}.
   *
   * @param result                a {@link Result} object
   * @param mediaType             the {@link MediaType} for the message payload, overrides the described in the {@code result}
   * @param cursorProviderFactory Factory that in case of finding a value which can create a cursor (eg.: {@link InputStream} or
   *                              {@link Iterator}), will create a {@link CursorProvider}
   * @param eventContext          Used for the case where a {@link CursorProvider} is created, register the one in it.
   *
   * @return a {@link Message}
   * @deprecated since 4.4.0. Use
   *             {@link #toMessage(Result, MediaType, CursorProviderFactory, BaseEventContext, DataType, ComponentLocation)}
   *             instead
   */
  @Deprecated
  public static Message toMessage(org.mule.runtime.extension.api.runtime.operation.Result<?, ?> result,
                                  MediaType mediaType,
                                  CursorProviderFactory cursorProviderFactory,
                                  BaseEventContext eventContext,
                                  DataType dataType,
                                  ComponentLocation originatingLocation) {
    Object value = streamingContent(result.getOutput(), cursorProviderFactory, eventContext, originatingLocation);
    return toMessage(new SdkResultAdapter<>(result), builder(dataType).mediaType(mediaType).build(), value);
  }


  /**
   * Transforms the given {@code result} into a {@link Message}.
   *
   * @param result                a {@link Result} object
   * @param mediaType             the {@link MediaType} for the message payload, overrides the described in the {@code result}
   * @param cursorProviderFactory Factory that in case of finding a value which can create a cursor (eg.: {@link InputStream} or
   *                              {@link Iterator}), will create a {@link CursorProvider}
   * @param event                 Used for the case where a {@link CursorProvider} is created, register the one in it.
   *
   * @return a {@link Message}
   * @deprecated since 4.4.0. Use {@link #toMessage(Result, MediaType, CursorProviderFactory, CoreEvent, ComponentLocation)}
   *             instead
   */
  @Deprecated
  public static Message toMessage(org.mule.runtime.extension.api.runtime.operation.Result<?, ?> result,
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
   * @param result                a {@link Result} object
   * @param mediaType             the {@link MediaType} for the message payload, overrides the described in the {@code result}
   * @param cursorProviderFactory Factory that in case of finding a value which can create a cursor (eg.: {@link InputStream} or
   *                              {@link Iterator}), will create a {@link CursorProvider}
   * @param event                 Used for the case where a {@link CursorProvider} is created, register the one in it.
   *
   * @return a {@link Message}
   * @deprecated since 4.4.0. Use
   *             {@link #toMessage(Result, MediaType, CursorProviderFactory, CoreEvent, DataType, ComponentLocation)} instead
   */
  @Deprecated
  public static Message toMessage(org.mule.runtime.extension.api.runtime.operation.Result<?, ?> result,
                                  MediaType mediaType,
                                  CursorProviderFactory cursorProviderFactory,
                                  CoreEvent event,
                                  DataType dataType,
                                  ComponentLocation originatingLocation) {
    return toMessage(result, mediaType, cursorProviderFactory, ((BaseEventContext) event.getContext()).getRootContext(),
                     dataType, originatingLocation);
  }

  /**
   * Transforms the given {@code results} into a list of {@link Message} objects
   *
   * @param results               a collection of {@link Result} items
   * @param cursorProviderFactory the {@link CursorProviderFactory} used to handle streaming cursors
   * @param eventContext          the toot context of the {@link CoreEvent} which originated the results being transformed
   * @return a {@link List} of {@link Message}
   * @deprecated since 4.4.0. Use
   *             {@link #messageCollection(Collection, CursorProviderFactory, BaseEventContext, ComponentLocation)}
   */
  public static List<Message> toMessageCollection(Collection<org.mule.runtime.extension.api.runtime.operation.Result> results,
                                                  CursorProviderFactory cursorProviderFactory,
                                                  BaseEventContext eventContext,
                                                  ComponentLocation originatingLocation) {
    if (!(results instanceof List)) {
      results = new ArrayList<>(results);
    }

    return new TransformingMessageList((List) results,
                                       legacyResultTransformer(cursorProviderFactory, eventContext, originatingLocation));
  }

  /**
   * Returns a {@link Function} which receives a value expected to be a legacy
   * {@link org.mule.runtime.extension.api.runtime.operation.Result} and transforms it to a {@link Message}
   *
   * @param cursorProviderFactory a {@link CursorProviderFactory} in case the value is streaming
   * @param eventContext          the current {@link EventContext}
   * @param originatingLocation   the location of the component that generated the {@link Result}
   * @return a {@link Function}
   * @since 4.4.0
   */
  private static Function<Object, Message> legacyResultTransformer(CursorProviderFactory cursorProviderFactory,
                                                                   BaseEventContext eventContext,
                                                                   ComponentLocation originatingLocation) {
    return value -> toMessage((org.mule.runtime.extension.api.runtime.operation.Result) value,
                              cursorProviderFactory, eventContext, originatingLocation);
  }

  /**
   * Returns a {@link Function} which receives a value expected to be {@link Result} and transforms it to a {@link Message}
   *
   * @param cursorProviderFactory a {@link CursorProviderFactory} in case the value is streaming
   * @param eventContext          the current {@link EventContext}
   * @param originatingLocation   the location of the component that generated the {@link Result}
   * @return a {@link Function}
   * @since 4.4.0
   */
  private static Function<Object, Message> resultTransformer(CursorProviderFactory cursorProviderFactory,
                                                             BaseEventContext eventContext,
                                                             ComponentLocation originatingLocation) {

    return value -> toMessage(SdkResultAdapter.from(value), cursorProviderFactory, eventContext, originatingLocation);
  }

  /**
   * Transforms the given {@code results} into a similar collection of {@link Message} objects
   *
   * @param results               a collection of {@link Result} items
   * @param cursorProviderFactory the {@link CursorProviderFactory} used to handle streaming cursors
   * @param eventContext          the root context of the {@link CoreEvent} which originated the results being transformed
   * @return a similar collection of {@link Message}
   * @deprecated since 4.4.0. Use {@link #messageIterator(Iterator, CursorProviderFactory, BaseEventContext, ComponentLocation)}
   *             instead
   */
  @Deprecated
  public static Iterator<Message> toMessageIterator(Iterator<org.mule.runtime.extension.api.runtime.operation.Result> results,
                                                    CursorProviderFactory cursorProviderFactory,
                                                    BaseEventContext eventContext,
                                                    ComponentLocation originatingLocation) {

    return TransformingIterator.from(results, legacyResultTransformer(cursorProviderFactory, eventContext, originatingLocation));
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
}
