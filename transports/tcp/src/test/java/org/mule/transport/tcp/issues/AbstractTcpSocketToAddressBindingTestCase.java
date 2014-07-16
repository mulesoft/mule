/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp.issues;

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

/**
 * Tests how sockets are bound to addresses by the TCP transport. This test is related to MULE-6584.
 */
public abstract class AbstractTcpSocketToAddressBindingTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Rule
    public DynamicPort dynamicPort3 = new DynamicPort("port3");

    protected List<InetAddress> localInetAddresses;

    public AbstractTcpSocketToAddressBindingTestCase() throws SocketException
    {
        super();
        localInetAddresses = getAllLocalInetAddresses();
    }

    @Override
    protected String getConfigFile()
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

    /**
     * Returns all local {@link java.net.InetAddress} except the loopback address.
     * @return A {@link java.util.List <InetAddress>} with the IPv4 local addresses.
     * @throws java.net.SocketException If there is a problem getting the addresses.
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
