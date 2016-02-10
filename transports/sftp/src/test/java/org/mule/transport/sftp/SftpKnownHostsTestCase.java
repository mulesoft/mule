/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.MessagingException;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.util.sftp.SftpServer;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SftpKnownHostsTestCase extends AbstractSftpFunctionalTestCase
{

    @Rule
    public SystemProperty sftpKnownHostsFile;

    public String expectedMessage;

    @Override
    protected String getConfigFile()
    {
        return "sftp-known_hosts-config.xml";
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                                             {new SystemProperty("mule.sftp.knownHostsFile", "src/test/resources/empty_known_hosts"),
                                              "Error during login to muletest1@localhost: UnknownHostKey: localhost. DSA key fingerprint is "},
                                             {new SystemProperty("mule.sftp.knownHostsFile", "src/test/resources/invalid_known_hosts"),
                                              "Error during login to muletest1@localhost: Known hosts file src/test/resources/invalid_known_hosts not found"}
        });
    }

    public SftpKnownHostsTestCase(SystemProperty sftpKnownHostsFile, String expectedMessage)
    {
        this.sftpKnownHostsFile = sftpKnownHostsFile;
        this.expectedMessage = expectedMessage;
    }

    @Test
    public void unknownHost() throws URISyntaxException
    {
        try
        {
            createSftpClient(sftpPort.getNumber(), new File(ClassLoader.getSystemResource("empty_known_hosts").toURI()));
            fail("Expected IOException");
        }
        catch (IOException e)
        {
            assertTrue(e.getMessage(), e.getMessage().startsWith("Error during login to muletest1@localhost: UnknownHostKey: localhost. DSA key fingerprint is "));
        }
    }

    @Test
    public void knownHostsInSysProp() throws Exception
    {
        try
        {
            runFlow("knownHostsInSysProp", "");
            fail("Expected IOException");
        }
        catch (MessagingException e)
        {
            assertTrue(e.getCause().getMessage(), e.getCause().getMessage().startsWith(expectedMessage));
        }
    }

    protected static SftpClient createSftpClient(int number, File knownHostsFile) throws IOException
    {
        SftpClient sftpClient = new SftpClient("localhost");
        sftpClient.setKnownHostsFile(knownHostsFile);
        sftpClient.setPort(number);
        sftpClient.login(SftpServer.USERNAME, SftpServer.PASSWORD);

        return sftpClient;
    }


}
