/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.internal.command;

import static java.lang.String.format;
import org.mule.extension.file.internal.LocalFileSystem;
import org.mule.runtime.core.util.FileUtils;
import org.mule.runtime.module.extension.file.api.FileConnectorConfig;
import org.mule.runtime.module.extension.file.api.command.DeleteCommand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link LocalFileCommand} which implements the {@link DeleteCommand} contract
 *
 * @since 4.0
 */
public final class LocalDeleteCommand extends LocalFileCommand implements DeleteCommand
{

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalDeleteCommand.class);

    /**
     * {@inheritDoc}
     */
    public LocalDeleteCommand(LocalFileSystem fileSystem)
    {
        super(fileSystem);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(FileConnectorConfig config, String filePath)
    {
        Path path = resolveExistingPath(config, filePath);

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Preparing to delete '{}'", path);
        }

        try
        {
            if (Files.isDirectory(path))
            {
                FileUtils.deleteTree(path.toFile());
            }
            else
            {
                fileSystem.verifyNotLocked(path);
                Files.deleteIfExists(path);
            }

            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("Successfully deleted '{}'", path);
            }
        }
        catch (IOException e)
        {
            throw exception(format("Could not delete '%s'", path), e);
        }
    }
}
