/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.vm.functional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MutableMuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import java.util.Collections;

import org.junit.Test;

/**
 * Test the propagation of a property in different scopes and in synchronous vs.
 * asynchronous flows.
 */
public class PropertyScopesTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "vm/property-scopes.xml";
    }

    @Test
    public void noPropagationOfInboundScopeSynchronous() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, Collections.singletonMap("foo", "bar"), null,
                                                     null, muleContext);

        MuleMessage response = client.send("vm://in-synch", message);
        assertNotNull(response);
        assertNull("Property should not have been propogated for this scope", response.getInboundProperty("foo"));
    }

    @Test
    public void noPropagationOfOutboundScopeSynchronous() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MutableMuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        message.setOutboundProperty("foo", "bar");
        MuleMessage response = client.send("vm://in-synch", message);
        assertNotNull(response);
        assertNull("Property should not have been propogated for this scope", response.getOutboundProperty("foo"));
    }

    @Test
    public void noPropagationOfInboundScopeAsynchronous() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MutableMuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        message.setOutboundProperty("foo", "bar");
        client.dispatch("vm://in-asynch", message);
        MuleMessage response = client.request("vm://out-asynch", RECEIVE_TIMEOUT);
        assertNotNull(response);
        assertNull("Property should not have been propogated for this scope", response.getInboundProperty("foo"));
    }

    @Test
    public void noPropagationOfOutboundScopeAsynchronous() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MutableMuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        message.setOutboundProperty("foo", "bar");
        client.dispatch("vm://in-asynch", message);
        MuleMessage response = client.request("vm://out-asynch", RECEIVE_TIMEOUT);
        assertNotNull(response);
        assertNull("Property should not have been propogated for this scope", response.getOutboundProperty("foo"));
    }

}
