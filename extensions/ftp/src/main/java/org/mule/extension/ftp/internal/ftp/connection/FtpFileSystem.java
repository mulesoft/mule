/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.ftp.connection;

import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.module.extension.file.api.FileAttributes;
import org.mule.runtime.module.extension.file.api.FileSystem;

import java.io.InputStream;

/**
 * Specialization of the {@link FileSystem} contract for file systems mounted on FTP/SFTP servers
 *
 * @since 4.0
 */
public interface FtpFileSystem extends FileSystem {

  /**
   * Severs the underlying connection to the remote server
   */
  void disconnect();

  /**
   * Returns an InputStream which obtains the content for the file of the given {@code filePayload}.
   * <p>
   * The invoked <b>MUST</b> make sure that the returned stream is closed in order for the underlying connection to be closed.
   *
   * @param filePayload a {@link FileAttributes} referencing to a FTP file
   * @return an {@link InputStream}
   */
  InputStream retrieveFileContent(FileAttributes filePayload);

  /**
   * Validates the underlying connection to the remote server
   *
   * @return a {@link ConnectionValidationResult}
   */
  ConnectionValidationResult validateConnection();
}
