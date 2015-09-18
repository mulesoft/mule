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
import org.mule.module.extension.file.api.command.MoveCommand;
import org.mule.util.FileUtils;

import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A {@link AbstractLocalCopyCommand} which implements the {@link MoveCommand} contract
 *
 * @since 4.0
 */
public final class LocalMoveCommand extends AbstractLocalCopyCommand implements MoveCommand
{

    /**
     * {@inheritDoc}
     */
    public LocalMoveCommand(LocalFileSystem fileSystem, FileConnector config)
    {
        super(fileSystem, config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(String sourcePath, String targetDirectory, boolean overwrite, boolean createParentDirectory)
    {
        execute(sourcePath, targetDirectory, overwrite, createParentDirectory);
    }

    /**
     * Implements recursive moving
     *
     * @param source     the path to be copied
     * @param targetPath the path to the target destination
     * @param overwrite  whether to overwrite existing target paths
     * @param options    an array of {@link CopyOption} which configure the copying operation
     */
    @Override
    protected void doExecute(Path source, Path targetPath, boolean overwrite, CopyOption[] options)
    {
        try
        {
            if (Files.isDirectory(source))
            {
                if (Files.exists(targetPath))
                {
                    if (overwrite)
                    {
                        FileUtils.deleteTree(targetPath.toFile());
                    }
                    else
                    {
                        alreadyExistsException(targetPath);
                    }
                }
                FileUtils.moveDirectory(source.toFile(), targetPath.toFile());
            }
            else
            {
                Files.move(source, targetPath, options);
            }
        }
        catch (Exception e)
        {
            throw exception(format("Found exception copying file '%s' to '%s'", source, targetPath), e);
        }
    }
}
