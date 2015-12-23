/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.internal.command;

import static java.lang.String.format;
import org.mule.extension.file.api.FileConnector;
import org.mule.extension.file.api.LocalFileSystem;
import org.mule.module.extension.file.api.command.RenameCommand;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * A {@link LocalFileCommand} which implements the {@link RenameCommand} contract
 *
 * @since 4.0
 */
public final class LocalRenameCommand extends LocalFileCommand implements RenameCommand
{

    /**
     * {@inheritDoc}
     */
    public LocalRenameCommand(LocalFileSystem fileSystem, FileConnector config)
    {
        super(fileSystem, config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rename(String filePath, String newName)
    {
        Path source = resolveExistingPath(filePath);
        Path target = source.getParent().resolve(newName);

        if (Files.exists(target))
        {
            throw new IllegalArgumentException(format("'%s' cannot be renamed because '%s' already exists", source, target));
        }

        try
        {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (Exception e)
        {
            throw exception(format("Exception was found renaming '%s' to '%s'", source, newName), e);
        }
    }
}
