/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.command;


import org.mule.api.MuleEvent;
import org.mule.extension.ftp.internal.FtpConnector;
import org.mule.extension.ftp.internal.FtpFileSystem;
import org.mule.module.extension.file.api.FileAttributes;
import org.mule.module.extension.file.api.FileSystem;

import java.nio.file.Path;

import org.apache.commons.net.ftp.FTPClient;

/**
 * Base class for commands that generates copies of a file in a FTP server,
 * either by copying or moving them.
 * <p>
 * This class contains the logic to determine the actual target path
 * in a way which provides bash semantics, as described in the
 * {@link FileSystem#copy(String, String, boolean, boolean, MuleEvent)}
 * and {@link FileSystem#move(String, String, boolean, boolean)} methods.
 * <p>
 * This command also handles the concern of the target path already
 * existing and whether or not overwrite it.
 *
 * @since 4.0
 */
abstract class AbstractFtpCopyCommand extends FtpCommand
{

    /**
     * {@inheritDoc}
     */
    AbstractFtpCopyCommand(FtpFileSystem fileSystem, FtpConnector config, FTPClient client)
    {
        super(fileSystem, config, client);
    }

    /**
     * Performs the base logic and delegates into {@link #doExecute(FileAttributes, Path, boolean, MuleEvent)}
     * to perform the actual copying logic
     *
     * @param sourcePath            the path to be copied
     * @param target                the path to the target destination
     * @param overwrite             whether to overwrite existing target paths
     * @param createParentDirectory whether to create the target's parent directory if it doesn't exists
     * @param event                 the {@link MuleEvent} which triggered this operation
     */
    protected final void execute(String sourcePath, String target, boolean overwrite, boolean createParentDirectory, MuleEvent event)
    {
        FileAttributes sourceFile = getExistingFile(sourcePath);
        Path targetPath = resolvePath(target);
        FileAttributes targetFile = getFile(targetPath.toString());

        if (targetFile != null)
        {
            if (targetFile.isDirectory())
            {
                if (sourceFile.isDirectory() && sourceFile.getName().equals(targetFile.getName()) && !overwrite)
                {
                    throw alreadyExistsException(targetPath);
                }
                else
                {
                    targetPath = targetPath.resolve(sourceFile.getName());
                }
            }
            else if (!overwrite)
            {
                throw alreadyExistsException(targetPath);
            }
        }
        else
        {
            targetFile = getFile(targetPath.getParent().toString());
            if (targetFile != null)
            {
                targetPath = targetPath.getParent().resolve(targetPath.getFileName());
            }
            else
            {
                if (createParentDirectory)
                {
                    mkdirs(targetPath);
                    targetPath = targetPath.resolve(sourceFile.getName());
                }
                else
                {
                    throw new IllegalArgumentException(String.format("Can't copy '%s' to '%s' because the destination path " +
                                                                     "doesn't exists",
                                                                     sourceFile.getPath(), targetPath.toAbsolutePath()));
                }
            }
        }

        final String cwd = getCurrentWorkingDirectory();
        doExecute(sourceFile, targetPath, overwrite, event);
        changeWorkingDirectory(cwd);
    }

    /**
     * Implement this method with the corresponding copying logic
     *
     * @param source     the {@link FileAttributes} for the file to be copied
     * @param targetPath the {@link Path} to the target destination
     * @param overwrite  whether to overwrite existing target paths
     * @param event      the {@link MuleEvent} which triggered this operation
     */
    protected abstract void doExecute(FileAttributes source, Path targetPath, boolean overwrite, MuleEvent event);
}
