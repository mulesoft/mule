/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.issues;


import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.mule.tck.junit4.rule.DynamicPort;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class PropertiesWithAllTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "org/mule/issues/all-properties.xml";
    }

    @Test
    public void testSessionAndOutboundProperties() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("vm://in", "Hello", null);
        assertNotNull(response);
        assertEquals("foo", response.getSessionProperty("foo"));
        assertEquals("bar", response.getSessionProperty("bar"));
        assertEquals("baz", response.getSessionProperty("baz"));
        assertNull(response.<String>getOutboundProperty("outbar"));
        assertNull(response.<String>getOutboundProperty("outbaz"));
    }
}
