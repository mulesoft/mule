/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.ftp.connection;

import org.mule.extension.ftp.api.FtpConnector;
import org.mule.extension.ftp.api.ftp.FtpTransferMode;
import org.mule.extension.ftp.internal.AbstractFtpConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandlingStrategy;
import org.mule.runtime.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.runtime.api.connection.PoolingListener;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.Password;

import java.io.IOException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

/**
 * An {@link AbstractFtpConnectionProvider} which provides instances of
 * {@link ClassicFtpFileSystem} from instances of {@link FtpConnector}
 *
 * @since 4.0
 */
public final class ClassicFtpConnectionProvider extends AbstractFtpConnectionProvider<FtpConnector, ClassicFtpFileSystem>
{

    /**
     * The port number to connect on
     */
    @Parameter
    @Optional(defaultValue = "21")
    private int port = 21;

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
     * The transfer mode to be used. Currently {@code BINARY}
     * and {@code ASCII} are supported.
     * <p>
     * Defaults to {@code BINARY}
     */
    @Parameter
    @Optional(defaultValue = "BINARY")
    private FtpTransferMode transferMode;

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
     * Creates and returns a new instance of {@link ClassicFtpFileSystem}
     *
     * @param config the {@link FtpConnector} which parametrizes the return value
     * @return a {@link ClassicFtpFileSystem}
     */
    @Override
    public ClassicFtpFileSystem connect(FtpConnector config) throws ConnectionException
    {
        return new ClassicFtpFileSystem(config, createClient(config), muleContext);
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
            client.connect(getHost(), port);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionHandlingStrategy<ClassicFtpFileSystem> getHandlingStrategy(ConnectionHandlingStrategyFactory<FtpConnector, ClassicFtpFileSystem> handlingStrategyFactory)
    {
        return handlingStrategyFactory.supportsPooling(new PoolingListener<FtpConnector, ClassicFtpFileSystem>()
        {
            @Override
            public void onBorrow(FtpConnector config, ClassicFtpFileSystem connection)
            {
                connection.changeToBaseDir();
                connection.setTransferMode(transferMode);
                connection.setResponseTimeout(config.getResponseTimeout(), config.getResponseTimeoutUnit());
                connection.setPassiveMode(passive);
            }

            @Override
            public void onReturn(FtpConnector config, ClassicFtpFileSystem connection)
            {
            }
        });
    }
}
