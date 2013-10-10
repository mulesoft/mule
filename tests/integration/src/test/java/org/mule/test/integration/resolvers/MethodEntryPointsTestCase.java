/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.resolvers;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.model.resolvers.EntryPointNotFoundException;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MethodEntryPointsTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/resolvers/method-entrypoints-config.xml";
    }

    @Test
    public void testTooManySatisfiableMethods() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://service", "hello", null);
        assertNotNull(message.getExceptionPayload());
        assertTrue(message.getExceptionPayload().getException().getCause() instanceof EntryPointNotFoundException);
        assertTrue(message.getExceptionPayload().getException().getCause().getMessage().indexOf("Found too many possible methods on object") > -1);
    }

    @Test
    public void testBadMethodName() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://service?method=foo", "hello", null);
        assertNotNull(message.getExceptionPayload());
        assertTrue(message.getExceptionPayload().getException().getCause() instanceof EntryPointNotFoundException);
    }

    @Test
    public void testValidCallToReverse() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        DefaultMuleMessage msg = new DefaultMuleMessage("hello", muleContext);
        msg.setOutboundProperty("method", "reverseString");
        MuleMessage message = client.send("vm://service", msg);
        assertNotNull(message);
        assertEquals("olleh", message.getPayloadAsString());
    }

    @Test
    public void testValidCallToUpperCase() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        DefaultMuleMessage msg = new DefaultMuleMessage("hello", muleContext);
        msg.setOutboundProperty("method", "upperCaseString");
        MuleMessage message = client.send("vm://service", msg);
        assertNotNull(message);
        assertEquals("HELLO", message.getPayloadAsString());
    }


    @Test
    public void testValidCallToReverseMethodSetOnEndpoint() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://service2-reverseString", "hello", null);
        assertNotNull(message);
        assertEquals("olleh", message.getPayloadAsString());
    }

    @Test
    public void testValidCallToUpperCaseMethodSetOnEndpoint() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://service2-upperCaseString", "hello", null);
        assertNotNull(message);
        assertEquals(message.getPayloadAsString(), "HELLO");
    }

    @Test
    public void testValidCallToReverseMethodSetAsHeader() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map props = new HashMap();
        props.put(MuleProperties.MULE_METHOD_PROPERTY, "reverseString");
        MuleMessage message = client.send("vm://service", "hello", props);
        assertNotNull(message);
        assertEquals("olleh", message.getPayloadAsString());
    }

    @Test
    public void testValidCallToUpperCaseMethodSetAsHeader() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map props = new HashMap();
        props.put(MuleProperties.MULE_METHOD_PROPERTY, "upperCaseString");
        MuleMessage message = client.send("vm://service", "hello", props);
        assertNotNull(message);
        assertEquals("HELLO", message.getPayloadAsString());
    }

}
