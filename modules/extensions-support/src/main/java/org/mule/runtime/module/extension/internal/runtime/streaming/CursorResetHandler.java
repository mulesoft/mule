/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.streaming;

import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.util.Pair;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entity in charge of reseting the position of a group of {@link Cursor}s .
 *
 * @since 4.3.1 4.4.0
 */
public class CursorResetHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(CursorResetHandler.class);

  private final List<Pair<Cursor, Long>> cursorPositions;

  public CursorResetHandler(List<Pair<Cursor, Long>> cursorPositions) {
    this.cursorPositions = cursorPositions;
  }

  /**
   * Resets the position of all the cursors to the position indicated when this object was created.
   */
  public void resetCursors() {
    cursorPositions.forEach(pair -> {
      try {
        pair.getFirst().seek(pair.getSecond());
      } catch (IOException e) {
        if (LOGGER.isWarnEnabled()) {
          LOGGER.warn("Could not reset cursor back to position " + pair.getSecond() + ". Inconsistencies might occur if "
              + "a retry is attempted", e);
        }
      }
    });
  }
}
