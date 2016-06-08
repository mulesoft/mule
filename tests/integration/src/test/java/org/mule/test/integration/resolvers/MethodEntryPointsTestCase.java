/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.resolvers;

import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.model.resolvers.EntryPointNotFoundException;

import java.io.Serializable;
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
        Map<String, Serializable> properties = new HashMap<>();
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
        MuleMessage msg = new DefaultMuleMessage("hello", singletonMap("method", "reverseString"), null, null, muleContext);
        MuleMessage message = flowRunner("Service").withPayload(msg).run().getMessage();
        assertNotNull(message);
        assertEquals("olleh", getPayloadAsString(message));
    }

    @Test
    public void testValidCallToUpperCase() throws Exception
    {
        MuleMessage msg = new DefaultMuleMessage("hello", singletonMap("method", "upperCaseString"), null, null, muleContext);
        msg.setOutboundProperty("method", "upperCaseString");
        MuleMessage message = flowRunner("Service").withPayload(msg).run().getMessage();
        assertNotNull(message);
        assertEquals("HELLO", getPayloadAsString(message));
    }
}
