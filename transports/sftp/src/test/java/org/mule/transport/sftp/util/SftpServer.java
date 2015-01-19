/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp.util;

import java.io.IOException;
import java.security.Security;
import java.util.Arrays;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class SftpServer
{

    protected static final String USERNAME = "muletest1";
    protected static final String PASSWORD = "muletest1";
    private SshServer sshdServer;
    private Integer port;

    public SftpServer(int port)
    {
        this.port = port;
        configureSecurityProvider();
        SftpSubsystem.Factory factory = createFtpSubsystemFactory();
        sshdServer = SshServer.setUpDefaultServer();
        configureSshdServer(factory, passwordAuthenticator());
    }

    public void setPasswordAuthenticator(PasswordAuthenticator passwordAuthenticator)
    {
        sshdServer.setPasswordAuthenticator(passwordAuthenticator);
    }

    private void configureSshdServer(SftpSubsystem.Factory factory,
                                     PasswordAuthenticator passwordAuthenticator)
    {
        sshdServer.setPort(port);
        sshdServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey.ser"));
        sshdServer.setSubsystemFactories(Arrays.<NamedFactory<Command>> asList(factory));
        sshdServer.setCommandFactory(new ScpCommandFactory());
        sshdServer.setShellFactory(new ProcessShellFactory());
        sshdServer.setPasswordAuthenticator(passwordAuthenticator);
    }

    private SftpSubsystem.Factory createFtpSubsystemFactory()
    {
        SftpSubsystem.Factory factory = new SftpSubsystem.Factory();
        return factory;
    }

    private void configureSecurityProvider()
    {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static PasswordAuthenticator passwordAuthenticator()
    {
        return new PasswordAuthenticator()
        {

            @Override
            public boolean authenticate(String arg0, String arg1, ServerSession arg2)
            {
                return USERNAME.equals(arg0) && PASSWORD.equals(arg1);
            }
        };
    }

    public void start()
    {
        try
        {
            sshdServer.start();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void stop()
    {
        try
        {
            sshdServer.stop();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        sshdServer = null;
    }

}
