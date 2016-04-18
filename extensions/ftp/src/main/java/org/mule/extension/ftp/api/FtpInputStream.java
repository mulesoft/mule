/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.api;

import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.module.extension.file.api.FileAttributes;
import org.mule.runtime.module.extension.file.api.lock.PathLock;
import org.mule.runtime.module.extension.file.api.stream.AbstractFileInputStream;
import org.mule.runtime.module.extension.file.api.stream.LazyStreamSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

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
public final class FtpInputStream extends AbstractFileInputStream
{

    private final ConnectionHandler<FtpFileSystem> connectionHandler;
    private final FtpFileSystem ftpFileSystem;

    /**
     * Establishes the underlying connection and returns a new instance of this class.
     * <p>
     * Instances returned by this method <b>MUST</b> be closed or fully consumed.
     *
     * @param ftpConnector the {@link FtpConnector} through which the file is to be obtained
     * @param attributes   a {@link FileAttributes} referencing the file which contents are to be fetched
     * @param lock         the {@link PathLock} to be used
     * @return a new {@link FtpInputStream}
     * @throws ConnectionException if a connection could not be established
     */
    public static FtpInputStream newInstance(FtpConnector ftpConnector, FtpFileAttributes attributes, PathLock lock) throws ConnectionException
    {
        ConnectionHandler<FtpFileSystem> connectionHandler = ftpConnector.getConnectionManager().getConnection(ftpConnector);
        Supplier<InputStream> streamSupplier = () -> {
            try
            {
                return connectionHandler.getConnection().retrieveFileContent(attributes);
            }
            catch (ConnectionException e)
            {
                throw new MuleRuntimeException(createStaticMessage("Could not obtain connection to fetch file " + attributes.getPath()), e);
            }
        };
        return new FtpInputStream(streamSupplier, connectionHandler, lock);
    }

    private FtpInputStream(Supplier<InputStream> streamSupplier, ConnectionHandler<FtpFileSystem> connectionHandler, PathLock lock) throws ConnectionException
    {
        super(new LazyStreamSupplier(streamSupplier), lock);
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
