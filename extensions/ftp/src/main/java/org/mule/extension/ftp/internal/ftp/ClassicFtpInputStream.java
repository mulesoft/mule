/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.ftp;

import org.mule.extension.ftp.internal.FtpConnector;
import org.mule.extension.ftp.api.FtpFileAttributes;
import org.mule.extension.ftp.internal.ftp.connection.FtpFileSystem;
import org.mule.extension.ftp.internal.FtpInputStream;
import org.mule.extension.ftp.internal.ftp.connection.ClassicFtpFileSystem;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.module.extension.file.api.FileAttributes;
import org.mule.runtime.module.extension.file.api.lock.PathLock;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

/**
 * Implementation of {@link FtpInputStream} for FTP connections
 *
 * @since 4.0
 */
public class ClassicFtpInputStream extends FtpInputStream {

  /**
   * Establishes the underlying connection and returns a new instance of this class.
   * <p>
   * Instances returned by this method <b>MUST</b> be closed or fully consumed.
   *
   * @param config the {@link FtpConnector} which is configuring the connection
   * @param attributes a {@link FileAttributes} referencing the file which contents are to be fetched
   * @param lock the {@link PathLock} to be used
   * @return a new {@link FtpInputStream}
   * @throws ConnectionException if a connection could not be established
   */
  public static FtpInputStream newInstance(FtpConnector config, FtpFileAttributes attributes, PathLock lock)
      throws ConnectionException {
    ConnectionHandler<FtpFileSystem> connectionHandler = getConnectionHandler(config);
    return new ClassicFtpInputStream(getStreamSupplier(attributes, connectionHandler), connectionHandler, lock);
  }

  private ClassicFtpInputStream(Supplier<InputStream> streamSupplier, ConnectionHandler<FtpFileSystem> connectionHandler,
                                PathLock lock)
      throws ConnectionException {
    super(streamSupplier, connectionHandler, lock);
  }

  /**
   * Invokes {@link ClassicFtpFileSystem#awaitCommandCompletion()} to make sure that the operation is completed before closing the
   * stream
   */
  @Override
  protected void beforeClose() throws IOException {
    ((ClassicFtpFileSystem) getFtpFileSystem()).awaitCommandCompletion();
  }
}
