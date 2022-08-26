/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.util;

import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.core.privileged.event.BaseEventContext;

/**
 * Utilities for handling events
 *
 * @since 4.3.0
 */
public final class EventUtils {

  private EventUtils() {}

  /**
   * Returns the root context for the given {@code eventContext}
   *
   * @param eventContext an {@link EventContext}
   * @return the root context
   */
  public static EventContext getRoot(EventContext eventContext) {
    return ((BaseEventContext) eventContext).getRootContext();
  }

  public static <T> T withNullEvent(CheckedFunction<CoreEvent, T> function) {
    CoreEvent event = getNullEvent();
    try {
      T value = function.apply(event);
      ((BaseEventContext) event.getContext()).success();
      return value;
    } catch (RuntimeException e) {
      ((BaseEventContext) event.getContext()).error(e);
      throw e;
    }
  }
}
