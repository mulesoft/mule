/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.api;

import org.mule.extension.ftp.internal.FtpFilePredicateBuilder;
import org.mule.extension.ftp.internal.ftp.connection.ClassicFtpConnectionProvider;
import org.mule.extension.ftp.internal.sftp.connection.SftpConnectionProvider;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.connector.Providers;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.module.extension.file.api.FileConnectorConfig;
import org.mule.runtime.module.extension.file.api.FilePredicateBuilder;
import org.mule.runtime.module.extension.file.api.StandardFileSystemOperations;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Allows manipulating files through the FTP, SFTP and FTPS protocols
 *
 * @since 4.0
 */
@Extension(name = "Ftp Connector", description = "Connector to manipulate Files on a FTP/SFTP server")
@Operations({StandardFileSystemOperations.class})
@SubTypeMapping(baseType = FilePredicateBuilder.class, subTypes = FtpFilePredicateBuilder.class)
@Providers({ClassicFtpConnectionProvider.class, SftpConnectionProvider.class})
public class FtpConnector implements FileConnectorConfig
{

    public static final String FTP_PROTOCOL = "ftp";

    @Inject
    private ConnectionManager connectionManager;

    /**
     * The directory to be considered as the root of every
     * relative path used with this connector. If not provided,
     * it will default to the remote server default.
     */
    @Parameter
    @Optional
    private String baseDir = null;

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
     * {@inheritDoc}
     */
    @Override
    public String getBaseDir()
    {
        return baseDir;
    }

    public Integer getConnectionTimeout()
    {
        return connectionTimeout;
    }

    public TimeUnit getConnectionTimeoutUnit()
    {
        return connectionTimeoutUnit;
    }

    public Integer getResponseTimeout()
    {
        return responseTimeout;
    }

    public TimeUnit getResponseTimeoutUnit()
    {
        return responseTimeoutUnit;
    }

    public ConnectionManager getConnectionManager()
    {
        return connectionManager;
    }
}
