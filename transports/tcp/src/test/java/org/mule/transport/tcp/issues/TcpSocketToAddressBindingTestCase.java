package org.mule.transport.tcp.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.mule.api.MuleMessage;
import org.mule.api.transport.DispatchException;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests how sockets are bound to addresses by the TCP transport. This test is related to MULE-6584.
 */
public class TcpSocketToAddressBindingTestCase extends AbstractServiceAndFlowTestCase
{
    protected static String TEST_MESSAGE = "Test TCP Request";

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Rule
    public DynamicPort dynamicPort3 = new DynamicPort("port3");

    public TcpSocketToAddressBindingTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
                {ConfigVariant.SERVICE, "tcp-socket-to-address-binding-test-service.xml"},
                {ConfigVariant.FLOW, "tcp-socket-to-address-binding-test-flow.xml"}
        });
    }

    @Test
    public void testRequestUsingLoopbackAddress() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result;

        // Request using loopback address to all endpoints should get an appropriate response.
        result = client.send("tcp://127.0.0.1:"+dynamicPort1.getNumber(), TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());

        result = client.send("tcp://127.0.0.1:"+dynamicPort2.getNumber(), TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());

        result = client.send("tcp://127.0.0.1:"+dynamicPort3.getNumber(), TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
    }

    @Test
    public void testRequestNotUsingLoopbackAddress() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result;

        // Iterate over local addresses.
        for (InetAddress inetAddress : getAllLocalInetAddresses())
        {
            if (!inetAddress.isLoopbackAddress())
            {
                // Request not using loopback address to endpoint listening at 127.0.0.1 should timeout.
                try
                {
                    result = client.send("tcp://"+inetAddress.getHostAddress()+":"+dynamicPort1.getNumber(), TEST_MESSAGE, null);
                    assertNull(result);
                }
                catch (DispatchException ex)
                {
                    ex.printStackTrace();
                }

                // Request not using loopback address to endpoint listening at localhost should timeout.
                try
                {
                    result = client.send("tcp://"+inetAddress.getHostAddress()+":"+dynamicPort2.getNumber(), TEST_MESSAGE, null);
                    assertNull(result);
                }
                catch (DispatchException ex)
                {
                    ex.printStackTrace();
                }

                /* Request not using loopback address to endpoint listening at all local addresses should get an
                 * appropriate response. */
                result = client.send("tcp://"+inetAddress.getHostAddress()+":"+dynamicPort3.getNumber(), TEST_MESSAGE, null);
                assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
            }
        }
    }

    /**
     * Returns all local {@link InetAddress}.
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
                if (inetAddress instanceof Inet4Address)
                {
                    result.add(inetAddress);
                }
            }
        }
        return result;
    }
}
