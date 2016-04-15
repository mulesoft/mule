/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.internal.command;

import static java.lang.String.format;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.extension.file.api.FileConnector;
import org.mule.extension.file.api.LocalFileSystem;
import org.mule.runtime.module.extension.file.api.FileWriteMode;
import org.mule.runtime.module.extension.file.api.lock.PathLock;
import org.mule.runtime.module.extension.file.api.command.WriteCommand;
import org.mule.runtime.module.extension.file.api.FileContentWrapper;
import org.mule.runtime.module.extension.file.api.FileWriterVisitor;
import org.mule.runtime.module.extension.file.api.lock.NullPathLock;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * A {@link LocalFileCommand} which implements the {@link WriteCommand} contract
 *
 * @since 4.0
 */
public final class LocalWriteCommand extends LocalFileCommand implements WriteCommand
{

    /**
     * {@inheritDoc}
     */
    public LocalWriteCommand(LocalFileSystem fileSystem, FileConnector config)
    {
        super(fileSystem, config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(String filePath, Object content, FileWriteMode mode, MuleEvent event, boolean lock, boolean createParentDirectory)
    {
        Path path = resolvePath(filePath);
        assureParentFolderExists(path, createParentDirectory);

        final OpenOption[] openOptions = getOpenOptions(mode);
        PathLock pathLock = lock ? fileSystem.lock(path, openOptions) : new NullPathLock();

        try (OutputStream out = getOutputStream(path, openOptions, mode))
        {
            new FileContentWrapper(content, event).accept(new FileWriterVisitor(out, event));
        }
        catch (Exception e)
        {
            throw exception(format("Exception was found writing to file '%s'", path), e);
        }
        finally
        {
            pathLock.release();
        }
    }

    private OutputStream getOutputStream(Path path, OpenOption[] openOptions, FileWriteMode mode) throws IOException
    {
        try
        {
            return Files.newOutputStream(path, openOptions);
        }
        catch (FileAlreadyExistsException e)
        {
            throw new IllegalArgumentException(String.format("Cannot write to path '%s' because it already exists and write mode '%s' was selected. " +
                                                             "Use a different write mode or point to a path which doesn't exists", path, mode));
        }
    }

    private OpenOption[] getOpenOptions(FileWriteMode mode)
    {

        switch (mode)
        {
            case APPEND:
                return new OpenOption[] {CREATE, WRITE, StandardOpenOption.APPEND};
            case CREATE_NEW:
                return new OpenOption[] {WRITE, StandardOpenOption.CREATE_NEW};
            case OVERWRITE:
                return new OpenOption[] {CREATE, WRITE, TRUNCATE_EXISTING};
        }

        throw new IllegalArgumentException("Unsupported write mode " + mode);
    }

    private void assureParentFolderExists(Path path, boolean createParentFolder)
    {
        if (Files.exists(path))
        {
            return;
        }

        File parentFolder = path.getParent().toFile();
        if (!parentFolder.exists())
        {
            if (createParentFolder)
            {
                createDirectory(parentFolder);
            }
            else
            {
                throw new IllegalArgumentException(format("Cannot write to file '%s' because path to it doesn't exist. Consider setting the 'createParentFolder' attribute to 'true'", path));
            }
        }
    }
}
