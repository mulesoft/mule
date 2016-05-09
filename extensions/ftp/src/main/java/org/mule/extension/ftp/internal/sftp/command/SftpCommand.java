/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.sftp.command;

import org.mule.extension.ftp.api.FtpConnector;
import org.mule.extension.ftp.api.FtpFileAttributes;
import org.mule.extension.ftp.api.sftp.SftpFileSystem;
import org.mule.extension.ftp.internal.ftp.command.FtpCommand;
import org.mule.extension.ftp.internal.sftp.SftpFileAttributes;
import org.mule.extension.ftp.internal.sftp.connection.SftpClient;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Stack;

/**
 * Base class for {@link FtpCommand} implementations that target a SFTP server
 *
 * @since 4.0
 */
public abstract class SftpCommand extends FtpCommand<FtpConnector, SftpFileSystem>
{

    protected SftpClient client;

    /**
     * Creates a new instance
     *
     * @param fileSystem a {@link SftpFileSystem} used as the connection object
     * @param config     a {@link FtpConnector} which configures this command
     * @param client     a {@link SftpClient}
     */
    public SftpCommand(SftpFileSystem fileSystem, FtpConnector config, SftpClient client)
    {
        super(fileSystem, config);
        this.client = client;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected FtpFileAttributes getFile(String filePath, boolean requireExistence)
    {
        Path path = resolvePath(filePath);
        SftpFileAttributes attributes;
        try
        {
            attributes = client.getAttributes(path);
        }
        catch (Exception e)
        {
            throw exception("Found exception trying to obtain path " + path, e);
        }

        if (attributes != null)
        {
            return attributes;
        }
        else
        {
            if (requireExistence)
            {
                throw pathNotFoundException(path);
            }
            else
            {
                return null;
            }
        }
    }

    /**
     * Creates the directory pointed by {@code directoryPath} also creating
     * any missing parent directories
     *
     * @param directoryPath the {@link Path} to the directory you want to create
     */
    @Override
    protected void doMkDirs(Path directoryPath)
    {
        Stack<Path> fragments = new Stack<>();
        for (int i = directoryPath.getNameCount(); i >= 0; i--)
        {
            Path subPath = Paths.get("/").resolve(directoryPath.subpath(0, i));
            if (exists(subPath))
            {
                break;
            }
            fragments.push(subPath);
        }

        while (!fragments.isEmpty())
        {
            Path fragment = fragments.pop();
            client.mkdir(fragment.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean tryChangeWorkingDirectory(String path)
    {
        try
        {
            client.changeWorkingDirectory(path);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doRename(String filePath, String newName) throws Exception
    {
        client.rename(filePath, newName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getCurrentWorkingDirectory()
    {
        try
        {
            return client.getWorkingDirectory();
        }
        catch (Exception e)
        {
            throw exception("Failed to determine current working directory");
        }
    }
}
