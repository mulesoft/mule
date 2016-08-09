/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.api;

import org.mule.runtime.module.extension.file.api.FileAttributes;

import java.time.LocalDateTime;

/**
 * Specialization of {@link FileAttributes} which adds FTP specific information
 *
 * @since 4.0
 */
public interface FtpFileAttributes extends FileAttributes {

  /**
   * @return The last time the file was modified
   */
  LocalDateTime getTimestamp();
}
