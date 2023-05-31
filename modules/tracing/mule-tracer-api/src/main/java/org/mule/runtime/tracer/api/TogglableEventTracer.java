/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.api;

import org.mule.runtime.api.event.Event;

/**
 * An {@link EventTracer} that can be enabled/disabled.
 *
 * @param <T> the extension of {@link Event} used as a carrier for the tracing context.
 */
public interface TogglableEventTracer<T extends Event> extends EventTracer<T> {

  /**
   * @param enabled whether tracing is enabled.
   */
  void toggle(boolean enabled);
}
