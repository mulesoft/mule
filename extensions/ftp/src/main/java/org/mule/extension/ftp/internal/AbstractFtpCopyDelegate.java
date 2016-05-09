/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal;

import static java.lang.String.format;
import org.mule.extension.ftp.api.FtpConnector;
import org.mule.extension.ftp.api.ftp.FtpFileSystem;
import org.mule.extension.ftp.internal.ftp.command.FtpCommand;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.module.extension.file.api.FileAttributes;
import org.mule.runtime.module.extension.file.api.FileWriteMode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Abstract implementation of {@link FtpCopyDelegate} for copying operations
 * which require to FTP connections, one for reading the source file
 * and another for writing into the target path
 *
 * @since 4.0
 */
public abstract class AbstractFtpCopyDelegate implements FtpCopyDelegate
{

    private final FtpConnector config;
    private final FtpCommand command;
    private final FtpFileSystem fileSystem;

    /**
     * Creates new instance
     *
     * @param config     a {@link FtpConnector}
     * @param command    the {@link FtpCommand} which requested this operation
     * @param fileSystem the {@link FtpFileSystem} which connects to the remote server
     */
    public AbstractFtpCopyDelegate(FtpConnector config, FtpCommand command, FtpFileSystem fileSystem)
    {
        this.config = config;
        this.command = command;
        this.fileSystem = fileSystem;
    }

    /**
     * Performs a recursive copy
     *
     * @param source     the {@link FileAttributes} for the file to be copied
     * @param targetPath the {@link Path} to the target destination
     * @param overwrite  whether to overwrite existing target paths
     * @param event      the {@link MuleEvent} which triggered this operation
     */
    @Override
    public void doCopy(FileAttributes source, Path targetPath, boolean overwrite, MuleEvent event)
    {
        ConnectionHandler<FtpFileSystem> writerConnectionHandler;
        final FtpFileSystem writerConnection;
        try
        {
            writerConnectionHandler = getWriterConnection();
            writerConnection = writerConnectionHandler.getConnection();
        }
        catch (ConnectionException e)
        {
            throw command.exception(format("FTP Copy operations require the use of two FTP connections. An exception was found trying to obtain second connection to" +
                                           "copy the path '%s' to '%s'", source.getPath(), targetPath), e);
        }
        try
        {
            if (source.isDirectory())
            {
                copyDirectory(Paths.get(source.getPath()), targetPath, overwrite, writerConnection, event);
            }
            else
            {
                copyFile(source, targetPath, overwrite, writerConnection, event);
            }
        }
        catch (Exception e)
        {
            throw command.exception(format("Found exception copying file '%s' to '%s'", source, targetPath), e);
        }
        finally
        {
            writerConnectionHandler.release();
        }
    }

    /**
     * Performs a recursive copy of a directory
     *
     * @param sourcePath       the path to the directory to be copied
     * @param target           the target path
     * @param overwrite        whether to overwrite the target files if they already exists
     * @param writerConnection the {@link FtpFileSystem} which connects to the target endpoint
     * @param event            the {@link MuleEvent} which triggered this operation
     */
    protected abstract void copyDirectory(Path sourcePath, Path target, boolean overwrite, FtpFileSystem writerConnection, MuleEvent event);

    /**
     * Copies one individual file
     *
     * @param source           the {@link FileAttributes} for the file to be copied
     * @param target           the target path
     * @param overwrite        whether to overwrite the target files if they already exists
     * @param writerConnection the {@link FtpFileSystem} which connects to the target endpoint
     * @param event            the {@link MuleEvent} which triggered this operation
     */
    protected void copyFile(FileAttributes source, Path target, boolean overwrite, FtpFileSystem writerConnection, MuleEvent event)
    {
        FileAttributes targetFile = command.getFile(target.toString());
        if (targetFile != null)
        {
            if (overwrite)
            {
                fileSystem.delete(targetFile.getPath());
            }
            else
            {
                throw command.exception(format("Cannot copy file '%s' to path '%s' because it already exists", source.getPath(), target));
            }
        }

        try (InputStream inputStream = fileSystem.retrieveFileContent(source))
        {
            if (inputStream == null)
            {
                throw command.exception(format("Could not read file '%s' while trying to copy it to remote path '%s'", source.getPath(), target));
            }

            writeCopy(target.toString(), inputStream, overwrite, writerConnection, event);
        }
        catch (Exception e)
        {
            throw command.exception(format("Found exception while trying to copy file '%s' to remote path '%s'", source.getPath(), target), e);
        }
    }

    private void writeCopy(String targetPath, InputStream inputStream, boolean overwrite, FtpFileSystem writerConnection, MuleEvent event) throws IOException
    {
        final FileWriteMode mode = overwrite ? FileWriteMode.OVERWRITE : FileWriteMode.CREATE_NEW;
        writerConnection.write(targetPath, inputStream, mode, event, false, true);
    }

    private ConnectionHandler<FtpFileSystem> getWriterConnection() throws ConnectionException
    {
        return config.getConnectionManager().getConnection(config);
    }
}
