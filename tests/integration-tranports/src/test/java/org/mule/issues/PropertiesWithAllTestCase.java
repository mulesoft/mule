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
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class PropertiesWithAllTestCase extends AbstractIntegrationTestCase
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
        MuleEvent response = flowRunner("flow1").withPayload("Hello").run();
        assertNotNull(response);
        assertEquals("foo", response.getSession().getProperty("foo"));
        assertEquals("bar", response.getSession().getProperty("bar"));
        assertEquals("baz", response.getSession().getProperty("baz"));
        assertNull(response.getMessage().<String>getOutboundProperty("outbar"));
        assertNull(response.getMessage().<String>getOutboundProperty("outbaz"));
    }
}
