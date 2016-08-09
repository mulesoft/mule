/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.api.sftp;

import org.mule.extension.ftp.api.FtpFileAttributes;
import org.mule.runtime.module.extension.file.api.AbstractFileAttributes;

import com.jcraft.jsch.SftpATTRS;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Implementation of {@link FtpFileAttributes} for files read from a SFTP server.
 *
 * @since 4.0
 */
public class SftpFileAttributes extends AbstractFileAttributes implements FtpFileAttributes {

  private final SftpATTRS attrs;

  /**
   * Creates a new instance
   *
   * @param path the file's {@link Path}
   * @param attrs the {@link SftpATTRS} which represents the file on the SFTP server
   */
  public SftpFileAttributes(Path path, SftpATTRS attrs) {
    super(path);
    this.attrs = attrs;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LocalDateTime getTimestamp() {
    Date timestamp = new Date(((long) attrs.getMTime()) * 1000L);
    return asDateTime(timestamp.toInstant());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getSize() {
    return attrs.getSize();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isRegularFile() {
    return attrs.isReg();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDirectory() {
    return attrs.isDir();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSymbolicLink() {
    return attrs.isLink();
  }
}
