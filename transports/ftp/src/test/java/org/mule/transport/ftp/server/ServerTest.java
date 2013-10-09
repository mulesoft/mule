/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
