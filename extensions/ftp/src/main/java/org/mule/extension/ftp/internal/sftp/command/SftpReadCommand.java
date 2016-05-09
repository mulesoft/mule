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
import org.mule.extension.ftp.internal.sftp.SftpInputStream;
import org.mule.extension.ftp.internal.sftp.connection.SftpClient;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.module.extension.file.api.FileAttributes;
import org.mule.runtime.module.extension.file.api.command.ReadCommand;
import org.mule.runtime.module.extension.file.api.lock.NullPathLock;
import org.mule.runtime.module.extension.file.api.lock.PathLock;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A {@link SftpCommand} which implements the {@link ReadCommand} contract
 *
 * @since 4.0
 */
public final class SftpReadCommand extends SftpCommand implements ReadCommand
{

    /**
     * {@inheritDoc}
     */
    public SftpReadCommand(SftpFileSystem fileSystem, FtpConnector config, SftpClient client)
    {
        super(fileSystem, config, client);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MuleMessage<InputStream, FileAttributes> read(MuleMessage<?, ?> message, String filePath, boolean lock)
    {
        FtpFileAttributes attributes = getExistingFile(filePath);
        if (attributes.isDirectory())
        {
            throw cannotReadDirectoryException(Paths.get(attributes.getPath()));
        }

        Path path = Paths.get(attributes.getPath());

        PathLock pathLock;
        if (lock)
        {
            pathLock = fileSystem.lock(path);
        }
        else
        {
            fileSystem.verifyNotLocked(path);
            pathLock = new NullPathLock();
        }

        try
        {
            return new DefaultMuleMessage(SftpInputStream.newInstance(config, attributes, pathLock),
                                          fileSystem.getFileMessageDataType(message.getDataType(), attributes),
                                          attributes).asNewMessage();
        }
        catch (ConnectionException e)
        {
            throw exception("Could not obtain connection to fetch file " + path, e);
        }
    }
}
