/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;

public final class CursorUtils {

  private CursorUtils() {}

  public static <T extends Cursor> CursorProvider<T> unwrap(CursorProvider<T> cursorProvider) {
    while (cursorProvider instanceof CursorProviderDecorator) {
      cursorProvider = ((CursorProviderDecorator<T>) cursorProvider).getDelegate();
    }

    return cursorProvider;
  }
}
