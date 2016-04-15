/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.internal.command;

import static java.lang.String.format;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.extension.file.api.FileConnector;
import org.mule.extension.file.api.LocalFileSystem;
import org.mule.module.extension.file.api.FileSystem;

import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Base class for commands that generates copies of a local file, either
 * by copying or moving them.
 * <p>
 * This class contains the logic to determine the actual target path
 * in a way which provides bash semantics, as described in the
 * {@link FileSystem#copy(String, String, boolean, boolean, MuleEvent)}
 * and {@link FileSystem#move(String, String, boolean, boolean)} methods.
 * <p>
 * This command also handles the concern of the target path already
 * existing and whether or not overwrite it.
 *
 * @since 4.0
 */
abstract class AbstractLocalCopyCommand extends LocalFileCommand
{

    /**
     * {@inheritDoc}
     */
    AbstractLocalCopyCommand(LocalFileSystem fileSystem, FileConnector config)
    {
        super(fileSystem, config);
    }

    /**
     * Performs the base logic and delegates into {@link #doExecute(Path, Path, boolean, CopyOption[])} to perform
     * the actual copying logic
     *
     * @param sourcePath            the path to be copied
     * @param target                the path to the target destination
     * @param overwrite             whether to overwrite existing target paths
     * @param createParentDirectory whether to create the target's parent directory if it doesn't exists
     */
    protected final void execute(String sourcePath, String target, boolean overwrite, boolean createParentDirectory)
    {
        Path source = resolveExistingPath(sourcePath);
        Path targetPath = resolvePath(target);

        CopyOption copyOption = null;
        if (Files.exists(targetPath))
        {
            if (Files.isDirectory(targetPath))
            {
                if (Files.isDirectory(source) && source.getFileName().equals(targetPath.getFileName()) && !overwrite)
                {
                    throw alreadyExistsException(targetPath);
                }
                else
                {
                    targetPath = targetPath.resolve(source.getFileName());
                }
            }
            else if (overwrite)
            {
                copyOption = StandardCopyOption.REPLACE_EXISTING;
            }
            else
            {
                throw alreadyExistsException(targetPath);
            }
        }
        else
        {
            if (Files.exists(targetPath.getParent()))
            {
                targetPath = targetPath.getParent().resolve(targetPath.getFileName());
            }
            else
            {
                if (createParentDirectory)
                {
                    targetPath.toFile().mkdirs();
                    targetPath = targetPath.resolve(source.getFileName());
                }
                else
                {
                    throw new IllegalArgumentException(format("Can't copy '%s' to '%s' because the destination path " +
                                                              "doesn't exists",
                                                              source.toAbsolutePath(), targetPath.toAbsolutePath()));
                }
            }
        }

        doExecute(source,
                  targetPath,
                  overwrite,
                  copyOption != null ? new CopyOption[] {copyOption} : new CopyOption[] {});
    }

    /**
     * Implement this method with the corresponding copying logic
     *
     * @param source     the path to be copied
     * @param targetPath the path to the target destination
     * @param overwrite  whether to overwrite existing target paths
     * @param options    an array of {@link CopyOption} which configure the copying operation
     */
    protected abstract void doExecute(Path source, Path targetPath, boolean overwrite, CopyOption[] options);
}
