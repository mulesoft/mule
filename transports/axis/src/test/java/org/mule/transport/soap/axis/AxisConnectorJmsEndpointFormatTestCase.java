/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.axis;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transport.DispatchException;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.NullPayload;

import org.apache.axis.AxisFault;

public class AxisConnectorJmsEndpointFormatTestCase extends FunctionalTestCase
{

    public void testAxisOverJmsWithQueueNameSameAsComponentName() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("componentName", new DefaultMuleMessage("test1"));
        assertNotNull(result.getPayload());
        assertEquals("test1", result.getPayloadAsString());
    }
    
    public void testAxisOverJmsWithQueueNameDifferentFromComponentName() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("soapActionDefined", new DefaultMuleMessage("test2"));
        assertNotNull(result.getPayload());
        assertEquals("test2", result.getPayloadAsString());
    }
    
    public void testAxisOverJmsWithoutSettingMethodOnEndpoint() throws Exception
    {
        MuleClient client = new MuleClient();
        Exception exception = null;
        try
        {
            client.send("noMethodDefined", new DefaultMuleMessage("test3"));
        }
        catch (Exception e)
        {
            exception = e;
        }
        assertNotNull(exception);
        assertTrue(exception instanceof DispatchException);
        Throwable rootCause = exception.getCause();
        assertTrue(rootCause instanceof DispatchException);
        assertTrue(rootCause.getMessage().startsWith("Cannot invoke WS call without an Operation."));
    }
    
    public void testAxisOverJmsWithoutSettingSoapAction() throws Exception
    {
        MuleClient client = new MuleClient();
        Exception exception = null;
        try
        {
            MuleMessage msg = client.send("noSoapActionDefined", new DefaultMuleMessage("test4"));
            assertEquals(NullPayload.getInstance(), msg.getPayload());
        }
        catch (Exception e)
        {
            exception = e;
        }
        assertNotNull(exception);
        assertTrue(exception instanceof DispatchException);
        Throwable rootCause = exception.getCause();
        assertTrue(rootCause instanceof AxisFault);
        assertTrue(rootCause.getMessage().startsWith("The AXIS engine could not find a target service to invoke!"));
    }
    
    protected String getConfigResources()
    {
        return "axis-jms-endpoint-format-config.xml";
    }

}
