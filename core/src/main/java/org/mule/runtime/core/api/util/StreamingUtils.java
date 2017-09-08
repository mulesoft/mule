/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import static org.mule.runtime.core.api.rx.Exceptions.rxExceptionToMuleException;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.bytes.CursorStreamProviderFactory;

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
   * @param event an {@link BaseEvent}
   * @param f the function to execute
   * @return the output {@link BaseEvent}
   * @throws MuleException
   */
  public static BaseEvent withCursoredEvent(BaseEvent event, CheckedFunction<BaseEvent, BaseEvent> f)
      throws MuleException {
    if (event.getMessage().getPayload() == null) {
      return event;
    }
    Reference<Throwable> exception = new Reference<>();
    CheckedFunction<BaseEvent, BaseEvent> function = new CheckedFunction<BaseEvent, BaseEvent>() {

      @Override
      public BaseEvent applyChecked(BaseEvent event) throws Throwable {
        return f.apply(event);
      }

      @Override
      public BaseEvent handleException(Throwable throwable) {
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

      BaseEvent value = function.apply(event);

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
   * {@link CursorProviderFactory#of(BaseEvent, Object)} is returned. Otherwise, the original {@code value} is.
   *
   * @param value a value which may be a repeatable streaming resource
   * @param cursorProviderFactory a nullable {@link CursorStreamProviderFactory}
   * @param event the event on which the {@code value} was generated
   * @return the {@code value} or a {@link CursorProvider}
   */
  public static Object streamingContent(Object value, CursorProviderFactory cursorProviderFactory, BaseEvent event) {
    if (cursorProviderFactory != null && cursorProviderFactory.accepts(value)) {
      return cursorProviderFactory.of(event, value);
    } else {
      return value;
    }
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

  private static BaseEvent replacePayload(BaseEvent event, Object newPayload) {
    return BaseEvent.builder(event)
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

  /**
   * Updates the value a given {@link TypedValue} instance by replacing it with a {@link CursorProvider}.
   *
   * @param value the typed value to update
   * @param event the current event
   * @param streamingManager the streaming manager
   * @return updated {@link TypedValue instance}
   */
  public static TypedValue updateTypedValueForStreaming(final TypedValue value, final BaseEvent event,
                                                        final StreamingManager streamingManager) {
    if (event == null) {
      return value;
    } else {
      Object payload = value.getValue();
      if (payload instanceof CursorProvider) {
        CursorProvider cursorProvider = streamingManager.manage((CursorProvider) payload, event);
        DataType dataType = DataType.builder(value.getDataType()).type(cursorProvider.getClass()).build();
        return new TypedValue<>(cursorProvider, dataType, value.getLength());
      }
      return value;
    }
  }

  /**
   * Provides a function that updates the payload value of an {@link BaseEvent} by replacing it with a {@link CursorProvider}.
   *
   * @param streamingManager the streaming manager
   * @return function that maps the an {@link BaseEvent}
   */
  public static Function<BaseEvent, BaseEvent> updateEventForStreaming(final StreamingManager streamingManager) {
    return event -> {
      TypedValue payload = event.getMessage().getPayload();
      if (payload.getValue() instanceof CursorProvider) {
        Message message = Message.builder(event.getMessage())
            .payload(updateTypedValueForStreaming(payload, event, streamingManager))
            .build();
        return BaseEvent.builder(event).message(message).build();
      }
      return event;
    };
  }

}
