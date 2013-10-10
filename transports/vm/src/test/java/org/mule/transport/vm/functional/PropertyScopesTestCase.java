/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.vm.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Test the propagation of a property in different scopes and in synchronous vs.
 * asynchronous flows.
 */
public class PropertyScopesTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "vm/property-scopes.xml";
    }

    @Test
    public void noPropagationOfInboundScopeSynchronous() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        message.setProperty("foo", "bar", PropertyScope.INBOUND);

        MuleMessage response = client.send("vm://in-synch", message);
        assertNotNull(response);
        assertNull("Property should not have been propogated for this scope",
            response.getProperty("foo", PropertyScope.INBOUND));
    }

    @Test
    public void noPropagationOfOutboundScopeSynchronous() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        message.setProperty("foo", "bar", PropertyScope.OUTBOUND);

        MuleMessage response = client.send("vm://in-synch", message);
        assertNotNull(response);
        assertNull("Property should not have been propogated for this scope",
            response.getProperty("foo", PropertyScope.OUTBOUND));
    }

    @Test
    @Ignore
    // MULE-5820  invocation properties lost after request-response outbound endpoint
    public void propagationOfInvocationScopeSynchronous() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        message.setProperty("foo", "bar", PropertyScope.INVOCATION);

        MuleMessage response = client.send("vm://in-synch", message);
        assertNotNull(response);
        // VM Transport does not propagate invocation properties. The properties
        // available here have not been returned from services, but rather conserved
        // in the message dispatcher
        assertEquals("bar", response.getProperty("foo", PropertyScope.INVOCATION));
    }

    @Test
    @Ignore
    // MULE-5302
    public void propagationOfSessionScopeSynchronous() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        message.setProperty("foo", "bar", PropertyScope.SESSION);

        MuleMessage response = client.send("vm://in-synch", message);
        assertNotNull(response);
        assertEquals("bar", response.getProperty("foo", PropertyScope.SESSION));
    }

    @Test
    public void noPropagationOfInboundScopeAsynchronous() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        message.setProperty("foo", "bar", PropertyScope.INBOUND);
        client.dispatch("vm://in-asynch", message);
        MuleMessage response = client.request("vm://out-asynch", RECEIVE_TIMEOUT);
        assertNotNull(response);
        assertNull("Property should not have been propogated for this scope",
            response.getProperty("foo", PropertyScope.INBOUND));
    }

    @Test
    public void noPropagationOfOutboundScopeAsynchronous() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        message.setProperty("foo", "bar", PropertyScope.OUTBOUND);
        client.dispatch("vm://in-asynch", message);
        MuleMessage response = client.request("vm://out-asynch", RECEIVE_TIMEOUT);
        assertNotNull(response);
        assertNull("Property should not have been propogated for this scope",
            response.getProperty("foo", PropertyScope.OUTBOUND));
    }

    @Test
    public void noPropagationOfInvocationScopeAsynchronous() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        message.setProperty("foo", "bar", PropertyScope.INVOCATION);
        client.dispatch("vm://in-asynch", message);
        MuleMessage response = client.request("vm://out-asynch", RECEIVE_TIMEOUT);
        assertNotNull(response);
        // VM Transport does not propagate invocation properties
        assertNull(response.getProperty("foo", PropertyScope.INVOCATION));
    }

    @Test
    @Ignore
    // MULE-5302
    public void propagationOfSessionScopeAsynchronous() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        message.setProperty("foo", "bar", PropertyScope.SESSION);
        client.dispatch("vm://in-asynch", message);
        MuleMessage response = client.request("vm://out-asynch", RECEIVE_TIMEOUT);
        assertNotNull(response);
        assertEquals("bar", response.getProperty("foo", PropertyScope.SESSION));
    }
}
