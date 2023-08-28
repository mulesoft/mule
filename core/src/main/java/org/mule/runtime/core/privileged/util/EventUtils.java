/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.util;

import org.mule.runtime.api.event.EventContext;
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

}
