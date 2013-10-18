/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.sftp.util.SftpServer;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class SftpFileWithOutExtensionTestCase extends FunctionalTestCase
{

    public static final String TESTDIR = "testdir";

    @Rule
    public DynamicPort sftpPort = new DynamicPort("SFTP_PORT");

    private SftpServer sftpServer;
    private SftpClient sftpClient;

    @Override
    protected String getConfigResources()
    {
        return "mule-sftp-file-without-extension-config.xml";
    }

    @Before
    public void setUp() throws IOException
    {
        setUpServer();
        setUpClient();
        cleanUpTestFolder();
        setUpTestData();
    }

    @Test
    public void readsFileWithNoExtension() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.request("vm://testOut", RECEIVE_TIMEOUT);

        assertNotNull("Did not processed the file", response);
    }

    @After
    public void tearDown() throws Exception
    {
        cleanUpTestFolder();

        sftpClient.disconnect();

        if (sftpServer != null)
        {
            sftpServer.stop();
        }
    }

    private void setUpClient() throws IOException
    {
        sftpClient = new SftpClient("localhost");
        sftpClient.setPort(sftpPort.getNumber());
        sftpClient.login("muletest1", "muletest1");
    }

    private void setUpTestData() throws IOException
    {
        sftpClient.mkdir(TESTDIR);
        sftpClient.changeWorkingDirectory(TESTDIR);
        sftpClient.storeFile("file", new ByteArrayInputStream(TEST_MESSAGE.getBytes()));
    }

    private void setUpServer()
    {
        sftpServer = new SftpServer(sftpPort.getNumber());
        sftpServer.start();
    }

    private void cleanUpTestFolder()
    {
        try
        {
            sftpClient.recursivelyDeleteDirectory(TESTDIR);
        }
        catch (IOException e)
        {
            // Ignore: folder does not exists
        }
    }
}
