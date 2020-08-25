/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static java.lang.Boolean.getBoolean;
import static org.mule.runtime.api.util.MuleSystemProperties.STREAMING_VERBOSE_PROPERTY;

import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;

/**
 * Utilities for handling {@link Cursor cursors}
 *
 * @since 4.3.0 - 4.2.3
 */
public final class CursorUtils {

  private CursorUtils() {}

  public static final boolean STREAMING_VERBOSE = getBoolean(STREAMING_VERBOSE_PROPERTY);

  /**
   * Used to get the original {@link CursorProvider} when {@link CursorProviderDecorator} is used.
   * <p>
   * If {@code cursorProvider} is a decorator, then the {@link CursorProviderDecorator#getDelegate()} will be invoked
   * recursively until a non-decorator delegate is found. If {@code cursorProvider} is not a decorator, then that same instance
   * is returned.
   *
   * @param cursorProvider a provider which may or may not be a {@link CursorProviderDecorator}
   * @param <T>            the generic {@link Cursor} type as defined in {@link CursorProvider}
   * @return a non-decorator {@link CursorProvider}
   */
  public static <T extends Cursor> CursorProvider<T> unwrap(CursorProvider<T> cursorProvider) {
    while (cursorProvider instanceof CursorProviderDecorator) {
      cursorProvider = ((CursorProviderDecorator<T>) cursorProvider).getDelegate();
    }

    return cursorProvider;
  }
}
