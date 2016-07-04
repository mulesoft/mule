/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal;

import org.mule.extension.ftp.internal.ftp.connection.FtpFileSystem;
import org.mule.extension.ftp.internal.ftp.connection.ClassicFtpFileSystem;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Base class for {@link ConnectionProvider} implementations which take a
 * {@link FtpConnector} as a config and provides a {@link FtpFileSystem}
 *
 * @param <Connection> the generic type of the connection object
 * @since 4.0
 */
public abstract class AbstractFtpConnectionProvider<Connection extends FtpFileSystem> implements ConnectionProvider<Connection>
{
    @Inject
    protected MuleContext muleContext;

    /**
     * The FTP server host, such as www.mulesoft.com, localhost, or 192.168.0.1, etc
     */
    @Parameter
    private String host;

    /**
     * A scalar value representing the amount of time to wait
     * before a connection attempt times out. This attribute
     * works in tandem with {@link #connectionTimeoutUnit}.
     * <p>
     * Defaults to {@code 10}
     */
    @Parameter
    @Optional(defaultValue = "10")
    private Integer connectionTimeout;

    /**
     * A {@link TimeUnit} which qualifies the {@link #connectionTimeout}
     * attribute.
     * <p>
     * Defaults to {@code SECONDS}
     */
    @Parameter
    @Optional(defaultValue = "SECONDS")
    private TimeUnit connectionTimeoutUnit;

    /**
     * A scalar value representing the amount of time to wait
     * before a request for data times out. This attribute
     * works in tandem with {@link #responseTimeoutUnit}.
     * <p>
     * Defaults to {@code 10}
     */
    @Parameter
    @Optional(defaultValue = "10")
    private Integer responseTimeout;

    /**
     * A {@link TimeUnit} which qualifies the {@link #responseTimeoutUnit}
     * attribute.
     * <p>
     * Defaults to {@code SECONDS}
     */
    @Parameter
    @Optional(defaultValue = "SECONDS")
    private TimeUnit responseTimeoutUnit;

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

    public void setHost(String host)
    {
        this.host = host;
    }

    protected Integer getConnectionTimeout()
    {
        return connectionTimeout;
    }

    protected TimeUnit getConnectionTimeoutUnit()
    {
        return connectionTimeoutUnit;
    }

    protected Integer getResponseTimeout()
    {
        return responseTimeout;
    }

    protected TimeUnit getResponseTimeoutUnit()
    {
        return responseTimeoutUnit;
    }

    public void setConnectionTimeout(Integer connectionTimeout)
    {
        this.connectionTimeout = connectionTimeout;
    }

    public void setConnectionTimeoutUnit(TimeUnit connectionTimeoutUnit)
    {
        this.connectionTimeoutUnit = connectionTimeoutUnit;
    }

    public void setResponseTimeout(Integer responseTimeout)
    {
        this.responseTimeout = responseTimeout;
    }

    public void setResponseTimeoutUnit(TimeUnit responseTimeoutUnit)
    {
        this.responseTimeoutUnit = responseTimeoutUnit;
    }
}
