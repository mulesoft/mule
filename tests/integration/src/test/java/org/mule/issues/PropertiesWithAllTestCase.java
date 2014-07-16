/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class PropertiesWithAllTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/issues/all-properties.xml";
    }

    @Test
    public void testSessionAndOutboundProperties() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://in", "Hello", null);
        assertNotNull(response);
        assertEquals("foo", response.getSessionProperty("foo"));
        assertEquals("bar", response.getSessionProperty("bar"));
        assertEquals("baz", response.getSessionProperty("baz"));
        assertNull(response.<String>getOutboundProperty("outbar"));
        assertNull(response.<String>getOutboundProperty("outbaz"));
    }
}
