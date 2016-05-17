/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal;

import org.mule.extension.ftp.api.FtpConnector;
import org.mule.extension.ftp.api.ftp.FtpFileSystem;
import org.mule.extension.ftp.internal.ftp.connection.ClassicFtpFileSystem;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.annotation.Parameter;

import javax.inject.Inject;

/**
 * Base class for {@link ConnectionProvider} implementations which take a
 * {@link FtpConnector} as a config and provides a {@link FtpFileSystem}
 *
 * @param <Config>     the generic type of the config object
 * @param <Connection> the generic type of the connection object
 * @since 4.0
 */
public abstract class AbstractFtpConnectionProvider<Config extends FtpConnector, Connection extends FtpFileSystem>
        implements ConnectionProvider<Config, Connection>
{
    @Inject
    protected MuleContext muleContext;

    /**
     * The FTP server host, such as www.mulesoft.com, localhost, or 192.168.0.1, etc
     */
    @Parameter
    private String host;

    /**
     * Invokes the {@link ClassicFtpFileSystem#disconnect()} method
     * on the given {@code ftpFileSystem}
     *
     * @param ftpFileSystem a {@link ClassicFtpFileSystem} instance
     */
    @Override
    public void disconnect(Connection ftpFileSystem)
    {
        ftpFileSystem.disconnect();
    }

    /**
     * Validates the connection by delegating into {@link FtpFileSystem#validateConnection()}
     *
     * @param ftpFileSystem the connection to validate
     * @return a {@link ConnectionValidationResult}
     */
    @Override
    public ConnectionValidationResult validate(Connection ftpFileSystem)
    {
        return ftpFileSystem.validateConnection();
    }

    protected String getHost()
    {
        return host;
    }
}
