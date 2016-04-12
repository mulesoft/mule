/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.api;

import org.mule.api.connector.ConnectionManager;
import org.mule.extension.api.annotation.Extension;
import org.mule.extension.api.annotation.Operations;
import org.mule.extension.api.annotation.Parameter;
import org.mule.extension.api.annotation.connector.Providers;
import org.mule.extension.api.annotation.param.Optional;
import org.mule.extension.ftp.internal.FtpListOperation;
import org.mule.module.extension.file.api.FileConnectorConfig;
import org.mule.module.extension.file.api.StandardFileSystemOperations;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * FTP connector used to manipulate files in a FTP server.
 * <p>
 * This class serves as both extension definition and configuration.
 * Operations are based on the standard {@link StandardFileSystemOperations}
 *
 * @since 4.0
 */
@Extension(name = "Ftp Connector", description = "Connector to manipulate Files on a FTP/SFTP server")
@Operations({StandardFileSystemOperations.class, FtpListOperation.class})
@Providers({FtpConnectionProvider.class})
public class FtpConnector implements FileConnectorConfig
{

    @Inject
    private ConnectionManager connectionManager;

    /**
     * The FTP server host, such as www.mulesoft.com, localhost, or 192.168.0.1, etc
     */
    @Parameter
    private String host;

    /**
     * The port number to connect on
     */
    @Parameter
    @Optional
    private Integer port;

    /**
     * The transfer mode to be used. Currently {@code BINARY}
     * and {@code ASCII} are supported.
     * <p>
     * Defaults to {@code BINARY}
     */
    @Parameter
    @Optional(defaultValue = "BINARY")
    private FtpTransferMode transferMode;

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
     * Whether to use passive mode. Set to {@code false} to
     * switch to active mode.
     * <p>
     * Defaults to {@code true}.
     */
    @Parameter
    @Optional(defaultValue = "true")
    private boolean passive = true;

    /**
     * The directory to be considered as the root of every
     * relative path used with this connector. If not provided,
     * it will default to the remote server default.
     */
    @Parameter
    @Optional
    private String baseDir = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBaseDir()
    {
        return baseDir;
    }

    String getHost()
    {
        return host;
    }

    Integer getPort()
    {
        return port;
    }

    Integer getConnectionTimeout()
    {
        return connectionTimeout;
    }

    TimeUnit getConnectionTimeoutUnit()
    {
        return connectionTimeoutUnit;
    }

    Integer getResponseTimeout()
    {
        return responseTimeout;
    }

    TimeUnit getResponseTimeoutUnit()
    {
        return responseTimeoutUnit;
    }

    FtpTransferMode getTransferMode()
    {
        return transferMode;
    }

    boolean isPassive()
    {
        return passive;
    }

    public ConnectionManager getConnectionManager()
    {
        return connectionManager;
    }
}
