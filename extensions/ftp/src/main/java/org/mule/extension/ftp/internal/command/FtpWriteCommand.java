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
import org.mule.module.extension.file.api.FileWriteMode;
import org.mule.module.extension.file.api.command.WriteCommand;
import org.mule.module.extension.file.api.FileContentWrapper;
import org.mule.module.extension.file.api.FileWriterVisitor;

import java.io.OutputStream;
import java.nio.file.Path;

import org.apache.commons.net.ftp.FTPClient;

/**
 * A {@link FtpCommand} which implements the {@link WriteCommand} contract
 *
 * @since 4.0
 */
public final class FtpWriteCommand extends FtpCommand implements WriteCommand
{

    /**
     * {@inheritDoc}
     */
    public FtpWriteCommand(FtpFileSystem fileSystem, FtpConnector config, FTPClient client)
    {
        super(fileSystem, config, client);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(String filePath, Object content, FileWriteMode mode, MuleEvent event, boolean lock, boolean createParentDirectory)
    {
        Path path = resolvePath(filePath);
        FileAttributes file = getFile(filePath);

        if (file == null)
        {
            assureParentFolderExists(path, createParentDirectory);
        }
        else
        {
            if (mode == FileWriteMode.CREATE_NEW)
            {
                throw new IllegalArgumentException(String.format("Cannot write to path '%s' because it already exists and write mode '%s' was selected. " +
                                                                 "Use a different write mode or point to a path which doesn't exists", path, mode));
            }
            else if (mode == FileWriteMode.OVERWRITE)
            {
                fileSystem.delete(file.getPath());
            }
        }

        try (OutputStream outputStream = getOutputStream(path.toString(), mode))
        {
            new FileContentWrapper(content, event).accept(new FileWriterVisitor(outputStream, event));
        }
        catch (Exception e)
        {
            throw exception(format("Exception was found writing to file '%s'", path), e);
        }
        finally
        {
            fileSystem.awaitCommandCompletion();
        }
    }

    private OutputStream getOutputStream(String path, FileWriteMode mode)
    {
        try
        {
            return mode == FileWriteMode.APPEND
                   ? client.appendFileStream(path)
                   : client.storeFileStream(path);
        }
        catch (Exception e)
        {
            throw exception(String.format("Could not open stream to write to path '%s' using mode '%s'", path, mode), e);
        }
    }

    private void assureParentFolderExists(Path filePath, boolean createParentFolder)
    {
        Path parentFolderPath = filePath.getParent();
        if (parentFolderPath == null)
        {
            return;
        }

        FileAttributes parentFolder = getFile(parentFolderPath.toString());
        if (parentFolder == null)
        {
            if (createParentFolder)
            {
                mkdirs(parentFolderPath);
            }
            else
            {
                throw new IllegalArgumentException(format("Cannot write to file '%s' because path to it doesn't exist. Consider setting the 'createParentFolder' attribute to 'true'", filePath));
            }
        }
    }
}
