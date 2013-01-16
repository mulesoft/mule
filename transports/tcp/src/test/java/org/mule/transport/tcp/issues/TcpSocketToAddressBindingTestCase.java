/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.tcp.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.mule.api.MuleMessage;
import org.mule.api.transport.DispatchException;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

/**
 * Tests how sockets are bound to addresses by the TCP transport. This test is related to MULE-6584.
 */
public class TcpSocketToAddressBindingTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Rule
    public DynamicPort dynamicPort3 = new DynamicPort("port3");

    private List<InetAddress> localInetAddresses;

    public TcpSocketToAddressBindingTestCase() throws SocketException
    {
        super();
        localInetAddresses = getAllLocalInetAddresses();
    }

    @Override
    protected String getConfigResources()
    {
        return "tcp-socket-to-address-binding-test.xml";
    }

    /**
     * Returns the name of the transport associated with this test.
     * @return The transport name.
     */
    protected String getTransportName()
    {
        return "tcp";
    }

    @Test
    public void testRequestUsingLoopbackAddressAtLoopbackAddress() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result;

        // Request using loopback address at endpoint listening at 127.0.0.1 should get an appropiate response.
        result = client.send(getTransportName()+"://127.0.0.1:"+dynamicPort1.getNumber(), TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
    }

    @Test
    public void testRequestUsingLoopbackAddressAtLocalhost() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result;

        // Request using loopback address at endpoint listening at localhost should get an appropiate response.
        result = client.send(getTransportName()+"://127.0.0.1:"+dynamicPort2.getNumber(), TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
    }

    @Test
    public void testRequestUsingLoopbackAddressAtAllAddresses() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result;

        // Request using loopback address at endpoint listening at all addresses should get an appropiate response.
        result = client.send(getTransportName()+"://127.0.0.1:"+dynamicPort3.getNumber(), TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
    }

    @Test
    public void testRequestNotUsingLoopbackAddressAtLoopbackAddress() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result;

        // Iterate over local addresses.
        for (InetAddress inetAddress : localInetAddresses)
        {
            // Request not using loopback address to endpoint listening at 127.0.0.1 should timeout.
            try
            {
                result = client.send(getTransportName()+"://"+inetAddress.getHostAddress()+":"+dynamicPort1.getNumber(), TEST_MESSAGE, null);
                assertNull(result);
            }
            catch (DispatchException ex)
            {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void testRequestNotUsingLoopbackAddressAtLocalhost() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result;

        // Iterate over local addresses.
        for (InetAddress inetAddress : localInetAddresses)
        {
            // Request not using loopback address to endpoint listening at localhost should timeout.
            try
            {
                result = client.send(getTransportName()+"://"+inetAddress.getHostAddress()+":"+dynamicPort2.getNumber(), TEST_MESSAGE, null);
                assertNull(result);
            }
            catch (DispatchException ex)
            {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void testRequestNotUsingLoopbackAddressAtAllAddresses() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result;

        // Iterate over local addresses.
        for (InetAddress inetAddress : localInetAddresses)
        {
            /* Request not using loopback address to endpoint listening at all local addresses should get an
             * appropriate response. */
            result = client.send(getTransportName()+"://"+inetAddress.getHostAddress()+":"+dynamicPort3.getNumber(), TEST_MESSAGE, null);
            assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
        }
    }

    /**
     * Returns all local {@link InetAddress} except the loopback address.
     * @return A {@link java.util.List <InetAddress>} with the IPv4 local addresses.
     * @throws SocketException If there is a problem getting the addresses.
     */
    private List<InetAddress> getAllLocalInetAddresses() throws SocketException
    {
        List<InetAddress> result = new ArrayList<InetAddress>();
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netInt : Collections.list(nets))
        {
            Enumeration<InetAddress> inetAddresses = netInt.getInetAddresses();
            for (InetAddress inetAddress : Collections.list(inetAddresses))
            {
                if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress())
                {
                    result.add(inetAddress);
                }
            }
        }
        return result;
    }
}
