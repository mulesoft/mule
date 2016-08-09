/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal;

import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import org.mule.extension.ftp.api.FtpFileAttributes;
import org.mule.extension.ftp.internal.ftp.connection.FtpFileSystem;
import org.mule.extension.ftp.internal.ftp.connection.ClassicFtpFileSystem;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.module.extension.file.api.lock.PathLock;
import org.mule.runtime.module.extension.file.api.stream.AbstractFileInputStream;
import org.mule.runtime.module.extension.file.api.stream.LazyStreamSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

/**
 * An {@link AbstractFileInputStream} implementation which obtains a {@link FtpFileSystem} through a {@link ConnectionManager} and
 * uses it to obtain the contents of a file on a FTP server.
 * <p>
 * When the stream is closed or fully consumed, the {@link ClassicFtpFileSystem} is released back to the {@link ConnectionManager}
 *
 * @since 4.0
 */
public abstract class FtpInputStream extends AbstractFileInputStream {

  private final ConnectionHandler<FtpFileSystem> connectionHandler;
  private final FtpFileSystem ftpFileSystem;

  protected static ConnectionHandler<FtpFileSystem> getConnectionHandler(FtpConnector config) throws ConnectionException {
    return config.getConnectionManager().getConnection(config);
  }

  protected static Supplier<InputStream> getStreamSupplier(FtpFileAttributes attributes,
                                                           ConnectionHandler<FtpFileSystem> connectionHandler) {
    Supplier<InputStream> streamSupplier = () -> {
      try {
        return connectionHandler.getConnection().retrieveFileContent(attributes);
      } catch (ConnectionException e) {
        throw new MuleRuntimeException(createStaticMessage("Could not obtain connection to fetch file " + attributes.getPath()),
                                       e);
      }
    };

    return streamSupplier;
  }


  protected FtpInputStream(Supplier<InputStream> streamSupplier, ConnectionHandler<FtpFileSystem> connectionHandler,
                           PathLock lock)
      throws ConnectionException {
    super(new LazyStreamSupplier(streamSupplier), lock);
    this.connectionHandler = connectionHandler;
    this.ftpFileSystem = connectionHandler.getConnection();
  }

  @Override
  protected void doClose() throws IOException {
    try {
      beforeClose();
    } finally {
      try {
        super.doClose();
      } finally {
        connectionHandler.release();
      }
    }
  }

  /**
   * Template method for performing operations just before the stream is closed. This default implementation is empty.
   *
   * @throws IOException
   */
  protected void beforeClose() throws IOException {}

  /**
   * @return the {@link FtpFileSystem} used to obtain the stream
   */
  protected FtpFileSystem getFtpFileSystem() {
    return ftpFileSystem;
  }
}
