/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.mule.runtime.core.api.rx.Exceptions.rxExceptionToMuleException;
import static reactor.core.Exceptions.unwrap;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.streaming.CursorStream;
import org.mule.runtime.api.streaming.CursorStreamProvider;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.util.func.CheckedFunction;

/**
 * Utilities for handling {@link CursorStream} instances
 *
 * @since 4.0
 */
public final class CursorStreamUtils {

  /**
   * Executes the given function {@code f} considering that the given {@code event} might have
   * a {@link CursorStreamProvider} as payload. In that case, this method obtains a cursor from the provider
   * and executes the function.
   *
   * Closing the opened cursor, handling exceptions and return values are all taken care of by this utility
   * method.
   *
   * @param event an {@link Event}
   * @param f the function to execute
   * @return the output {@link Event}
   * @throws MuleException
   */
  public static Event withCursoredEvent(Event event, CheckedFunction<Event, Event> f) throws MuleException {
    if (event.getMessage().getPayload() == null) {
      return event;
    }
    Reference<Throwable> exception = new Reference<>();
    CheckedFunction<Event, Event> function = new CheckedFunction<Event, Event>() {

      @Override
      public Event applyChecked(Event event) throws Throwable {
        return f.apply(event);
      }

      @Override
      public Event handleException(Throwable throwable) {
        exception.set(unwrap(throwable));
        return null;
      }
    };

    Object payload = event.getMessage().getPayload().getValue();
    CursorStreamProvider cursorStreamProvider = null;
    CursorStream cursor = null;
    try {
      if (payload instanceof CursorStreamProvider) {
        cursorStreamProvider = (CursorStreamProvider) payload;
        cursor = cursorStreamProvider.openCursor();
        event = replacePayload(event, cursor);
      }

      Event value = function.apply(event);

      if (value == null) {
        handlePossibleException(exception);
      } else if (value.getMessage().getPayload().getValue() == cursor) {
        value = replacePayload(value, cursorStreamProvider);
      }

      return value;
    } finally {
      closeQuietly(cursor);
    }
  }

  private static Event replacePayload(Event event, Object newPayload) {
    return Event.builder(event)
        .message(Message.builder(event.getMessage())
            .payload(newPayload)
            .build())
        .build();
  }

  private static void handlePossibleException(Reference<Throwable> exception) throws MuleException {
    Throwable t = exception.get();
    if (t != null) {
      throw rxExceptionToMuleException(t);
    }
  }

  private CursorStreamUtils() {}
}
