/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.ftp.connection;

import static org.mule.extension.file.common.api.exceptions.FileError.DISCONNECTED;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import org.mule.extension.file.common.api.AbstractFileSystem;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.exceptions.FileError;
import org.mule.extension.ftp.api.FTPConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;

import java.io.InputStream;

/**
 * Specialization of the {@link AbstractFileSystem} contract for file systems mounted on FTP/SFTP servers
 *
 * @since 4.0
 */
public abstract class FtpFileSystem extends AbstractFileSystem {

  /**
   * {@inheritDoc}
   */
  public FtpFileSystem(String basePath) {
    super(basePath);
  }

  /**
   * Severs the underlying connection to the remote server
   */
  public abstract void disconnect();

  /**
   * Returns an InputStream which obtains the content for the file of the given {@code filePayload}.
   * <p>
   * The invoked <b>MUST</b> make sure that the returned stream is closed in order for the underlying connection to be closed.
   *
   * @param filePayload a {@link FileAttributes} referencing to a FTP file
   * @return an {@link InputStream}
   */
  public abstract InputStream retrieveFileContent(FileAttributes filePayload);

  /**
   * Validates the underlying connection to the remote server
   *
   * @return a {@link ConnectionValidationResult}
   */
  public ConnectionValidationResult validateConnection() {
    if (!isConnected()) {
      return failure("Connection is stale", new FTPConnectionException("Connection is stale", DISCONNECTED));
    }

    try {
      changeToBaseDir();
    } catch (Exception e) {
      failure("Configured workingDir is unavailable", e);
    }
    return success();
  }

  protected abstract boolean isConnected();
}
