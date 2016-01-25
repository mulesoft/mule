/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.command;

import static java.lang.String.format;
import org.mule.extension.ftp.internal.FtpConnector;
import org.mule.extension.ftp.internal.FtpFileSystem;
import org.mule.module.extension.file.api.FileAttributes;
import org.mule.module.extension.file.api.command.CreateDirectoryCommand;

import java.nio.file.Paths;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.ftp.FTPClient;

/**
 * A {@link FtpCommand} which implements the {@link CreateDirectoryCommand}
 *
 * @since 4.0
 */
public final class FtpCreateDirectoryCommand extends FtpCommand implements CreateDirectoryCommand
{

    /**
     * {@inheritDoc}
     */
    public FtpCreateDirectoryCommand(FtpFileSystem fileSystem, FtpConnector config, FTPClient client)
    {
        super(fileSystem, config, client);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createDirectory(String basePath, String directoryName)
    {
        if (!StringUtils.isBlank(basePath))
        {
            changeWorkingDirectory(basePath);
        }

        FileAttributes targetFile = getFile(directoryName);

        if (targetFile != null)
        {
            throw new IllegalArgumentException(format("Directory '%s' already exists", directoryName));
        }

        mkdirs(Paths.get(config.getBaseDir()).resolve(directoryName));
    }
}
