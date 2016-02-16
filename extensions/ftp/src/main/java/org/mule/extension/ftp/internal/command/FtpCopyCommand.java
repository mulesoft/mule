/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.command;

import static java.lang.String.format;
import org.mule.api.MuleEvent;
import org.mule.api.connection.ConnectionException;
import org.mule.api.connection.ConnectionHandler;
import org.mule.api.connector.ConnectionManager;
import org.mule.extension.ftp.internal.FtpConnector;
import org.mule.extension.ftp.internal.FtpFileAttributes;
import org.mule.extension.ftp.internal.FtpFileSystem;
import org.mule.module.extension.file.api.FileAttributes;
import org.mule.module.extension.file.api.FileWriteMode;
import org.mule.module.extension.file.api.command.CopyCommand;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

/**
 * A {@link AbstractFtpCopyCommand} which implements the {@link CopyCommand} contract
 *
 * @since 4.0
 */
public final class FtpCopyCommand extends AbstractFtpCopyCommand implements CopyCommand
{

    /**
     * {@inheritDoc}
     */
    public FtpCopyCommand(FtpFileSystem fileSystem, FtpConnector config, FTPClient client)
    {
        super(fileSystem, config, client);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copy(String sourcePath, String targetPath, boolean overwrite, boolean createParentDirectory, MuleEvent event)
    {
        execute(sourcePath, targetPath, overwrite, createParentDirectory, event);
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
    protected void doExecute(FileAttributes source, Path targetPath, boolean overwrite, MuleEvent event)
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
            throw exception(String.format("FTP Copy operations require the use of two FTP connections. An exception was found trying to obtain second connection to" +
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
            throw exception(format("Found exception copying file '%s' to '%s'", source, targetPath), e);
        }
        finally
        {
            writerConnectionHandler.release();
        }
    }

    private void copyDirectory(Path sourcePath, Path target, boolean overwrite, FtpFileSystem writerConnection, MuleEvent event)
    {
        changeWorkingDirectory(sourcePath);
        FTPFile[] files;
        try
        {
            files = client.listFiles();
        }
        catch (IOException e)
        {
            throw exception(format("Could not list contents of directory '%s' while trying to copy it to ", sourcePath, target), e);
        }

        for (FTPFile file : files)
        {
            if (isVirtualDirectory(file.getName()))
            {
                continue;
            }

            FileAttributes fileAttributes = new FtpFileAttributes(sourcePath.resolve(file.getName()), file);

            if (fileAttributes.isDirectory())
            {
                Path targetPath = target.resolve(fileAttributes.getName());
                copyDirectory(Paths.get(fileAttributes.getPath()), targetPath, overwrite, writerConnection, event);
            }
            else
            {
                copyFile(fileAttributes, target.resolve(fileAttributes.getName()), overwrite, writerConnection, event);
            }
        }
    }

    private void copyFile(FileAttributes source, Path target, boolean overwrite, FtpFileSystem writerConnection, MuleEvent event)
    {
        FileAttributes targetFile = getFile(target.toString());
        if (targetFile != null)
        {
            if (overwrite)
            {
                fileSystem.delete(targetFile.getPath());
            }
            else
            {
                throw exception(String.format("Cannot copy file '%s' to path '%s' because it already exists", source.getPath(), target));
            }
        }

        try (InputStream inputStream = client.retrieveFileStream(source.getPath()))
        {
            if (inputStream == null)
            {
                throw exception(String.format("Could not read file '%s' while trying to copy it to remote path '%s'", source.getPath(), target));
            }

            writeCopy(target.toString(), inputStream, overwrite, writerConnection, event);
        }
        catch (Exception e)
        {
            throw exception(String.format("Found exception while trying to copy file '%s' to remote path '%s'", source.getPath(), target), e);
        }
        finally
        {
            fileSystem.awaitCommandCompletion();
        }
    }

    private void writeCopy(String targetPath, InputStream inputStream, boolean overwrite, FtpFileSystem writerConnection, MuleEvent event) throws IOException
    {
        final FileWriteMode mode = overwrite ? FileWriteMode.OVERWRITE : FileWriteMode.CREATE_NEW;
        writerConnection.write(targetPath, inputStream, mode, event, false, true);
    }

    private ConnectionHandler<FtpFileSystem> getWriterConnection() throws ConnectionException
    {
        ConnectionManager connectionManager = fileSystem.getConfig().getConnectionManager();
        return connectionManager.getConnection(fileSystem.getConfig());
    }
}
