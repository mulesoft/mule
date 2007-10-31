/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.firewall;

import org.mule.config.factories.HostNameFactory;
import org.mule.tck.AbstractMuleTestCase;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.SecureRandom;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FirewallTestCase extends AbstractMuleTestCase
{
    public static final String LOCALHOST = "localhost";
    public static final String LOCALADDR = "127.0.0.1";
    public static final int TEST_COUNT = 1;

    protected final Log logger = LogFactory.getLog(this.getClass());

    private SecureRandom random = new SecureRandom();

    public void testLoopback() throws Exception
    {
        // this gives localhost.localdomain on sourceforge
//        consistentAddress(LOCALHOST, true);
        consistentAddress(LOCALHOST, false);
//        assertEquals("Strange name for loopback", LOCALHOST, InetAddress.getByName(LOCALADDR).getCanonicalHostName());
    }

    public void testLocalHost() throws Exception
    {
        InetAddress aLocalAddress = InetAddress.getLocalHost();
        logger.info("Java returns " + addressToString(aLocalAddress) + " as the 'local' address");
        assertNotSame("No external address", LOCALADDR, aLocalAddress.getHostAddress());
        consistentAddress(aLocalAddress.getHostName(), false);
        assertEquals("Inconsistent hostname", aLocalAddress.getHostName(), new HostNameFactory().create(null));
    }

    public void testCanonicalHost() throws Exception
    {
        InetAddress aLocalAddress = InetAddress.getLocalHost();
        assertNotSame("No extrernal name", LOCALHOST, aLocalAddress.getCanonicalHostName());
        consistentAddress(aLocalAddress.getCanonicalHostName(), true);
    }

    protected void consistentAddress(String name, boolean canonical) throws UnknownHostException
    {
        String address = InetAddress.getByName(name).getHostAddress();
        logger.debug("Testing relationship between " + name + " and " + address);
        assertEquals("Name " + name + " is inconsistent", name,
                name(InetAddress.getByName(name), canonical));
        assertEquals("Address " + address + " is inconsistent", address,
                InetAddress.getByName(address).getHostAddress());
        // we cannot expect to go from address to name consistently, but we can expect
        // names always to resolve to the same address, and for addresses not to change
        // when going via a name (in other words, any aliases are consistent).
        assertEquals(name + " -> " + address + " is inconsistent", address,
                InetAddress.getByName(name).getHostAddress());
        assertEquals(name + " -> " + address + " -> " + name + " -> " + address + " is inconsistent", address,
                InetAddress.getByName(
                        name(
                                InetAddress.getByName(
                                        InetAddress.getByName(name).getHostAddress()), canonical)).getHostAddress());
    }

    protected String name(InetAddress address, boolean canonical)
    {
        if (canonical)
        {
            return address.getCanonicalHostName();
        }
        else
        {
            return address.getHostName();
        }
    }

    public void testLocalhostTcp() throws Exception
    {
        for (int i = 0; i < TEST_COUNT; ++i)
        {
            doTestTcp(InetAddress.getByName(LOCALHOST), randomPrivatePort());
        }
    }

    public void testHostnameTcp() throws Exception
    {
        for (int i = 0; i < TEST_COUNT; ++i)
        {
            doTestTcp(InetAddress.getLocalHost(), randomPrivatePort());
        }
    }

    public void testLocalhostUdp() throws Exception
    {
        for (int i = 0; i < TEST_COUNT; ++i)
        {
            doTestUdp(InetAddress.getByName(LOCALHOST), randomPrivatePort());
        }
    }

    public void testHostnameUdp() throws Exception
    {
        for (int i = 0; i < TEST_COUNT; ++i)
        {
            doTestUdp(InetAddress.getLocalHost(), randomPrivatePort());
        }
    }

    protected void doTestTcp(InetAddress address, int port) throws Exception
    {
        try
        {
            logger.debug("Testing TCP on " + addressToString(address, port));
            ServerSocket server = openTcpServer(address, port);
            Socket client = openTcpClient(address, port);
            Socket receiver = server.accept();
            client.getOutputStream().write(1);
            assertEquals("Failed to send byte via " + addressToString(address, port),
                    1, receiver.getInputStream().read());
            client.close();
            server.close();
        }
        catch (Exception e)
        {
            logger.error("Error while attempting TCP message on " + addressToString(address, port));
            throw e;
        }
    }

    protected void doTestUdp(InetAddress address, int port) throws Exception
    {
        try
        {
            logger.debug("Testing UDP on " + addressToString(address, port));
            DatagramSocket server = openUdpServer(address, port);
            DatagramSocket client = openUdpClient();
            client.send(new DatagramPacket(new byte[]{1}, 1, address, port));
            DatagramPacket packet = new DatagramPacket(new byte[1], 1);
            server.receive(packet);
            assertEquals("Failed to send packet via " + addressToString(address, port),
                    1, packet.getData()[0]);
            client.close();
            server.close();
        }
        catch (Exception e)
        {
            logger.error("Error while attempting UDP message on " + addressToString(address, port));
            throw e;
        }
    }

    protected Socket openTcpClient(InetAddress address, int port) throws IOException
    {
        try
        {
            return new Socket(address, port);
        }
        catch (IOException e)
        {
            logger.error("Could not open TCP client to " + addressToString(address, port));
            throw e;
        }
    }

    protected ServerSocket openTcpServer(InetAddress address, int port) throws IOException
    {
        try
        {
            return new ServerSocket(port, 1, address);
        }
        catch (IOException e)
        {
            logger.error("Could not open TCP server on " + addressToString(address, port));
            throw e;
        }
    }

    protected DatagramSocket openUdpServer(InetAddress address, int port) throws IOException
    {
        try
        {
            return new DatagramSocket(port, address);
        }
        catch (IOException e)
        {
            logger.error("Could not open UDP server on " + addressToString(address, port));
            throw e;
        }
    }

    protected DatagramSocket openUdpClient() throws IOException
    {
        try
        {
            return new DatagramSocket();
        }
        catch (IOException e)
        {
            logger.error("Could not open UDP client");
            throw e;
        }
    }

    protected String addressToString(InetAddress address, int port)
    {
        return addressToString(address) + ":" + port;
    }

    protected String addressToString(InetAddress address)
    {
        return address.getHostName() + "/" + address.getCanonicalHostName() + "/" + address.getHostAddress();
    }

    protected int randomPrivatePort()
    {
        return randomPort(49152, 65535);
    }

    /**
     * @param lo
     * @param hi
     * @return A number between lo and hi (inclusive)
     */
    protected int randomPort(int lo, int hi)
    {
        return lo + random.nextInt(hi - lo + 1);
    }

}
