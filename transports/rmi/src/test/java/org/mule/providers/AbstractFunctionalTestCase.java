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
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
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
            fail("expected error");
        }
        catch (UMOException e)
        {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }

    public void testBadMethodType() throws Exception
    {
        try
        {
            new MuleClient().send("BadType", "hello", null);
            fail("expected error");
        }
        catch (UMOException e)
        {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }

    public void testCorrectMethodType() throws Exception
    {
        UMOMessage message = new MuleClient().send("GoodType", "hello", null);
        assertNotNull(message);
        assertEquals("olleh", message.getPayloadAsString());
    }


    protected String getConfigResources()
    {
        return config;
    }

}