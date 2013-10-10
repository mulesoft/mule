/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.MuleMessage;
import org.mule.api.transport.DispatchException;
import org.mule.config.i18n.Message;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.transport.rmi.i18n.RmiMessages;

import java.util.HashMap;

import org.junit.Test;

public abstract class AbstractFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    protected String prefix;

    public AbstractFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        
    }    

    // from earlier multiple target test case

    @Test
    public void testCase() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        // send Echo String
        MuleMessage message = client.send("vm://testin", 12, null);
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
        message = client.send("vm://testin", 15, null);
        assertNotNull(message);
        payload = (Integer)message.getPayload();
        assertEquals(payload, new Integer(25));
    }

    // from earlier invocation test case

    private MuleMessage send(String uri, String message) throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        return client.send(prefix + uri, message, new HashMap());
    }

    @Test
    public void testReverseString() throws Exception
    {
        MuleMessage message = send("://localhost/TestService?method=reverseString", "hello");
        assertNotNull(message.getPayload());
        assertEquals("olleh", message.getPayloadAsString());
    }

    @Test
    public void testUpperCaseString() throws Exception
    {
        MuleMessage message = send("://localhost/TestService?method=upperCaseString", "hello");
        assertNotNull(message.getPayload());
        assertEquals("HELLO", message.getPayloadAsString());
    }

    @Test
    public void testNoMethodSet() throws Exception
    {
        try
        {
            send("://localhost/TestService", "hello");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof DispatchException);

            Message message = RmiMessages.messageParamServiceMethodNotSet();
            assertTrue("Expected to start with: " + message.toString() + "\n but was: " + e.getCause().getMessage(), e.getCause().getMessage().startsWith(message.toString()));
        }
    }

    @Test
    public void testBadMethodName() throws Exception
    {
        try
        {
            send("://localhost/TestService?method=foo", "hello");
            fail("exception expected");
        }
        catch (Exception e)
        {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }

    @Test
    public void testBadMethodType() throws Exception
    {
        try
        {
            muleContext.getClient().send("BadType", "hello", null);
            fail("exception expected");
        }
        catch (Exception e)
        {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }

    @Test
    public void testCorrectMethodType() throws Exception
    {
        MuleMessage message = new MuleClient(muleContext).send("GoodType", "hello", null);
        assertNotNull(message);
        assertEquals("olleh", message.getPayloadAsString());
    }   
}
