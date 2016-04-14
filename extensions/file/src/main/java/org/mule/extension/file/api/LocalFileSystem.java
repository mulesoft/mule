/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.api;

import static java.nio.file.StandardOpenOption.WRITE;
import org.mule.extension.file.internal.command.LocalCopyCommand;
import org.mule.extension.file.internal.command.LocalCreateDirectoryCommand;
import org.mule.extension.file.internal.command.LocalDeleteCommand;
import org.mule.extension.file.internal.command.LocalListCommand;
import org.mule.extension.file.internal.command.LocalMoveCommand;
import org.mule.extension.file.internal.command.LocalReadCommand;
import org.mule.extension.file.internal.command.LocalRenameCommand;
import org.mule.extension.file.internal.command.LocalWriteCommand;
import org.mule.extension.file.internal.lock.LocalPathLock;
import org.mule.module.extension.file.api.FileAttributes;
import org.mule.module.extension.file.api.FileSystem;
import org.mule.module.extension.file.api.lock.PathLock;
import org.mule.module.extension.file.api.command.CopyCommand;
import org.mule.module.extension.file.api.command.CreateDirectoryCommand;
import org.mule.module.extension.file.api.command.DeleteCommand;
import org.mule.module.extension.file.api.command.ListCommand;
import org.mule.module.extension.file.api.command.MoveCommand;
import org.mule.module.extension.file.api.command.ReadCommand;
import org.mule.module.extension.file.api.command.RenameCommand;
import org.mule.module.extension.file.api.command.WriteCommand;
import org.mule.module.extension.file.api.AbstractFileSystem;
import org.mule.runtime.core.util.ArrayUtils;

import java.nio.file.OpenOption;
import java.nio.file.Path;

/**
 * Implementation of {@link FileSystem} for file systems
 * mounted on the host operating system.
 * <p>
 * Whenever the {@link FileSystem} contract refers to locking,
 * this implementation will resolve through a {@link LocalPathLock},
 * which produces file system level locks which rely on the host
 * operating system.
 * <p>
 * Also, for any method returning {@link FileAttributes} instances,
 * a {@link LocalFileAttributes} will be used.
 *
 * @since 4.0
 */
public final class LocalFileSystem extends AbstractFileSystem
{
    private final CopyCommand copyCommand;
    private final CreateDirectoryCommand createDirectoryCommand;
    private final DeleteCommand deleteCommand;
    private final ListCommand listCommand;
    private final MoveCommand moveCommand;
    private final ReadCommand readCommand;
    private final RenameCommand renameCommand;
    private final WriteCommand writeCommand;

    /**
     * Creates a new instance
     *
     * @param config a {@link FileConnector} which acts as a config
     */
    public LocalFileSystem(FileConnector config)
    {
        copyCommand = new LocalCopyCommand(this, config);
        createDirectoryCommand = new LocalCreateDirectoryCommand(this, config);
        deleteCommand = new LocalDeleteCommand(this, config);
        listCommand = new LocalListCommand(this, config);
        moveCommand = new LocalMoveCommand(this, config);
        readCommand = new LocalReadCommand(this, config);
        renameCommand = new LocalRenameCommand(this, config);
        writeCommand = new LocalWriteCommand(this, config);
    }

    @Override
    protected CopyCommand getCopyCommand()
    {
        return copyCommand;
    }

    @Override
    public CreateDirectoryCommand getCreateDirectoryCommand()
    {
        return createDirectoryCommand;
    }

    @Override
    protected DeleteCommand getDeleteCommand()
    {
        return deleteCommand;
    }

    @Override
    protected ListCommand getListCommand()
    {
        return listCommand;
    }

    @Override
    protected MoveCommand getMoveCommand()
    {
        return moveCommand;
    }

    @Override
    protected ReadCommand getReadCommand()
    {
        return readCommand;
    }

    @Override
    protected RenameCommand getRenameCommand()
    {
        return renameCommand;
    }

    @Override
    protected WriteCommand getWriteCommand()
    {
        return writeCommand;
    }

    @Override
    protected PathLock createLock(Path path, Object... params)
    {
        return new LocalPathLock(path, ArrayUtils.isEmpty(params)
                                       ? new OpenOption[] {WRITE}
                                       : (OpenOption[]) params);
    }
}
