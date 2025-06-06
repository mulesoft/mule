/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import static org.mule.runtime.core.api.rx.Exceptions.rxExceptionToMuleException;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.core.api.util.IOUtils.toByteArray;
import static org.mule.runtime.core.internal.event.EventUtils.getRoot;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ConnectableComponentModel;
import org.mule.runtime.api.metadata.CollectionDataType;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.core.internal.streaming.bytes.ByteArrayCursorStreamProvider;
import org.mule.runtime.core.internal.streaming.object.ListCursorIteratorProvider;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 * Utilities for handling {@link Cursor} instances
 *
 * @since 4.0
 */
public final class StreamingUtils {

  /**
   * Executes the given function {@code f} considering that the given {@code event} might have a {@link CursorProvider} as
   * payload. In that case, this method obtains a cursor from the provider and executes the function.
   * <p>
   * Closing the opened cursor, handling exceptions and return values are all taken care of by this utility method.
   *
   * @param event an {@link CoreEvent}
   * @param f     the function to execute
   * @return the output {@link CoreEvent}
   * @throws MuleException
   */
  public static CoreEvent withCursoredEvent(CoreEvent event, CheckedFunction<CoreEvent, CoreEvent> f)
      throws MuleException {
    if (event.getMessage().getPayload() == null) {
      return event;
    }
    Reference<Throwable> exception = new Reference<>();
    CheckedFunction<CoreEvent, CoreEvent> function = new CheckedFunction<CoreEvent, CoreEvent>() {

      @Override
      public CoreEvent applyChecked(CoreEvent event) throws Throwable {
        return f.apply(event);
      }

      @Override
      public CoreEvent handleException(Throwable throwable) {
        exception.set(unwrap(throwable));
        return null;
      }
    };

    Object payload = event.getMessage().getPayload().getValue();
    CursorProvider cursorProvider = null;
    Cursor cursor = null;
    try {
      if (payload instanceof CursorProvider) {
        cursorProvider = (CursorProvider) payload;
        cursor = cursorProvider.openCursor();
        event = replacePayload(event, cursor);
      }

      CoreEvent value = function.apply(event);

      if (value == null) {
        handlePossibleException(exception);
      } else if (value.getMessage().getPayload().getValue() == cursor) {
        value = replacePayload(value, cursorProvider);
      }

      return value;
    } finally {
      if (cursor != null) {
        closeQuietly(cursor);
      }
    }
  }

  /**
   * If the {@code cursorProviderFactory} accepts the given {@code value}, then the result of invoking
   * {@link CursorProviderFactory#of(EventContext, Object, ComponentLocation)} is returned. Otherwise, the original {@code value}
   * is.
   *
   * @param value                 a value which may be a repeatable streaming resource
   * @param cursorProviderFactory a nullable {@link CursorStreamProviderFactory}
   * @param eventContext          the root context of the event on which the {@code value} was generated
   * @param originatingLocation   the {@link ComponentLocation} where the cursor was created
   * @return the {@code value} or a {@link CursorProvider}
   *
   * @since 4.4.0
   */
  public static Object streamingContent(Object value, CursorProviderFactory cursorProviderFactory,
                                        EventContext eventContext, ComponentLocation originatingLocation) {
    if (cursorProviderFactory != null && cursorProviderFactory.accepts(value)) {
      return cursorProviderFactory.of(eventContext, value, originatingLocation);
    } else {
      return value;
    }
  }

  /**
   * If the {@code cursorProviderFactory} accepts the given {@code value}, then the result of invoking
   * {@link CursorProviderFactory#of(EventContext, Object, ComponentLocation)} is returned. Otherwise, the original {@code value}
   * is.
   *
   * @param value                 a value which may be a repeatable streaming resource
   * @param cursorProviderFactory a nullable {@link CursorStreamProviderFactory}
   * @param eventContext          the root context of the event on which the {@code value} was generated
   * @return the {@code value} or a {@link CursorProvider}
   *
   * @deprecated Use {@link #streamingContent(Object, CursorProviderFactory, EventContext, ComponentLocation)}
   */
  @Deprecated
  public static Object streamingContent(Object value, CursorProviderFactory cursorProviderFactory, EventContext eventContext) {
    return streamingContent(value, cursorProviderFactory, eventContext, null);
  }

