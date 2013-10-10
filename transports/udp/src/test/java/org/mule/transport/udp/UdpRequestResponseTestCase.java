/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.udp;

import org.mule.api.MuleMessage;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UdpRequestResponseTestCase extends FunctionalTestCase
{
    private static final String EXPECTED = TEST_MESSAGE + " received";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "udp-request-response.xml";
    }

    @Test
    public void testRequestResponse() throws Exception
    {
        MuleMessage response = muleContext.getClient().send("vm://fromTest", TEST_MESSAGE, null);
        assertEquals(EXPECTED, response.getPayloadAsString());
    }
}
