/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.api;

import java.nio.file.Path;

/**
 * A specialization of {@link LocalFileAttributes} which also implements {@link EventedFileAttributes}
 *
 * @since 4.0
 */
public class ListenerFileAttributes extends LocalFileAttributes implements EventedFileAttributes {

  private final FileEventType eventType;

  /**
   * Creates a new instance
   *
   * @param path a {@link Path} pointing to the represented file
   * @param eventType the associated {@link FileEventType}
   */
  public ListenerFileAttributes(Path path, FileEventType eventType) {
    super(path);
    this.eventType = eventType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEventType() {
    return eventType.name();
  }
}
