/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ejb;

import org.mule.config.i18n.Message;
import org.mule.extras.client.MuleClient;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.providers.rmi.RmiConnector;
import org.mule.providers.rmi.i18n.RmiMessages;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.DispatchException;

import java.util.HashMap;

/**
 * test RMI object invocations
 */
public class EjbFunctionalTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "ejb-functional-test.xml";
    }

    private UMOMessage send(String uri, String message) throws Exception
    {
        MuleClient client = new MuleClient();
        return client.send(uri, message, new HashMap());
    }

    public void testReverseString() throws Exception
    {
        UMOMessage message = send("ejb://localhost/TestService?method=reverseString", "hello");
        assertNotNull(message.getPayload());
        assertEquals("olleh", message.getPayloadAsString());
    }

    public void testUpperCaseString() throws Exception
    {
        UMOMessage message = send("ejb://localhost/TestService?method=upperCaseString", "hello");
        assertNotNull(message.getPayload());
        assertEquals("HELLO", message.getPayloadAsString());
    }

    public void testNoMethodSet() throws Exception
    {
        try
        {
            send("ejb://localhost/TestService", "hello");

        }
        catch (UMOException e)
        {
            assertTrue(e instanceof DispatchException);
            
            Message message = RmiMessages.messageParamServiceMethodNotSet();
            assertTrue(e.getMessage().startsWith(message.toString()));
        }
    }

    public void testBadMethodName() throws Exception
    {
        try
        {
            send("ejb://localhost/TestService?method=foo", "hello");
        }
        catch (UMOException e)
        {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }

    public void testBadMethodType() throws Exception
    {
        // moving this to xml config requires endpoint properties
        // MULE-1790
        UMOEndpoint ep = new MuleEndpoint("ejb://localhost/TestService?method=reverseString", false);
        ep.setProperty(RmiConnector.PROPERTY_SERVICE_METHOD_PARAM_TYPES, StringBuffer.class.getName());
        try
        {
            ep.send(getTestEvent("hello", ep));
        }
        catch (UMOException e)
        {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }

    public void testCorrectMethodType() throws Exception
    {
        // moving this to xml config requires endpoint properties
        // MULE-1790
        UMOEndpoint ep = new MuleEndpoint("ejb://localhost/TestService?method=reverseString", false);
        ep.setProperty(RmiConnector.PROPERTY_SERVICE_METHOD_PARAM_TYPES, String.class.getName());
        try
        {
            ep.send(getTestEvent("hello", ep));
        }
        catch (UMOException e)
        {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }

}
