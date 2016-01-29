/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.file.api.command;

import static java.lang.String.format;
import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.module.extension.file.api.FileConnectorConfig;
import org.mule.module.extension.file.api.FileSystem;

import java.nio.file.Path;
import java.util.function.Predicate;

/**
 * Base class for implementations of the Command design pattern which
 * performs operations on a file system
 *
 * @param <C> the generic type of the {@link FileConnectorConfig} which configures the operation
 * @param <F> the generic type of the {@link FileSystem} on which the operation is performed
 * @since 4.0
 */
public abstract class FileCommand<C extends FileConnectorConfig, F extends FileSystem>
{

    protected final F fileSystem;
    protected final C config;

    /**
     * Creates a new instance
     *
     * @param fileSystem the {@link FileSystem} on which the operation is performed
     * @param config     the config which configures the operation
     */
    protected FileCommand(F fileSystem, C config)
    {
        this.fileSystem = fileSystem;
        this.config = config;
    }

    /**
     * Returns true if the given {@code path} exists
     *
     * @param path the {@link Path} to test
     * @return whether the {@code path} exists
     */
    protected abstract boolean exists(Path path);

    /**
     * Returns an absolute {@link Path} to the given
     * {@code filePath}
     *
     * @param filePath the path to a file or directory
     * @return an absolute {@link Path}
     */
    protected abstract Path resolvePath(String filePath);

    /**
     * Similar to {@link #resolvePath(String)} only that it throws a
     * {@link IllegalArgumentException} if the given path doesn't exists.
     * <p>
     * The existence of the obtained path is verified by delegating into
     * {@link #exists(Path)}
     *
     * @param filePath the path to a file or directory
     * @return an absolute {@link Path}
     */
    protected Path resolveExistingPath(String filePath)
    {
        Path path = resolvePath(filePath);
        if (!exists(path))
        {
            throw pathNotFoundException(path);
        }

        return path;
    }

    /**
     * Returns a properly formatted {@link MuleRuntimeException}
     * for the given {@code message} and {@code cause}
     *
     * @param message the exception's message
     * @return a {@link RuntimeException}
     */
    protected RuntimeException exception(String message)
    {
        return new MuleRuntimeException(createStaticMessage(message));
    }

    /**
     * Returns a properly formatted {@link MuleRuntimeException}
     * for the given {@code message} and {@code cause}
     *
     * @param message the exception's message
     * @param cause   the exception's cause
     * @return {@link RuntimeException}
     */
    protected RuntimeException exception(String message, Exception cause)
    {
        return new MuleRuntimeException(createStaticMessage(message), cause);
    }

    /**
     * Returns an {@link IllegalArgumentException} explaining that
     * a {@link FileSystem#read(String, boolean, ContentMetadata)} operation
     * was attempted on a {@code path} pointing to a directory
     *
     * @param path the {@link Path} on which a read was attempted
     * @return {@link RuntimeException}
     */
    protected RuntimeException cannotReadDirectoryException(Path path)
    {
        return new IllegalArgumentException(format("Cannot read path '%s' since it's a directory", path));
    }

    /**
     * Returns a {@link IllegalArgumentException} explaining that
     * a {@link FileSystem#list(String, boolean, Predicate)} operation
     * was attempted on a {@code path} pointing to a file.
     *
     * @param path the {@link Path} on which a list was attempted
     * @return {@link RuntimeException}
     */
    protected RuntimeException cannotListFileException(Path path)
    {
        return new IllegalArgumentException(format("Cannot list path '%s' because it's a file. Only directories can be listed", path));
    }

    /**
     * Returns a {@link IllegalArgumentException} explaining that
     * a {@link FileSystem#list(String, boolean, Predicate)} operation
     * was attempted on a {@code path} pointing to a file.
     *
     * @param path the {@link Path} on which a list was attempted
     * @return {@link RuntimeException}
     */
    protected RuntimeException pathNotFoundException(Path path)
    {
        return new IllegalArgumentException(format("Path '%s' doesn't exists", path));
    }

    /**
     * Returns a {@link IllegalArgumentException} explaining that
     * an operation is trying to write to the given {@code path}
     * but it already exists and no overwrite instruction was provided.
     *
     * @param path the {@link Path} that the operation tried to modify
     * @return {@link RuntimeException}
     */
    protected IllegalArgumentException alreadyExistsException(Path path)
    {
        return new IllegalArgumentException(format("'%s' already exists. Set the 'overwrite' parameter to 'true' to perform the operation anyway", path));
    }
}
