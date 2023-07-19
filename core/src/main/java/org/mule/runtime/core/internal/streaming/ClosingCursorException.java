/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.streaming;

import org.mule.runtime.api.streaming.Cursor;

import java.io.IOException;

/**
 * This {@link RuntimeException} is used for debugging purposes to track where a {@link Cursor} is closed.
 * 
 * @since 4.4.0, 4.3.1
 */
public class ClosingCursorException extends IOException {

  private static final long serialVersionUID = 7821893999235016552L;

  public ClosingCursorException(String message) {
    super(message);
  }
}
