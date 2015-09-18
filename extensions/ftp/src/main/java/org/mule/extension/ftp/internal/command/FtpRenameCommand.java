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
import org.mule.module.extension.file.api.command.RenameCommand;

import java.nio.file.Path;

import org.apache.commons.net.ftp.FTPClient;

/**
 * A {@link FtpCommand} which implements the {@link RenameCommand}
 *
 * @since 4.0
 */
public final class FtpRenameCommand extends FtpCommand implements RenameCommand
{

    /**
     * {@inheritDoc}
     */
    public FtpRenameCommand(FtpFileSystem fileSystem, FtpConnector config, FTPClient client)
    {
        super(fileSystem, config, client);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rename(String filePath, String newName)
    {
        Path source = resolveExistingPath(filePath);
        Path target = source.getParent().resolve(newName);

        if (exists(target))
        {
            throw new IllegalArgumentException(format("'%s' cannot be renamed because '%s' already exists", source, target));
        }

        try
        {
            client.rename(source.toString(), target.toString());
        }
        catch (Exception e)
        {
            throw exception(format("Exception was found renaming '%s' to '%s'", source, newName), e);
        }
    }
}
