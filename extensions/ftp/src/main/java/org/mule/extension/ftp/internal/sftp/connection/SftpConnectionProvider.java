/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.sftp.connection;

import org.mule.extension.ftp.api.FtpConnector;
import org.mule.extension.ftp.api.sftp.SftpFileSystem;
import org.mule.extension.ftp.internal.AbstractFtpConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandlingStrategy;
import org.mule.runtime.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.runtime.api.connection.PoolingListener;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.Password;

/**
 * An {@link AbstractFtpConnectionProvider} which provides instances of
 * {@link SftpFileSystem} from instances of {@link FtpConnector}
 *
 * @since 4.0
 */
@Alias("sftp")
public class SftpConnectionProvider extends AbstractFtpConnectionProvider<FtpConnector, SftpFileSystem>
{

    /**
     * The port number to connect on
     */
    @Parameter
    @Optional(defaultValue = "22")
    private int port = 22;

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

    @Override
    public SftpFileSystem connect(FtpConnector config) throws ConnectionException
    {
        SftpClient client = new SftpClient(getHost(), port);
        client.setConnectionTimeoutMillis(config.getConnectionTimeoutUnit().toMillis(config.getConnectionTimeout()));
        try
        {
            client.login(username, password);
        }
        catch (Exception e)
        {
            throw new ConnectionException(e);
        }

        return new SftpFileSystem(config, client, muleContext);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionHandlingStrategy<SftpFileSystem> getHandlingStrategy(ConnectionHandlingStrategyFactory<FtpConnector, SftpFileSystem> handlingStrategyFactory)
    {
        return handlingStrategyFactory.supportsPooling(new PoolingListener<FtpConnector, SftpFileSystem>()
        {
            @Override
            public void onBorrow(FtpConnector config, SftpFileSystem connection)
            {
                connection.changeToBaseDir();
            }

            @Override
            public void onReturn(FtpConnector sftpConfig, SftpFileSystem sftpFileSystem)
            {
            }
        });
    }
}
