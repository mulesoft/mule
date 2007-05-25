/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import org.mule.config.i18n.Message;
import org.mule.extras.client.MuleClient;
import org.mule.providers.rmi.i18n.RmiMessages;
import org.mule.providers.rmi.RmiConnector;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.DispatchException;

import java.util.HashMap;

public abstract class AbstractFunctionalTestCase extends FunctionalTestCase
{

    private String prefix;
    private String config;

    public AbstractFunctionalTestCase(String prefix, String config)
    {
        this.prefix = prefix;
        this.config = config;
    }

    // from earlier multiple target test case

    public void testCase() throws Exception
    {
        MuleClient client = new MuleClient();

        // send Echo String
        UMOMessage message = client.send("vm://testin", new Integer(12), null);
        assertNotNull(message);
        Integer payload = (Integer)message.getPayload();
        assertEquals(payload, new Integer(22));

        // send String
        message = client.send("vm://testin", "test matching component first time", null);
        assertNotNull(message);
        assertEquals((String)message.getPayload(), "emit tsrif tnenopmoc gnihctam tset");

        // send String
        message = client.send("vm://testin", "test mathching component second time", null);
        assertNotNull(message);
        assertEquals((String)message.getPayload(), "emit dnoces tnenopmoc gnihchtam tset");

        // send Integer
        message = client.send("vm://testin", new Integer(15), null);
        assertNotNull(message);
        payload = (Integer)message.getPayload();
        assertEquals(payload, new Integer(25));
    }

    // from earlier invocation test case

    private UMOMessage send(String uri, String message) throws Exception
    {
        MuleClient client = new MuleClient();
        return client.send(prefix + uri, message, new HashMap());
    }

    public void testReverseString() throws Exception
    {
        UMOMessage message = send("://localhost/TestService?method=reverseString", "hello");
        assertNotNull(message.getPayload());
        assertEquals("olleh", message.getPayloadAsString());
    }

    public void testUpperCaseString() throws Exception
    {
        UMOMessage message = send("://localhost/TestService?method=upperCaseString", "hello");
        assertNotNull(message.getPayload());
        assertEquals("HELLO", message.getPayloadAsString());
    }

    public void testNoMethodSet() throws Exception
    {
        try
        {
            send("://localhost/TestService", "hello");
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
            send("://localhost/TestService?method=foo", "hello");
        }
        catch (UMOException e)
        {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }

    public void testBadMethodType() throws Exception
    {
        UMOEndpoint ep =
                managementContext.getRegistry().getEndpointFromUri(prefix + "://localhost/TestService?method=reverseString");
        // this fails here because of an NPE.
        // what we really want is (i think) is to be able to specify the endpoint proeprties
        // in the xml config, but i don't know how to do that, so i sent an email to dev.
        // once that is resolved, we can fix this.  otehrwise, please leave as failing for now.
        // MULE-1790
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
        UMOEndpoint ep =
                managementContext.getRegistry().getEndpointFromUri(prefix + "://localhost/TestService?method=reverseString");
        // this fails here because of an NPE.
        // what we really want is (i think) is to be able to specify the endpoint proeprties
        // in the xml config, but i don't know how to do that, so i sent an email to dev.
        // once that is resolved, we can fix this.  otehrwise, please leave as failing for now.
        // MULE-1790
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


    protected String getConfigResources()
    {
        return config;
    }

}