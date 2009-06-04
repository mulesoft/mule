/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ftp.server;

import org.apache.commons.net.ftp.FTPClient;

import junit.framework.TestCase;

/**
 * Tests for the embedded ftp server startup, log in, and shutdown.
 */
public class ServerTest extends TestCase
{
    private Server ftpServer = null;

    public void setUp() throws Exception
    {
        ftpServer = new Server(Server.DEFAULT_PORT);
    }

    /**
     * Sanity test that the embedded ftp server is working. Useful as a first step if
     * the ftp transport tests are failing.
     * 
     * @throws Exception
     */
    public void testServer() throws Exception
    {
        FTPClient ftpClient = new FTPClient();
        ftpClient.connect("localhost", Server.DEFAULT_PORT);
        ftpClient.login("admin", "admin");
    }

    public void tearDown()
    {
        ftpServer.stop();
        ftpServer = null;
    }
}
