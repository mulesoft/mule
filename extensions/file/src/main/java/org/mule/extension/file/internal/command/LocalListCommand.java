/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.internal.command;

import org.mule.extension.file.api.FileConnector;
import org.mule.extension.file.api.LocalFilePayload;
import org.mule.extension.file.api.LocalFileSystem;
import org.mule.module.extension.file.api.FilePayload;
import org.mule.module.extension.file.api.command.ListCommand;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

/**
 * A {@link LocalFileCommand} which implements the {@link ListCommand}
 *
 * @since 4.0
 */
public final class LocalListCommand extends LocalFileCommand implements ListCommand
{
    /**
     * {@inheritDoc}
     */
    public LocalListCommand(LocalFileSystem fileSystem, FileConnector config)
    {
        super(fileSystem, config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FilePayload> list(String directoryPath, boolean recursive, Predicate<FilePayload> matcher)
    {
        Path path = resolveExistingPath(directoryPath);
        if (!Files.isDirectory(path))
        {
            throw cannotListFileException(path);
        }

        List<FilePayload> accumulator = new LinkedList<>();
        doList(path.toFile(), accumulator, recursive, matcher);

        return accumulator;
    }

    private void doList(File parent, List<FilePayload> accumulator, boolean recursive, Predicate<FilePayload> matcher)
    {
        for (File child : parent.listFiles())
        {
            FilePayload payload = new LocalFilePayload(child.toPath());
            if (!matcher.test(payload))
            {
                continue;
            }

            accumulator.add(payload);
            if (child.isDirectory() && recursive)
            {
                doList(child, accumulator, recursive, matcher);
            }
        }
    }
}
