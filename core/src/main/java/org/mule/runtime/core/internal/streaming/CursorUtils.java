/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static java.lang.System.identityHashCode;

import org.mule.runtime.api.streaming.Cursor;

/**
 * Utilities for handling {@link Cursor cursors}
 *
 * @since 4.3.0 - 4.2.3
 */
public class CursorUtils {

  private CursorUtils() {}

  public static <T extends Cursor> int createKey(ManagedCursorProvider<T> provider) {
    return identityHashCode(provider.getDelegate());
  }
}