  /**
   * If the {@code cursorProviderFactory} accepts the given {@code value}, then the result of invoking
   * {@link CursorProviderFactory#of(EventContext, Object, ComponentLocation)} is returned. Otherwise, the original {@code value}
   * is.
   *
   * @param value                 a value which may be a repeatable streaming resource
   * @param cursorProviderFactory a nullable {@link CursorStreamProviderFactory}
   * @param event                 the event on which the {@code value} was generated
   * @param originatingLocation   the {@link ComponentLocation} where the cursor was created
   * @return the {@code value} or a {@link CursorProvider}
   *
   * @since 4.4.0
   */
  public static Object streamingContent(Object value, CursorProviderFactory cursorProviderFactory, CoreEvent event,
                                        ComponentLocation originatingLocation) {
    return streamingContent(value, cursorProviderFactory, getRoot(event.getContext()),
                            originatingLocation);
  }

  /**
   * If the {@code cursorProviderFactory} accepts the given {@code value}, then the result of invoking
   * {@link CursorProviderFactory#of(EventContext, Object, ComponentLocation)} is returned. Otherwise, the original {@code value}
   * is.
   *
   * @param value                 a value which may be a repeatable streaming resource
   * @param cursorProviderFactory a nullable {@link CursorStreamProviderFactory}
   * @param event                 the event on which the {@code value} was generated
   * @return the {@code value} or a {@link CursorProvider}
   *
   * @deprecated Use {@link #streamingContent(Object, CursorProviderFactory, EventContext, ComponentLocation)}
   */
  @Deprecated
  public static Object streamingContent(Object value, CursorProviderFactory cursorProviderFactory, CoreEvent event) {
    return streamingContent(value, cursorProviderFactory, event, null);
  }

