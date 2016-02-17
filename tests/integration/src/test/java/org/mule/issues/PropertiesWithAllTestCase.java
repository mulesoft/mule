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
import static org.mule.PropertyScope.SESSION;
import org.mule.api.MuleMessage;
import org.mule.functional.junit4.FunctionalTestCase;
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
        MuleMessage response = flowRunner("flow1").withPayload("Hello").run().getMessage();
        assertNotNull(response);
        assertEquals("foo", response.getProperty("foo", SESSION));
        assertEquals("bar", response.getProperty("bar", SESSION));
        assertEquals("baz", response.getProperty("baz", SESSION));
        assertNull(response.<String>getOutboundProperty("outbar"));
        assertNull(response.<String>getOutboundProperty("outbaz"));
    }
}
