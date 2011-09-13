/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class RESTTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "rest-functional-test.xml";
    }

    @Override
    protected boolean isFailOnTimeout()
    {
        // Do not fail test case upon timeout because this probably just means
        // that the 3rd-party web service is off-line.
        return false;
    }
    
    @Test
    public void testRest1ParamPost() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply  = client.send("vm://in1", new DefaultMuleMessage("IBM", muleContext));
        
        assertNotNull(reply);
        assertNotNull(reply.getPayloadAsString());
        assertTrue(reply.getPayloadAsString().indexOf("Symbol&gt;IBM&lt;") > -1);
    }
    
    @Test
    public void testRest2ParamsPost() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply  = client.send("vm://in2", new DefaultMuleMessage(new Object[]{"ARS","ARS"}, muleContext));
        
        assertNotNull(reply.getPayloadAsString());
        assertTrue(reply.getPayloadAsString().indexOf(">1</double>") > -1);
    }
    
    @Test
    public void testRest1ParamGet() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply  = client.send("vm://in3", new DefaultMuleMessage(new Object[]{"IBM"}, muleContext));
        
        assertNotNull(reply);
        String replyStr = reply.getPayloadAsString();
        assertNotNull(replyStr);
        assertTrue("'Symbol&gt;IBM&lt;' not found in reply: " + replyStr, replyStr.indexOf("Symbol&gt;IBM&lt;") > -1);
    }
    
    @Test
    public void testRest2ParamsGet() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply  = client.send("vm://in4", new DefaultMuleMessage(new Object[]{"ARS","ARS"}, muleContext));
        
        String replyStr = reply.getPayloadAsString();
        assertNotNull(replyStr);
        assertTrue("'>1</double>' not found in reply: " + replyStr, replyStr.indexOf(">1</double>") > -1);
    }

}


