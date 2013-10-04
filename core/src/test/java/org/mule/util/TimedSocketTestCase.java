/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Note:
 * <p>These test cases do not cover all possible connection failures. 
 * It is e.g. not easily possible to simulate an unplugged cable.</p>
 * 
 * Note: this test will fail if off-line.
 */
public class TimedSocketTestCase extends AbstractMuleTestCase
{
    private static final String REACHABLE_HOSTNAME = "127.0.0.1";
    private static final int REACHABLE_PORT = 3333;
    private static final String UNREACHABLE_HOSTNAME = "4.7.1.1";
    private static final int UNREACHABLE_PORT = 4711;

    private static final int TEST_TIMEOUT = 1000;
    private static final int TEST_TIMEOUT_DELTA = 300;    
        
    @Test
    public void testWorkingConnection() throws Exception
    {
        Socket client = null;
        ServerSocket server = null;
        
        try
        {
            server = new ServerSocket(REACHABLE_PORT);
            assertNotNull(server);
            client = TimedSocket.createSocket(REACHABLE_HOSTNAME, REACHABLE_PORT, TEST_TIMEOUT);
            assertNotNull(client);
        }
        catch (InterruptedIOException iioe)
        {
            fail("Server timed out");
        }
        catch (SocketException se)
        {
            fail("Client/Server socket exception");
        }
        catch (IOException ioe)
        {
            fail("Client/Server network I/O error - " + ioe);
        }
        finally
        {
            try
            {
                if (client != null)
                {
                    client.close();
                }
            }
            catch (Exception ignore)
            {
                fail("Error closing client connection");
            }
            finally
            {
                try
                {
                    if (server != null)
                    {
                        server.close();
                    }
                }
                catch (Exception ignore)
                {
                    fail("Error closing server connection");
                }
            }
        }
    }

    @Test
    public void testConnectionTimeoutInterruptionOnReachableHostnameAndUnreachablePort() throws Exception
    {
        Socket client = null;

        try
        {
            client = TimedSocket.createSocket(REACHABLE_HOSTNAME, UNREACHABLE_PORT, TEST_TIMEOUT);
            fail("Socket exception is expected");
        }
        catch (InterruptedIOException iioe)
        {
            assertNull(client);
        }
        catch (MalformedURLException mue)
        {
            fail("Invalid URL");
        }
        catch (SocketException se)
        {
            assertNull(client);
        }
        catch (IOException ioe)
        {
            fail("Network I/O error - " + ioe);
        }
        finally
        {
            try
            {
                if (client != null)
                {
                    client.close();
                }
            }
            catch (Exception ignore)
            {
                fail("Error closing connection");
            }
        }
    }
    
    @Test
    public void testConnectionTimeoutInterruptionOnUnreachableHostnameAndPost() throws Exception
    {
        Socket client = null;
        long startTime = 0;
        long stopTime = 0;
        try
        {
            startTime = System.currentTimeMillis();
            client = TimedSocket.createSocket(UNREACHABLE_HOSTNAME, UNREACHABLE_PORT, TEST_TIMEOUT);
            fail("Timeout is expected");
        }
        catch (InterruptedIOException iioe)
        {
            stopTime = System.currentTimeMillis();
            assertTrue("Remote host timeout was longer than expected. Expected: " + TEST_TIMEOUT + ", but was" + stopTime, (stopTime - startTime) > (TEST_TIMEOUT - TEST_TIMEOUT_DELTA));
        }
        catch (MalformedURLException mue)
        {
            fail("Invalid URL: " + mue);
        }
        catch (SocketException se)
        {
            fail("Socket exception: " + se);
        }
        catch (IOException ioe)
        {
            fail("Network I/O error: " + ioe);
        }
        finally
        {
            try
            {
                if (client != null)
                {
                    client.close();
                }
            }
            catch (Exception ignore)
            {
                fail("Error closing connection: " + ignore);
            }
        }
    }
}
