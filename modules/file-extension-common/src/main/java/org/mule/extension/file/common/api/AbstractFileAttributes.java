/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.common.api;

import org.mule.runtime.core.message.BaseAttributes;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Base class for implementations of {@link FileAttributes}
 *
 * @since 4.0
 */
public abstract class AbstractFileAttributes extends BaseAttributes implements FileAttributes {

  protected final String path;
  private String fileName;

  /**
   * Creates a new instance
   *
   * @param path a {@link Path} pointing to the represented file
   */
  protected AbstractFileAttributes(Path path) {
    this.path = path.toString();
    this.fileName = path.getFileName().toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getPath() {
    return path;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return fileName;
  }

  protected LocalDateTime asDateTime(Instant instant) {
    return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
  }
}
