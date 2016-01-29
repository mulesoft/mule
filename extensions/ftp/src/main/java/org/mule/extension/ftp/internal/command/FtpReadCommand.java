/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.command;

import org.mule.DefaultMuleMessage;
import org.mule.api.metadata.DataType;
import org.mule.api.temporary.MuleMessage;
import org.mule.extension.ftp.internal.FtpConnector;
import org.mule.extension.ftp.internal.FtpFilePayload;
import org.mule.extension.ftp.internal.FtpFileSystem;
import org.mule.module.extension.file.api.FilePayload;
import org.mule.module.extension.file.api.command.ReadCommand;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.net.ftp.FTPClient;

/**
 * A {@link FtpCommand} which implements the {@link FtpReadCommand}
 *
 * @since 4.0
 */
public final class FtpReadCommand extends FtpCommand implements ReadCommand
{

    /**
     * {@inheritDoc}
     */
    public FtpReadCommand(FtpFileSystem fileSystem, FtpConnector config, FTPClient client)
    {
        super(fileSystem, config, client);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MuleMessage read(MuleMessage message, String filePath, boolean lock)
    {
        FtpFilePayload filePayload = getExistingFile(filePath);
        if (filePayload.isDirectory())
        {
            throw cannotReadDirectoryException(Paths.get(filePayload.getPath()));
        }

        try
        {
            filePayload = new FtpFilePayload(resolvePath(filePath), client.listFiles(filePath)[0], config);
        }
        catch (Exception e)
        {
            throw exception("Found exception while trying to list path " + filePath, e);
        }

        Path path = Paths.get(filePayload.getPath());

        if (lock)
        {
            filePayload = new FtpFilePayload(filePayload, fileSystem.lock(path));
        }
        else
        {
            fileSystem.verifyNotLocked(path);
        }

        DataType<FilePayload> updatedDataType = fileSystem.updateDataType(message.getDataType(), filePayload);
        return new DefaultMuleMessage(filePayload, updatedDataType);
    }
}
