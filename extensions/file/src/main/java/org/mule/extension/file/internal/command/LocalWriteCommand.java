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
import org.mule.extension.file.internal.LocalFileSystem;
import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.extension.file.api.FileConnectorConfig;
import org.mule.runtime.module.extension.file.api.FileContentWrapper;
import org.mule.runtime.module.extension.file.api.FileWriteMode;
import org.mule.runtime.module.extension.file.api.FileWriterVisitor;
import org.mule.runtime.module.extension.file.api.command.WriteCommand;
import org.mule.runtime.module.extension.file.api.lock.NullPathLock;
import org.mule.runtime.module.extension.file.api.lock.PathLock;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
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

    private final MuleContext muleContext;

    /**
     * {@inheritDoc}
     */
    public LocalWriteCommand(LocalFileSystem fileSystem, MuleContext muleContext)
    {
        super(fileSystem);
        this.muleContext = muleContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(FileConnectorConfig config,
                      String filePath,
                      Object content,
                      FileWriteMode mode,
                      MuleEvent event,
                      boolean lock,
                      boolean createParentDirectory,
                      String encoding)
    {
        Path path = resolvePath(config, filePath);
        assureParentFolderExists(config, path, createParentDirectory);

        final OpenOption[] openOptions = getOpenOptions(mode);
        PathLock pathLock = lock ? fileSystem.lock(path, openOptions) : new NullPathLock();

        try (OutputStream out = getOutputStream(path, openOptions, mode))
        {
            new FileContentWrapper(content, event, muleContext).accept(new FileWriterVisitor(out, event, encoding));
        }
        catch (AccessDeniedException e)
        {
            throw exception(format("Could not write to file '%s' because access was denied by the operating system", path), e);
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
}
