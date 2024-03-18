/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;

/**
 * A Cursor provider that contains an ID that can unequivocally identifies it.
 *
 * @param <T> the generic {@link Cursor} type as defined in {@link CursorProvider}
 * @since 4.3.0 - 4.2.3
 */
public interface IdentifiableCursorProvider<T extends Cursor> extends CursorProvider<T> {

  /**
   * @return the provider's id.
   */
  int getId();
}
