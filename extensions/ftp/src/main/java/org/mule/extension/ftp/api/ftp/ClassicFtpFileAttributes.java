/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.api.ftp;

import org.mule.extension.file.common.api.AbstractFileAttributes;
import org.mule.extension.ftp.api.FtpFileAttributes;

import java.nio.file.Path;
import java.time.LocalDateTime;

import org.apache.commons.net.ftp.FTPFile;

/**
 * Implementation of {@link FtpFileAttributes} for files read from a FTP server.
 *
 * @since 4.0
 */
public class ClassicFtpFileAttributes extends AbstractFileAttributes implements FtpFileAttributes {

  private LocalDateTime timestamp;
  private String name;
  private long size;
  private boolean regularFile;
  private boolean directory;
  private boolean symbolicLink;

  /**
   * Creates a new instance
   *
   * @param path the file's {@link Path}
   * @param ftpFile the {@link FTPFile} which represents the file on the FTP server
   */
  public ClassicFtpFileAttributes(Path path, FTPFile ftpFile) {
    super(path);
    timestamp = asDateTime(ftpFile.getTimestamp().toInstant());
    name = ftpFile.getName();
    size = ftpFile.getSize();
    regularFile = ftpFile.isFile();
    directory = ftpFile.isDirectory();
    symbolicLink = ftpFile.isSymbolicLink();
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
  public String getName() {
    return name;
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
    return regularFile;
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
