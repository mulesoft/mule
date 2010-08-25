/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("componentName", new DefaultMuleMessage("test1", muleContext));
        assertNotNull(result.getPayload());
        assertEquals("test1", result.getPayloadAsString());
    }
    
    public void testAxisOverJmsWithQueueNameDifferentFromComponentName() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("soapActionDefined", new DefaultMuleMessage("test2", muleContext));
        assertNotNull(result.getPayload());
        assertEquals("test2", result.getPayloadAsString());
    }
    
    public void testAxisOverJmsWithoutSettingMethodOnEndpoint() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage result = client.send("noMethodDefined", new DefaultMuleMessage("test3", muleContext));
        assertNotNull(result);
        assertNotNull("Exception expected", result.getExceptionPayload());
        assertTrue(result.getExceptionPayload().getException() instanceof DispatchException);
        assertTrue(result.getExceptionPayload().getException().getMessage().startsWith("Cannot invoke WS call without an Operation."));
    }
    
    public void testAxisOverJmsWithoutSettingSoapAction() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage result = client.send("noSoapActionDefined", new DefaultMuleMessage("test4", muleContext));
        assertNotNull(result);
        assertNotNull("Exception expected", result.getExceptionPayload());
        assertTrue(result.getExceptionPayload().getException() instanceof DispatchException);
        assertTrue(result.getExceptionPayload().getRootException() instanceof AxisFault);
        assertTrue(result.getExceptionPayload().getRootException().getMessage().startsWith("The AXIS engine could not find a target service to invoke!"));
    }
    
    protected String getConfigResources()
    {
        return "axis-jms-endpoint-format-config.xml";
    }

}
