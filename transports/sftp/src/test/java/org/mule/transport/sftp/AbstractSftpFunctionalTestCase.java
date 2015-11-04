/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.util.sftp.SftpServer;

import java.io.IOException;

import org.junit.After;
import org.junit.Rule;

public abstract class AbstractSftpFunctionalTestCase extends FunctionalTestCase
{

    public static final String TESTDIR = "testdir";
    public static final String SFTP_PORT = "SFTP_PORT";

    @Rule
    public DynamicPort sftpPort = new DynamicPort(SFTP_PORT);

    protected SftpServer sftpServer;
    protected SftpClient sftpClient;

    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        setUpServer();
        sftpClient = createDefaultSftpClient(sftpPort.getNumber());
        cleanUpTestFolder();
        createTestFolder();
        setUpTestData();
    }

    private void createTestFolder() throws IOException
    {
        sftpClient.mkdir(TESTDIR);
        sftpClient.changeWorkingDirectory(TESTDIR);
    }

    @After
    public void tearDown() throws Exception
    {
        // In case there was a failure in @Before and the client is not set, avoid throwing an NPE that would hide the
        // first exception.
        if (sftpClient != null)
        {
            cleanUpTestFolder();

            sftpClient.disconnect();
        }

        if (sftpServer != null)
        {
            sftpServer.stop();
        }
    }

    protected static SftpClient createDefaultSftpClient(int number) throws IOException
    {
        SftpClient sftpClient = new SftpClient("localhost");
        sftpClient.setPort(number);
        sftpClient.login(SftpServer.USERNAME, SftpServer.PASSWORD);

        return sftpClient;
    }

    protected void setUpTestData() throws IOException
    {

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
