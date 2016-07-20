/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.internal;

import org.mule.runtime.api.connection.ConnectionHandlingStrategy;
import org.mule.runtime.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.module.extension.file.api.FileSystem;

import javax.inject.Inject;

/**
 * A {@link ConnectionProvider} which provides instances of
 * {@link FileSystem} from instances of {@link FileConnector}
 *
 * @since 4.0
 */
@DisplayName("Local FileSystem Connection")
public final class LocalFileConnectionProvider implements ConnectionProvider<FileSystem>
{
    @Inject
    private MuleContext muleContext;

    /**
     * Creates and returns a new instance of {@link LocalFileSystem}
     *
     * @return a {@link LocalFileSystem}
     */
    @Override
    public FileSystem connect()
    {
        return new LocalFileSystem(muleContext);
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
    public ConnectionHandlingStrategy<FileSystem> getHandlingStrategy(ConnectionHandlingStrategyFactory<FileSystem> handlingStrategyFactory)
    {
        return handlingStrategyFactory.cached();
    }
}
