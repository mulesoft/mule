/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.resolvers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.PropertyScope;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.model.resolvers.EntryPointNotFoundException;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class MethodEntryPointsTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/resolvers/method-entrypoints-config-flow.xml";
    }

    @Test
    public void testTooManySatisfiableMethods() throws Exception
    {
        try
        {
            flowRunner("Service").withPayload("hello").run().getMessage();
        }
        catch (Exception e)
        {
            assertThat(e.getCause(), instanceOf(EntryPointNotFoundException.class));
            assertThat(e.getMessage(), containsString("Found too many possible methods on object"));
        }
    }

    @Test
    public void testBadMethodName() throws Exception
    {
        Map<String, Object> properties = new HashMap<>();
        properties.put("method", "foo");
        MuleMessage send = new DefaultMuleMessage("hello", properties, null, null, muleContext);
        try
        {
            flowRunner("Service").withPayload(send).run().getMessage();
        }
        catch (Exception e)
        {
            assertThat(e.getCause(), instanceOf(EntryPointNotFoundException.class));
        }
    }

    @Test
    public void testValidCallToReverse() throws Exception
    {
        MuleMessage msg = getTestMuleMessage("hello");
        msg.setProperty("method", "reverseString", PropertyScope.INBOUND);
        MuleMessage message = flowRunner("Service").withPayload(msg).run().getMessage();
        assertNotNull(message);
        assertEquals("olleh", getPayloadAsString(message));
    }

    @Test
    public void testValidCallToUpperCase() throws Exception
    {
        MuleMessage msg = getTestMuleMessage("hello");
        msg.setProperty("method", "upperCaseString", PropertyScope.INBOUND);
        MuleMessage message = flowRunner("Service").withPayload(msg).run().getMessage();
        assertNotNull(message);
        assertEquals("HELLO", getPayloadAsString(message));
    }
}
