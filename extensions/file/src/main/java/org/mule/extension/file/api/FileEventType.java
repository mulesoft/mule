/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.api;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.nio.file.WatchEvent.Kind;

/**
 * Enumerates types of events which can occur on a file
 *
 * @since 4.0
 */
public enum FileEventType {

  /**
   * Represents the event of a file being created
   */
  CREATE(ENTRY_CREATE),

  /**
   * Represents the event of a file being updated
   */
  UPDATE(ENTRY_MODIFY),

  /**
   * Represents the event of a file being deleted
   */
  DELETE(ENTRY_DELETE);

  private final Kind kind;

  /**
   * Returns a {@Link FileEventType} which is equivalent to the given {@code kind}
   *
   * @param kind a Java event {@link Kind}
   * @return a {@link FileEventType}
   * @throws IllegalArgumentException if the {@code kind} does not have an equivalent {@link FileEventType}
   */
  public static FileEventType of(Kind kind) {
    if (kind == ENTRY_CREATE) {
      return CREATE;
    } else if (kind == ENTRY_MODIFY) {
      return UPDATE;
    } else if (kind == ENTRY_DELETE) {
      return DELETE;
    }

    throw new IllegalArgumentException("Invalid Event Kind: " + kind.name());
  }

  FileEventType(Kind kind) {
    this.kind = kind;
  }

  /**
   * @return a {@link Kind} with the same semantic meaning as {@code this} instance
   */
  public Kind asEventKind() {
    return kind;
  }
}
