/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.sftp.connection;

import org.mule.extension.ftp.api.FtpConnector;
import org.mule.extension.ftp.api.sftp.SftpAuthenticationMethod;
import org.mule.extension.ftp.api.sftp.SftpFileSystem;
import org.mule.extension.ftp.internal.AbstractFtpConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandlingStrategy;
import org.mule.runtime.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.runtime.core.util.CollectionUtils;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.Password;

import com.google.common.base.Joiner;

import java.util.Set;

/**
 * An {@link AbstractFtpConnectionProvider} which provides instances of
 * {@link SftpFileSystem} from instances of {@link FtpConnector}
 *
 * @since 4.0
 */
@Alias("sftp")
public class SftpConnectionProvider extends AbstractFtpConnectionProvider<SftpFileSystem>
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
    protected String username;

    /**
     * The password for the user being authenticated.
     */
    @Parameter
    @Optional
    @Password
    private String password;

    /**
     * The passphrase (password) for the identityFile if required.
     * Notice that this parameter is ignored if {@link #identityFile}
     * is not provided
     */
    @Parameter
    @Optional
    @Password
    private String passphrase;

    /**
     * An identityFile location for a PKI private key.
     */
    @Parameter
    @Optional
    private String identityFile;

    /**
     * Comma separated list of authentication methods used by the SFTP client.
     * Valid values are: gssapi-with-mic, publickey, keyboard-interactive and password.
     */
    @Parameter
    @Optional
    private Set<SftpAuthenticationMethod> preferredAuthenticationMethods;

    /**
     * If provided, the client will validate the server's key against the one in the referenced file.
     * If the server key doesn't match the one in the file, the connection will be aborted.
     */
    @Parameter
    @Optional
    private String knownHostsFile;

    private SftpClientFactory clientFactory = new SftpClientFactory();

    @Override
    public SftpFileSystem connect() throws ConnectionException
    {
        SftpClient client = clientFactory.createInstance(getHost(), port);
        client.setConnectionTimeoutMillis(getConnectionTimeoutUnit().toMillis(getConnectionTimeout()));
        client.setPassword(password);
        client.setIdentity(identityFile, passphrase);
        if (!CollectionUtils.isEmpty(preferredAuthenticationMethods))
        {
            client.setPreferredAuthenticationMethods(Joiner.on(",").join(preferredAuthenticationMethods));
        }
        client.setKnownHostsFile(knownHostsFile);
        try
        {
            client.login(username);
        }
        catch (Exception e)
        {
            throw new ConnectionException(e);
        }

        return new SftpFileSystem(client, muleContext);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionHandlingStrategy<SftpFileSystem> getHandlingStrategy(ConnectionHandlingStrategyFactory<SftpFileSystem> handlingStrategyFactory)
    {
        return handlingStrategyFactory.supportsPooling();
    }

    void setPort(int port)
    {
        this.port = port;
    }

    void setUsername(String username)
    {
        this.username = username;
    }

    void setPassword(String password)
    {
        this.password = password;
    }

    void setPassphrase(String passphrase)
    {
        this.passphrase = passphrase;
    }

    void setIdentityFile(String identityFile)
    {
        this.identityFile = identityFile;
    }

    void setPreferredAuthenticationMethods(Set<SftpAuthenticationMethod> preferredAuthenticationMethods)
    {
        this.preferredAuthenticationMethods = preferredAuthenticationMethods;
    }

    void setKnownHostsFile(String knownHostsFile)
    {
        this.knownHostsFile = knownHostsFile;
    }

    void setClientFactory(SftpClientFactory clientFactory)
    {
        this.clientFactory = clientFactory;
    }
}
