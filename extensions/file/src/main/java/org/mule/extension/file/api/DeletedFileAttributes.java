/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.api;

import static org.mule.extension.file.api.FileEventType.DELETE;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * A specialized version of {@link ListenerFileAttributes} which is always associated to a {@link FileEventType#DELETE} event.
 * <p>
 * Because the referenced file has been deleted, most of its attributes (such as size, type, timestamps, etc) are no longer
 * available. Thus, all but the below listed methods will throw {@link IllegalStateException} upon invocation:
 * <p>
 * <ul>
 * <li>{@link #getPath()}</li>
 * <li>{@link #getName()}</li>
 * <li>{@link #getEventType()}</li>
 * </ul>
 *
 * @since 4.0
 */
public class DeletedFileAttributes extends ListenerFileAttributes {

  /**
   * Creates a new instance
   * 
   * @param path the path of the deleted file
   */
  public DeletedFileAttributes(Path path) {
    super(path, DELETE);
  }

  /**
   * @throws IllegalStateException
   */
  private IllegalStateException unsupported(String property) {
    throw new IllegalStateException(String.format("Cannot obtain %s property for path '%s' because it has been deleted", property,
                                                  getPath()));
  }

  /**
   * @throws IllegalStateException
   */
  @Override
  public LocalDateTime getLastModifiedTime() {
    throw unsupported("lastModifiedTime");
  }

  /**
   * @throws IllegalStateException
   */
  @Override
  public LocalDateTime getLastAccessTime() {
    throw unsupported("lastAccessTime");
  }

  /**
   * @throws IllegalStateException
   */
  @Override
  public LocalDateTime getCreationTime() {
    throw unsupported("creationTime");
  }

  /**
   * @throws IllegalStateException
   */
  @Override
  public long getSize() {
    throw unsupported("size");
  }

  /**
   * @throws IllegalStateException
   */
  @Override
  public boolean isRegularFile() {
    throw unsupported("isRegularFile");
  }

  /**
   * @throws IllegalStateException
   */
  @Override
  public boolean isDirectory() {
    throw unsupported("isDirectory");
  }

  /**
   * @throws IllegalStateException
   */
  @Override
  public boolean isSymbolicLink() {
    throw unsupported("isSymbolicLink");
  }

  /**
   * @throws IllegalStateException
   */
  @Override
  protected LocalDateTime asDateTime(Instant instant) {
    throw unsupported("dateTime");
  }
}
