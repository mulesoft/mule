/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.api;

import org.mule.api.connection.ConnectionException;
import org.mule.api.connection.ConnectionHandlingStrategy;
import org.mule.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.api.connection.ConnectionProvider;
import org.mule.api.connection.ConnectionValidationResult;
import org.mule.api.connection.PoolingListener;
import org.mule.extension.api.annotation.Parameter;
import org.mule.extension.api.annotation.param.display.Password;
import org.mule.module.extension.file.api.FileSystem;

import java.io.IOException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

/**
 * A {@link ConnectionProvider} which provides instances of
 * {@link FileSystem} from instances of {@link FtpConnector}
 *
 * @since 4.0
 */
public final class FtpConnectionProvider implements ConnectionProvider<FtpConnector, FtpFileSystem>
{

    /**
     * If the FTP server is authenticated, this is the username used for authentication
     */
    @Parameter
    private String username;

    /**
     * The password for the user being authenticated.
     */
    @Parameter
    @Password
    private String password;

    /**
     * Creates and returns a new instance of {@link FtpFileSystem}
     *
     * @param config the {@link FtpConnector} which parametrizes the return value
     * @return a {@link FtpFileSystem}
     */
    @Override
    public FtpFileSystem connect(FtpConnector config) throws ConnectionException
    {
        return new FtpFileSystem(config, createClient(config));
    }

    /**
     * Invokes the {@link FtpFileSystem#disconnect()} method
     * on the given {@code ftpFileSystem}
     *
     * @param ftpFileSystem a {@link FtpFileSystem} instance
     */
    @Override
    public void disconnect(FtpFileSystem ftpFileSystem)
    {
        ftpFileSystem.disconnect();
    }

    //TODO: MULE-9291 Add the proper connection validation
    @Override
    public ConnectionValidationResult validate(FtpFileSystem ftpFileSystem)
    {
        return ConnectionValidationResult.success();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionHandlingStrategy<FtpFileSystem> getHandlingStrategy(ConnectionHandlingStrategyFactory<FtpConnector, FtpFileSystem> handlingStrategyFactory)
    {
        return handlingStrategyFactory.supportsPooling(new PoolingListener<FtpConnector, FtpFileSystem>()
        {
            @Override
            public void onBorrow(FtpConnector ftpConnector, FtpFileSystem ftpFileSystem)
            {
                ftpFileSystem.changeToBaseDir();
                ftpFileSystem.setTransferMode(ftpConnector.getTransferMode());
                ftpFileSystem.setResponseTimeout(ftpConnector.getResponseTimeout(), ftpConnector.getResponseTimeoutUnit());
                ftpFileSystem.setPassiveMode(ftpConnector.isPassive());
            }

            @Override
            public void onReturn(FtpConnector ftpConnector, FtpFileSystem ftpFileSystem)
            {
            }
        });
    }

    private FTPClient createClient(FtpConnector config) throws ConnectionException
    {
        FTPClient client = new FTPClient();
        if (config.getConnectionTimeout() != null && config.getConnectionTimeoutUnit() != null)
        {
            client.setConnectTimeout(new Long(config.getConnectionTimeoutUnit().toMillis(config.getConnectionTimeout())).intValue());
        }

        try
        {
            if (config.getPort() != null)
            {
                client.connect(config.getHost(), config.getPort());
            }
            else
            {
                client.connect(config.getHost());
            }
            if (!FTPReply.isPositiveCompletion(client.getReplyCode()))
            {
                throw new IOException("Ftp connect failed: " + client.getReplyCode());
            }
            if (!client.login(username, password))
            {
                throw new IOException("Ftp login failed: " + client.getReplyCode());
            }
        }
        catch (Exception e)
        {
            throw new ConnectionException("Could not establish FTP connection", e);
        }

        return client;
    }
}
