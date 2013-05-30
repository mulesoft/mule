/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
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
import org.junit.Before;
import org.junit.Rule;

public abstract class AbstractSftpFunctionalTestCase extends FunctionalTestCase
{

    public static final String TESTDIR = "testdir";

    @Rule
    public DynamicPort sftpPort = new DynamicPort("SFTP_PORT");

    protected SftpServer sftpServer;
    protected SftpClient sftpClient;

    @Before
    public void setUp() throws IOException
    {
        setUpServer();
        setUpClient();
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
        sftpClient.login(SftpServer.USERNAME, SftpServer.PASSWORD);
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
