/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.core.api.EventContext;

/**
 * Groups a {@link CursorProvider} with information about its context
 *
 * @since 4.0
 */
public class CursorProviderHandle {

  private final CursorProvider cursorProvider;
  private final EventContext ownerContext;

  /**
   * Creates a new instance
   *
   * @param cursorProvider the {@link CursorProvider} which will be tracked
   * @param ownerContext   the {@link EventContext} which owns the {@code cursorProvider}
   */
  public CursorProviderHandle(CursorProvider cursorProvider, EventContext ownerContext) {
    this.cursorProvider = cursorProvider;
    this.ownerContext = ownerContext;
  }

  /**
   * @return the {@link CursorProvider} being tracked
   */
  public CursorProvider getCursorProvider() {
    return cursorProvider;
  }

  /**
   * @return the {@link EventContext} which owns the {@code cursorProvider}
   */
  public EventContext getOwnerContext() {
    return ownerContext;
  }
}
