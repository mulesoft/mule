/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.transport.DispatchException;
import org.mule.tck.junit4.FunctionalTestCase;

import org.apache.axis.AxisFault;
import org.junit.Test;

public class AxisConnectorJmsEndpointFormatTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "axis-jms-endpoint-format-config.xml";
    }

    @Test
    public void testAxisOverJmsWithQueueNameSameAsComponentName() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("componentName", new DefaultMuleMessage("test1", muleContext));
        assertNotNull(result.getPayload());
        assertEquals("test1", result.getPayloadAsString());
    }

    @Test
    public void testAxisOverJmsWithQueueNameDifferentFromComponentName() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("soapActionDefined", new DefaultMuleMessage("test2", muleContext));
        assertNotNull(result.getPayload());
        assertEquals("test2", result.getPayloadAsString());
    }

    @Test
    public void testAxisOverJmsWithoutSettingMethodOnEndpoint() throws Exception
    {
        try
        {
            MuleClient client = muleContext.getClient();
            client.send("noMethodDefined", new DefaultMuleMessage("test3", muleContext));
            fail("Exception expected");
        }
        catch (DispatchException e)
        {
            assertTrue(e.getMessage().startsWith("Cannot invoke WS call without an Operation."));
        }
    }

    @Test
    public void testAxisOverJmsWithoutSettingSoapAction() throws Exception
    {
        try
        {
            MuleClient client = muleContext.getClient();
            client.send("noSoapActionDefined", new DefaultMuleMessage("test4", muleContext));
            fail("Exception expected");
        }
        catch (DispatchException e)
        {
            assertTrue(e.getCause() instanceof AxisFault);
            assertTrue(e.getCause().getMessage().startsWith("The AXIS engine could not find a target service to invoke!"));
        }
    }
}
