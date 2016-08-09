/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.api;

import org.mule.runtime.module.extension.file.api.FileAttributes;

/**
 * A specialization of {@link FileAttributes} which is associated to a {@link FileEventType}.
 * <p>
 * Because a {@link FileAttributes} is a snapshot of a file's attributes at a specific point of time, the purpose of this class is
 * to associate such state to a particular event type which took it into that state
 *
 * @since 4.0
 */
public interface EventedFileAttributes extends FileAttributes {

  /**
   * @return The name of the {@link FileEventType} upon which {@code this} instance was created
   */
  String getEventType();
}