  /**
   * Closes the given {@code cursor} swallowing any exceptions found.
   *
   * @param cursor a {@link Cursor}
   * @return whether the {@code cursor} was closed or not
   */
  public static boolean closeQuietly(Cursor cursor) {
    if (cursor == null) {
      return false;
    }

    try {
      cursor.close();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Returns a {@link CursorIteratorProvider} which is backed by the given {@code items}.
   * <p>
   * Notice that since the {@code items} data is already fully loaded into memory, this kind of defeats the purpose of the cursor
   * provider. The purpose of this method is to provide a way to bridge the given data with the {@link CursorIteratorProvider}
   * abstraction. Possible use cases are mainly deserialization and testing. <b>Think twice</b> before using this method. Most
   * likely you're doing something wrong.
   * <p>
   * Also consider that because the data is already in memory, the cursors will never buffer into disk.
   *
   * @param items the items which back the returned provider
   * @param <T>   the generic type of the provider's items
   * @return a new {@link CursorIteratorProvider}
   */
  public static <T> CursorIteratorProvider asCursorProvider(List<T> items) {
    return new ListCursorIteratorProvider(items);
  }

  /**
   * Returns a {@link CursorStreamProvider} which is backed by the given {@code bytes}.
   * <p>
   * Notice that since the {@code bytes} data is already fully loaded into memory, this kind of defeats the purpose of the cursor
   * provider. The purpose of this method is to provide a way to bridge the given data with the {@link CursorStreamProvider}
   * abstraction. Possible use cases are mainly deserialization and testing. <b>Think twice</b> before using this method. Most
   * likely you're doing something wrong.
   * <p>
   * Also consider that because the data is already in memory, the cursors will never buffer into disk.
   *
   * @param bytes the byte array which backs the returned provider
   * @return a new {@link CursorStreamProvider}
   */
  public static CursorStreamProvider asCursorProvider(byte[] bytes) {
    return new ByteArrayCursorStreamProvider(bytes);
  }

  /**
   * If the {@code event} has a repeatable payload (instance of {@link CursorProvider}), then this method returns a new event
   * which payload has an equivalent, already consumed structure. This functionality makes sense for cases like caching in which
   * the contents of the stream need to survive the completion of the event that generated it.
   * <p>
   * If the payload is a {@link CursorStreamProvider}, then it will be consumed into a {@link ByteArrayCursorStreamProvider} so
   * that the contents are fully in memory while still keeping repeatable byte streaming semantics.
   * <p>
   * If the payload is a {@link CursorIteratorProvider}, then the contents will be consumed into a {@link List}.
   * <p>
   * In any other case, the same input event is returned
   *
   * @param event an event which might have a repeatable payload
   * @return a {@link CoreEvent}
   * @since 4.1
   */
  public static CoreEvent consumeRepeatablePayload(CoreEvent event) {
    TypedValue payload = event.getMessage().getPayload();

    if (payload.getValue() == null) {
      return event;
    }

    TypedValue replacedPayload = consumeRepeatableValue(payload);
    if (replacedPayload != payload) {
      event = CoreEvent.builder(event).message(
                                               Message.builder(event.getMessage())
                                                   .payload(replacedPayload)
                                                   .build())
          .build();
    }

    return event;
  }

  /**
   * If the {@code typedValue} has a repeatable payload (instance of {@link CursorProvider}), then this method returns a new
   * {@link TypedValue} which payload has an equivalent, already consumed structure. This functionality makes sense for cases like
   * caching in which the contents of the stream need to survive the completion of the event that generated it.
   * <p>
   * If the payload is a {@link CursorStreamProvider}, then it will be consumed into a {@link ByteArrayCursorStreamProvider} so
   * that the contents are fully in memory while still keeping repeatable byte streaming semantics.
   * <p>
   * If the payload is a {@link CursorIteratorProvider}, then the contents will be consumed into a {@link List}.
   * <p>
   * In any other case, the same input event is returned
   *
   * @param typedValue a typed value which might have a repeatable payload
   * @return a {@link TypedValue}
   * @since 4.1.4
   */
  public static TypedValue consumeRepeatableValue(TypedValue typedValue) {
    final Object originalPayload = typedValue.getValue();

    if (originalPayload == null) {
      return typedValue;
    }

    DataType originalDataType = typedValue.getDataType();
    TypedValue replacedPayload = typedValue;

    if (originalPayload instanceof CursorStreamProvider) {
      Object consumedPayload = asCursorProvider(toByteArray((CursorStreamProvider) originalPayload));

      replacedPayload = new TypedValue(consumedPayload, DataType.builder(originalDataType)
          .type(consumedPayload.getClass())
          .build());

    } else if (originalPayload instanceof CursorIteratorProvider) {
      List consumed = new LinkedList<>();
      ((CursorIteratorProvider) originalPayload).openCursor().forEachRemaining(consumed::add);
      DataType newDataType;
      if (originalDataType instanceof CollectionDataType) {
        final CollectionDataType collectionDataType = (CollectionDataType) originalDataType;
        newDataType = DataType.builder(originalDataType).collectionType(consumed.getClass())
            .itemType(collectionDataType.getItemDataType().getType())
            .itemMediaType(collectionDataType.getItemDataType().getMediaType())
            .build();
      } else {
        newDataType = DataType.builder(originalDataType).type(consumed.getClass()).build();
      }

      replacedPayload = new TypedValue(consumed, newDataType);
    }

    return replacedPayload;
  }

  private static CoreEvent replacePayload(CoreEvent event, Object newPayload) {
    return CoreEvent.builder(event)
        .message(Message.builder(event.getMessage())
            .value(newPayload)
            .build())
        .build();
  }

  private static void handlePossibleException(Reference<Throwable> exception) throws MuleException {
    Throwable t = exception.get();
    if (t != null) {
      throw rxExceptionToMuleException(t);
    }
  }

  private StreamingUtils() {}

  public static TypedValue updateTypedValueForStreaming(final TypedValue value,
                                                        final CoreEvent event,
                                                        final StreamingManager streamingManager) {
    if (event == null) {
      return value;
    }

    return updateTypedValueForStreaming(value, getRoot(event.getContext()), streamingManager);
  }

  /**
   * Updates the value a given {@link TypedValue} instance by replacing it with a {@link CursorProvider}.
   *
   * @param value            the typed value to update
   * @param rootEventContext the root context of the creating event
   * @param streamingManager the streaming manager
   * @return updated {@link TypedValue instance}
   */
  public static TypedValue updateTypedValueForStreaming(final TypedValue value,
                                                        final EventContext rootEventContext,
                                                        final StreamingManager streamingManager) {
    Object payload = value.getValue();
    if (payload instanceof CursorProvider) {
      CursorProvider cursorProvider =
          streamingManager.manage((CursorProvider) payload, rootEventContext);

      if (cursorProvider == payload) {
        return value;
      } else {
        DataType dataType = DataType.builder(value.getDataType()).type(cursorProvider.getClass()).build();
        return new TypedValue<>(cursorProvider, dataType, value.getByteLength());
      }
    }
    return value;
  }

  /**
   * Provides a function that updates the payload value of an {@link CoreEvent} by replacing it with a {@link CursorProvider}.
   *
   * @param streamingManager the streaming manager
   * @return function that maps the an {@link CoreEvent}
   */
  public static Function<CoreEvent, CoreEvent> updateEventForStreaming(final StreamingManager streamingManager) {
    return event -> {
      TypedValue payload = event.getMessage().getPayload();
      if (payload.getValue() instanceof CursorProvider) {
        final TypedValue updatedPayload = updateTypedValueForStreaming(payload, event, streamingManager);

        if (updatedPayload == payload) {
          return event;
        } else {
          Message message = Message.builder(event.getMessage())
              .payload(updatedPayload)
              .build();
          return CoreEvent.builder(event).message(message).build();
        }
      }
      return event;
    };
  }

  /**
   * Updates the {@link Cursor} value a given {@link TypedValue} instance by replacing it with a {@link CursorProvider}.
   *
   * @param value            the typed value to update
   * @param event            the current event
   * @param streamingManager the streaming manager
   * @return updated {@link TypedValue instance}
   * @deprecated Use {@link #updateTypedValueWithCursorProvider(TypedValue, StreamingManager)}.
   */
  @Deprecated
  public static TypedValue updateTypedValueWithCursorProvider(final TypedValue value, final CoreEvent event,
                                                              final StreamingManager streamingManager) {
    if (event == null) {
      return value;
    } else {
      return updateTypedValueWithCursorProvider(value, streamingManager);
    }
  }

  /**
   * Updates the {@link Cursor} value a given {@link TypedValue} instance by replacing it with a {@link CursorProvider}.
   *
   * @param value            the typed value to update
   * @param streamingManager the streaming manager
   * @return updated {@link TypedValue instance}
   */
  public static TypedValue updateTypedValueWithCursorProvider(final TypedValue value,
                                                              final StreamingManager streamingManager) {
    Object payload = value.getValue();
    if (payload instanceof CursorStream) {
      CursorProvider provider = ((CursorStream) payload).getProvider();
      DataType dataType = DataType.builder(value.getDataType()).type(provider.getClass()).build();
      return new TypedValue(provider, dataType, value.getByteLength());
    } else {
      return value;
    }
  }

  /**
   * @param componentModel a {@link Component}
   * @return Whether the {@code componentModel} supports streaming or not
   */
  public static boolean supportsStreaming(ComponentModel componentModel) {
    return componentModel instanceof ConnectableComponentModel
        && ((ConnectableComponentModel) componentModel).supportsStreaming();
  }
}
