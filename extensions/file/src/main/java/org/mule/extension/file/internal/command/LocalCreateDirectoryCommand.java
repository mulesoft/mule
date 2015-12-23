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
import org.mule.module.extension.file.api.command.CreateDirectoryCommand;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang.StringUtils;

/**
 * A {@link LocalFileCommand} which implements the {@link CreateDirectoryCommand} contract
 *
 * @since 4.0
 */
public final class LocalCreateDirectoryCommand extends LocalFileCommand implements CreateDirectoryCommand
{

    /**
     * {@inheritDoc}
     */
    public LocalCreateDirectoryCommand(LocalFileSystem fileSystem, FileConnector config)
    {
        super(fileSystem, config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createDirectory(String basePath, String directoryName)
    {
        if (StringUtils.isBlank(basePath))
        {
            basePath = config.getBaseDir();
        }

        Path base = resolveExistingPath(basePath);
        Path target = base.resolve(directoryName).toAbsolutePath();

        if (Files.exists(target))
        {
            throw new IllegalArgumentException(format("Directory '%s' already exists", target));
        }

        createDirectory(target.toFile());
    }
}
