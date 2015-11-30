/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.internal;

import static org.mule.extension.file.internal.LocalFileSystem.exception;
import org.mule.util.FileUtils;

import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Specialization of the {@link CopyCommand} class which moves
 * instead of copying
 *
 * @since 4.0
 */
final class MoveCommand extends CopyCommand
{

    /**
     * Creates a new instance
     *
     * @param source                the path of the file to be copied
     * @param targetPath            the path to which the file is to be copied
     * @param overwrite             whether or not overwrite the target file if it already exists
     * @param createParentDirectory whether or not to created the target's parent directory if it doesn't exists
     */
    MoveCommand(Path source, Path targetPath, boolean overwrite, boolean createParentDirectory)
    {
        super(source, targetPath, overwrite, createParentDirectory);
    }

    @Override
    protected void doExecute(CopyOption[] options)
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
                        alreadyExistsException();
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
            throw exception(String.format("Found exception copying file '%s' to '%s'", source, targetPath), e);
        }
    }
}
