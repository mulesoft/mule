/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ftp.server;

import org.mule.tck.junit4.rule.DynamicPort;

import org.apache.commons.net.ftp.FTPClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests for the embedded ftp server startup, log in, and shutdown.
 */
public class ServerTest
{

    private Server ftpServer = null;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Before
    public void setUpServer() throws Exception
    {
        ftpServer = new Server(dynamicPort.getNumber());
    }

    @After
    public void tearDown()
    {
        ftpServer.stop();
    }

    /**
     * Sanity test that the embedded ftp server is working. Useful as a first step if
     * the ftp transport tests are failing.
     *
     * @throws Exception
     */
    @Test
    public void testServerLogin() throws Exception
    {
        FTPClient ftpClient = new FTPClient();
        ftpClient.connect("localhost", dynamicPort.getNumber());
        ftpClient.login("admin", "admin");
    }
}
