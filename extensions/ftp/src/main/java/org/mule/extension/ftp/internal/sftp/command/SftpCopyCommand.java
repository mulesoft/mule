/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.sftp.command;

import org.mule.extension.ftp.api.FtpConnector;
import org.mule.extension.ftp.api.ftp.FtpFileSystem;
import org.mule.extension.ftp.api.sftp.SftpFileSystem;
import org.mule.extension.ftp.internal.AbstractFtpCopyDelegate;
import org.mule.extension.ftp.internal.ftp.command.FtpCommand;
import org.mule.extension.ftp.internal.sftp.connection.SftpClient;
import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.module.extension.file.api.FileAttributes;
import org.mule.runtime.module.extension.file.api.command.CopyCommand;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A {@link SftpCommand} which implements the {@link CopyCommand} contract
 *
 * @since 4.0
 */
public class SftpCopyCommand extends SftpCommand implements CopyCommand
{

    /**
     * {@inheritDoc}
     */
    public SftpCopyCommand(SftpFileSystem fileSystem, FtpConnector config, SftpClient client)
    {
        super(fileSystem, config, client);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copy(String sourcePath, String targetPath, boolean overwrite, boolean createParentDirectories, MuleEvent event)
    {
        copy(sourcePath, targetPath, overwrite, createParentDirectories, event, new SftpCopyDelegate(config, this, fileSystem));
    }

    private class SftpCopyDelegate extends AbstractFtpCopyDelegate
    {

        public SftpCopyDelegate(FtpConnector config, FtpCommand command, FtpFileSystem fileSystem)
        {
            super(config, command, fileSystem);
        }

        @Override
        protected void copyDirectory(Path sourcePath, Path target, boolean overwrite, FtpFileSystem writerConnection, MuleEvent event)
        {
            for (FileAttributes fileAttributes : client.list(sourcePath.toString()))
            {
                if (isVirtualDirectory(fileAttributes.getName()))
                {
                    continue;
                }

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
    }
}
