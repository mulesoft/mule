/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.file.api;

import static java.lang.String.format;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.temporary.MuleMessage;
import org.mule.runtime.module.extension.file.api.command.CopyCommand;
import org.mule.runtime.module.extension.file.api.command.CreateDirectoryCommand;
import org.mule.runtime.module.extension.file.api.command.DeleteCommand;
import org.mule.runtime.module.extension.file.api.command.ListCommand;
import org.mule.runtime.module.extension.file.api.command.MoveCommand;
import org.mule.runtime.module.extension.file.api.command.ReadCommand;
import org.mule.runtime.module.extension.file.api.command.RenameCommand;
import org.mule.runtime.module.extension.file.api.command.WriteCommand;
import org.mule.runtime.module.extension.file.api.lock.PathLock;
import org.mule.runtime.core.transformer.types.DataTypeFactory;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Predicate;

import javax.activation.MimetypesFileTypeMap;

/**
 * Base class for implementations of {@link FileSystem}
 *
 * @since 4.0
 */
public abstract class AbstractFileSystem implements FileSystem
{

    private final MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();

    /**
     * @return a {@link ListCommand}
     */
    protected abstract ListCommand getListCommand();

    /**
     * @return a {@link ReadCommand}
     */
    protected abstract ReadCommand getReadCommand();

    /**
     * @return a {@link WriteCommand}
     */
    protected abstract WriteCommand getWriteCommand();

    /**
     * @return a {@link CopyCommand}
     */
    protected abstract CopyCommand getCopyCommand();

    /**
     * @return a {@link MoveCommand}
     */
    protected abstract MoveCommand getMoveCommand();

    /**
     * @return a {@link DeleteCommand}
     */
    protected abstract DeleteCommand getDeleteCommand();

    /**
     * @return a {@link RenameCommand}
     */
    protected abstract RenameCommand getRenameCommand();

    /**
     * @return a {@link CreateDirectoryCommand}
     */
    protected abstract CreateDirectoryCommand getCreateDirectoryCommand();

    /**
     * {@inheritDoc}
     */
    @Override
    public TreeNode list(String directoryPath, boolean recursive, MuleMessage<?, ?> message, Predicate<FileAttributes> matcher)
    {
        return getListCommand().list(directoryPath, recursive, message, matcher);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MuleMessage<InputStream, FileAttributes> read(MuleMessage<?, ?> message, String filePath, boolean lock)
    {
        return getReadCommand().read(message, filePath, lock);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(String filePath, Object content, FileWriteMode mode, MuleEvent event, boolean lock, boolean createParentDirectories)
    {
        getWriteCommand().write(filePath, content, mode, event, lock, createParentDirectories);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copy(String sourcePath, String targetDirectory, boolean overwrite, boolean createParentDirectories, MuleEvent event)
    {
        getCopyCommand().copy(sourcePath, targetDirectory, overwrite, createParentDirectories, event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(String sourcePath, String targetDirectory, boolean overwrite, boolean createParentDirectories)
    {
        getMoveCommand().move(sourcePath, targetDirectory, overwrite, createParentDirectories);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String filePath)
    {
        getDeleteCommand().delete(filePath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rename(String filePath, String newName)
    {
        getRenameCommand().rename(filePath, newName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createDirectory(String basePath, String directoryName)
    {
        getCreateDirectoryCommand().createDirectory(basePath, directoryName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final PathLock lock(Path path, Object... params)
    {
        PathLock lock = createLock(path, params);
        if (!lock.tryLock())
        {
            throw new IllegalStateException(format("Could not lock file '%s' because it's already owned by another process", path));
        }

        return lock;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataType<InputStream> getFileMessageDataType(DataType<?> originalDataType, FileAttributes attributes)
    {
        DataType<InputStream> newDataType = DataTypeFactory.create(InputStream.class);
        newDataType.setEncoding(originalDataType.getEncoding());

        String presumedMimeType = mimetypesFileTypeMap.getContentType(attributes.getPath());
        newDataType.setMimeType(presumedMimeType != null ? presumedMimeType : originalDataType.getMimeType());

        return newDataType;
    }

    /**
     * Try to acquire a lock on a file and release it immediately. Usually used as a
     * quick check to see if another process is still holding onto the file, e.g. a
     * large file (more than 100MB) is still being written to.
     */
    protected boolean isLocked(Path path)
    {
        PathLock lock = createLock(path);
        try
        {
            return !lock.tryLock();
        }
        finally
        {
            lock.release();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void verifyNotLocked(Path path)
    {
        if (isLocked(path))
        {
            throw new IllegalStateException(format("File '%s' is locked by another process", path));
        }
    }

    protected abstract PathLock createLock(Path path, Object... params);
}
