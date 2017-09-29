/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.core.privileged.event.BaseEventContext;

/**
 * Groups a {@link CursorProvider} with information about its context
 *
 * @since 4.0
 */
public final class CursorContext {

  private final CursorProvider cursorProvider;
  private final BaseEventContext ownerContext;

  /**
   * Creates a new instance
   *
   * @param cursorProvider the {@link CursorProvider} which will be managed
   * @param ownerContext   the {@link BaseEventContext} which owns the {@code cursorProvider}
   */
  public CursorContext(CursorProvider cursorProvider, BaseEventContext ownerContext) {
    this.cursorProvider = cursorProvider;
    this.ownerContext = ownerContext;
  }

  /**
   * @return the {@link CursorProvider} being managed
   */
  public CursorProvider getCursorProvider() {
    return cursorProvider;
  }

  /**
   * @return the {@link BaseEventContext} which owns the {@code cursorProvider}
   */
  public BaseEventContext getOwnerContext() {
    return ownerContext;
  }
}
