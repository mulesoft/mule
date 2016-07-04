/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.internal.command;

import org.mule.extension.file.internal.LocalFileSystem;
import org.mule.runtime.core.util.FileUtils;
import org.mule.runtime.module.extension.file.api.FileConnectorConfig;
import org.mule.runtime.module.extension.file.api.command.MoveCommand;

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
    public LocalMoveCommand(LocalFileSystem fileSystem)
    {
        super(fileSystem);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(FileConnectorConfig config, String sourcePath, String targetDirectory, boolean overwrite, boolean createParentDirectories)
    {
        execute(config, sourcePath, targetDirectory, overwrite, createParentDirectories);
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
    protected void doExecute(Path source, Path targetPath, boolean overwrite, CopyOption[] options) throws Exception
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getAction()
    {
        return "moving";
    }
}
