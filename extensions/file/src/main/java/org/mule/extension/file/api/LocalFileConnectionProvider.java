/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.api;

import org.mule.api.connection.ConnectionHandlingStrategy;
import org.mule.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.api.connection.ConnectionProvider;
import org.mule.api.connection.ConnectionValidationResult;
import org.mule.module.extension.file.api.FileSystem;

/**
 * A {@link ConnectionProvider} which provides instances of
 * {@link FileSystem} from instances of {@link FileConnector}
 *
 * @since 4.0
 */
public final class LocalFileConnectionProvider implements ConnectionProvider<FileConnector, FileSystem>
{

    /**
     * Creates and returns a new instance of {@link LocalFileSystem}
     *
     * @param fileConnector the {@link FileConnector} which parametrizes the return value
     * @return a {@link LocalFileSystem}
     */
    @Override
    public FileSystem connect(FileConnector fileConnector)
    {
        return new LocalFileSystem(fileConnector);
    }

    /**
     * Does nothing since {@link LocalFileSystem} instances do not
     * require disconnecting
     *
     * @param localFileSystem a {@link LocalFileSystem} instance
     */
    @Override
    public void disconnect(FileSystem localFileSystem)
    {
        // no-op
    }

    @Override
    public ConnectionValidationResult validate(FileSystem fileSystem)
    {
        return ConnectionValidationResult.success();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionHandlingStrategy<FileSystem> getHandlingStrategy(ConnectionHandlingStrategyFactory<FileConnector, FileSystem> handlingStrategyFactory)
    {
        return handlingStrategyFactory.cached();
    }
}
