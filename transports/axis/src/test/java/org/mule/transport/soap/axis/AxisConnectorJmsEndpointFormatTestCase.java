/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transport.DispatchException;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.apache.axis.AxisFault;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AxisConnectorJmsEndpointFormatTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "axis-jms-endpoint-format-config.xml";
    }

    @Test
    public void testAxisOverJmsWithQueueNameSameAsComponentName() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("componentName", new DefaultMuleMessage("test1", muleContext));
        assertNotNull(result.getPayload());
        assertEquals("test1", result.getPayloadAsString());
    }

    @Test
    public void testAxisOverJmsWithQueueNameDifferentFromComponentName() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("soapActionDefined", new DefaultMuleMessage("test2", muleContext));
        assertNotNull(result.getPayload());
        assertEquals("test2", result.getPayloadAsString());
    }

    @Test
    public void testAxisOverJmsWithoutSettingMethodOnEndpoint() throws Exception
    {
        try
        {
            new MuleClient(muleContext).send("noMethodDefined", new DefaultMuleMessage("test3", muleContext));
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
            new MuleClient(muleContext).send("noSoapActionDefined", new DefaultMuleMessage("test4", muleContext));
            fail("Exception expected");
        }
        catch (DispatchException e)
        {
            assertTrue(e.getCause() instanceof AxisFault);
            assertTrue(e.getCause().getMessage().startsWith("The AXIS engine could not find a target service to invoke!"));
        }
    }

}
