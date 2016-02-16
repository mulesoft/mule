/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.command;

import static java.lang.String.format;
import org.mule.api.MuleEvent;
import org.mule.extension.ftp.internal.FtpConnector;
import org.mule.extension.ftp.internal.FtpFileSystem;
import org.mule.module.extension.file.api.FileAttributes;
import org.mule.module.extension.file.api.command.MoveCommand;

import java.nio.file.Path;

import org.apache.commons.net.ftp.FTPClient;

/**
 * A {@link AbstractFtpCopyCommand} which implements the {@link MoveCommand} contract
 *
 * @since 4.0
 */
public final class FtpMoveCommand extends AbstractFtpCopyCommand implements MoveCommand
{

    /**
     * {@inheritDoc}
     */
    public FtpMoveCommand(FtpFileSystem fileSystem, FtpConnector config, FTPClient client)
    {
        super(fileSystem, config, client);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(String sourcePath, String targetPath, boolean overwrite, boolean createParentDirectory)
    {
        execute(sourcePath, targetPath, overwrite, createParentDirectory, null);
    }

    /**
     * Performs a recursive move
     *
     * @param source     the {@link FileAttributes} for the file to be copied
     * @param targetPath the {@link Path} to the target destination
     * @param overwrite  whether to overwrite existing target paths
     * @param event      the {@link MuleEvent} which triggered this operation
     */
    @Override
    protected void doExecute(FileAttributes source, Path targetPath, boolean overwrite, MuleEvent event)
    {
        try
        {
            if (exists(targetPath))
            {
                if (overwrite)
                {
                    fileSystem.delete(targetPath.toString());
                }
                else
                {
                    alreadyExistsException(targetPath);
                }
            }

            client.rename(source.getPath(), targetPath.toString());
        }
        catch (Exception e)
        {
            throw exception(format("Found exception copying file '%s' to '%s'", source, targetPath), e);
        }
    }
}
