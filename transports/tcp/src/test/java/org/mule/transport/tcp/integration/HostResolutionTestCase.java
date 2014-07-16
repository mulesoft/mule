/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp.integration;

import org.junit.Rule;
import org.junit.Test;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HostResolutionTestCase extends FunctionalTestCase 
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");    

    @Override
    protected String getConfigFile()
    {
        return "host-resolution-config.xml";
    }

    @Test
    public void testDefaultConfiguration() throws MuleException
    {
        final MuleClient client = new MuleClient(muleContext);
        final MuleMessage message = client.send("vm://defaultConfiguration", "something", null);
        assertNotNull(message.getExceptionPayload());
        assertEquals(NullPayload.getInstance(), message.getPayload());
    }

    @Test
    public void testFailOnUnresolvedHost_false() throws MuleException
    {
        final MuleClient client = new MuleClient(muleContext);
        final MuleMessage message = client.send("vm://failOnUnresolvedHostFalse", "something", null);
        assertTrue(message.getExceptionPayload() == null);
        assertArrayEquals("something else".getBytes(), (byte[])message.getPayload());
    }

    @Test
    public void testFailOnUnresolvedHost_true() throws MuleException
    {
        final MuleClient client = new MuleClient(muleContext);
        final MuleMessage message = client.send("vm://failOnUnresolvedHostTrue", "something", null);
        assertNotNull(message.getExceptionPayload());
        assertEquals(NullPayload.getInstance(), message.getPayload());
    }

}
