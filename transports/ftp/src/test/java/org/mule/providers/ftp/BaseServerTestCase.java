/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ftp;

import org.mule.providers.ftp.server.Server;
import org.mule.providers.ftp.server.NamedPayload;
import org.mule.tck.FunctionalTestCase;


public abstract class BaseServerTestCase extends FunctionalTestCase
{

    public static final String TEST_MESSAGE = "Test FTP message";
    private static int DEFAULT_TIMEOUT = 10000;
    private int timeout;
    private int port;
    private Server server;

    public BaseServerTestCase(int port, int timeout)
    {
        this.port = port;
        this.timeout = timeout;
        setDisposeManagerPerSuite(true);
    }

    public BaseServerTestCase(int port)
    {
        this(port, DEFAULT_TIMEOUT);
    }

    protected void startServer() throws Exception
    {
        server = new Server(port);
        server.awaitStart(timeout);
        // this is really ugly, but the above doesn't get to waiting.
        // need to improve this as part of ftp server work
        synchronized(this)
        {
            wait(500);
        }
    }

    protected void stopServer() throws Exception
    {
        // stop the server
        if (null != server)
        {
            server.stop();
        }
    }

    protected void doSetUp() throws Exception
    {
        startServer();
    }

    protected void doTearDown() throws Exception
    {
        stopServer();
    }

    protected int getTimeout()
    {
        return timeout;
    }

    protected NamedPayload awaitUpload() throws InterruptedException
    {
        return server.awaitUpload(timeout);
    }

}
