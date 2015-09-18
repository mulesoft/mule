/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal;

import org.mule.api.connection.ConnectionException;
import org.mule.api.connection.ConnectionHandler;
import org.mule.api.connector.ConnectionManager;
import org.mule.module.extension.file.api.AbstractFileInputStream;
import org.mule.module.extension.file.api.FilePayload;
import org.mule.module.extension.file.api.PathLock;

import java.io.IOException;
import java.io.InputStream;

/**
 * An {@link AbstractFileInputStream} implementation which obtains a
 * {@link FtpFileSystem} through a {@link ConnectionManager} and uses it
 * to obtain the contents of a file on a FTP server.
 * <p>
 * When the stream is closed or fully consumed, the {@link FtpFileSystem}
 * is released back to the {@link ConnectionManager}
 *
 * @since 4.0
 */
final class FtpInputStream extends AbstractFileInputStream
{

    private final ConnectionHandler<FtpFileSystem> connectionHandler;
    private final FtpFileSystem ftpFileSystem;

    /**
     * Establishes the underlying connection and returns a new instance of this class.
     * <p>
     * Instances returned by this method <b>MUST</b> be closed or fully consumed.
     *
     * @param ftpConnector the {@link FtpConnector} through which the file is to be obtained
     * @param filePayload  a {@link FilePayload} referencing the file which contents are to be fetched
     * @param lock         the {@link PathLock} to be used
     * @return a new {@link FtpInputStream}
     * @throws ConnectionException if a connection could not be established
     */
    public static FtpInputStream newInstance(FtpConnector ftpConnector, FtpFilePayload filePayload, PathLock lock) throws ConnectionException
    {
        ConnectionHandler<FtpFileSystem> connection = ftpConnector.getConnectionManager().getConnection(ftpConnector);
        return new FtpInputStream(connection.getConnection().retrieveFileContent(filePayload), connection, lock);
    }

    private FtpInputStream(InputStream inputStream, ConnectionHandler<FtpFileSystem> connectionHandler, PathLock lock) throws ConnectionException
    {
        super(inputStream, lock);
        this.connectionHandler = connectionHandler;
        this.ftpFileSystem = connectionHandler.getConnection();
    }

    @Override
    protected void doClose() throws IOException
    {
        try
        {
            ftpFileSystem.awaitCommandCompletion();
        }
        finally
        {
            try
            {
                super.doClose();
            }
            finally
            {
                connectionHandler.release();
            }
        }
    }
}
