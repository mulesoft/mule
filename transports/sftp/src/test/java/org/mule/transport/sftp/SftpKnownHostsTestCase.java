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
import org.mule.tck.util.sftp.SftpServer;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;

public class SftpKnownHostsTestCase extends AbstractSftpFunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "sftp-known_hosts-config.xml";
    }

    @Test
    public void unknownHost() throws URISyntaxException
    {
        try
        {
            createSftpClient(sftpPort.getNumber(), new File(ClassLoader.getSystemResource("empty_known_hosts").toURI()));
            fail("Expected IOException: UnknownHostKey");
        }
        catch (IOException e)
        {
            assertTrue(e.getMessage().startsWith("Error during login to muletest1@localhost: UnknownHostKey: localhost. DSA key fingerprint is "));
        }
    }

    @Test
    public void knownHostsInConnector() throws Exception
    {
        try
        {
            runFlow("knownHostsInConnector", "");
            fail("Expected IOException: UnknownHostKey");
        }
        catch (MessagingException e)
        {
            assertTrue(e.getCause().getMessage().startsWith("Error during login to muletest1@localhost: UnknownHostKey: localhost. DSA key fingerprint is "));
        }
    }

    @Test
    public void knownHostsInEndpoint() throws Exception
    {
        try
        {
            runFlow("knownHostsInEndpoint", "");
            fail("Expected IOException: UnknownHostKey");
        }
        catch (MessagingException e)
        {
            assertTrue(e.getCause().getMessage().startsWith("Error during login to muletest1@localhost: UnknownHostKey: localhost. DSA key fingerprint is "));
        }
    }

    @Test
    public void invalidKnownHostsInConnector() throws Exception
    {
        try
        {
            runFlow("invalidKnownHostsInConnector", "");
            fail("Expected IOException: java.io.FileNotFoundException");
        }
        catch (MessagingException e)
        {
            assertTrue(e.getCause().getMessage(), e.getCause().getMessage().startsWith("Error during login to muletest1@localhost: Known hosts file src/test/resources/invalid_known_hosts not found"));
        }
    }

    @Test
    public void invalidKnownHostsInEndpoint() throws Exception
    {
        try
        {
            runFlow("invalidKnownHostsInEndpoint", "");
            fail("Expected IOException: java.io.FileNotFoundException");
        }
        catch (MessagingException e)
        {
            assertTrue(e.getCause().getMessage(), e.getCause().getMessage().startsWith("Error during login to muletest1@localhost: Known hosts file src/test/resources/invalid_known_hosts not found"));
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
