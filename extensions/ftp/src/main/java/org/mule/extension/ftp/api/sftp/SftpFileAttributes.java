/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.api.sftp;

import org.mule.extension.file.common.api.AbstractFileAttributes;
import org.mule.extension.ftp.api.FtpFileAttributes;

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

  private LocalDateTime timestamp;
  private long size;
  private boolean regularSize;
  private boolean directory;
  private boolean symbolicLink;

  /**
   * Creates a new instance
   *
   * @param path the file's {@link Path}
   * @param attrs the {@link SftpATTRS} which represents the file on the SFTP server
   */
  public SftpFileAttributes(Path path, SftpATTRS attrs) {
    super(path);

    Date timestamp = new Date(((long) attrs.getMTime()) * 1000L);
    this.timestamp = asDateTime(timestamp.toInstant());
    this.size = attrs.getSize();
    this.regularSize = attrs.isReg();
    this.directory = attrs.isDir();
    this.symbolicLink = attrs.isLink();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getSize() {
    return size;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isRegularFile() {
    return regularSize;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDirectory() {
    return directory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSymbolicLink() {
    return symbolicLink;
  }
}
