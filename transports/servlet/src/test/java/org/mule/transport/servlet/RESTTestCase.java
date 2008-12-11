/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.TestCaseWatchdog;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class RESTTestCase extends FunctionalTestCase
{
    public RESTTestCase()
    {
        super();
        
        // Do not fail test case upon timeout because this probably just means
        // that the 3rd-party web service is off-line.
        setFailOnTimeout(false);
    }

    protected String getConfigResources()
    {
        return "rest-functional-test.xml";
    }
    
    public void testRest1ParamPost() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage reply  = client.send("vm://in1", new DefaultMuleMessage("IBM"));
        
        assertNotNull(reply);
        assertNotNull(reply.getPayloadAsString());
        assertTrue(reply.getPayloadAsString().indexOf("Symbol&gt;IBM&lt;") > -1);
    }
    
    public void testRest2ParamsPost() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage reply  = client.send("vm://in2", new DefaultMuleMessage(new Object[]{"MTL","MTL"}));
        
        assertNotNull(reply.getPayloadAsString());
        assertTrue(reply.getPayloadAsString().indexOf(">1</double>") > -1);
    }
    
    public void testRest1ParamGet() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage reply  = client.send("vm://in3", new DefaultMuleMessage(new Object[]{"IBM"}));
        
        assertNotNull(reply);
        String replyStr = reply.getPayloadAsString();
        assertNotNull(replyStr);
        assertTrue("'Symbol&gt;IBM&lt;' not found in reply: " + replyStr, replyStr.indexOf("Symbol&gt;IBM&lt;") > -1);
    }
    
    public void testRest2ParamsGet() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage reply  = client.send("vm://in4", new DefaultMuleMessage(new Object[]{"MTL","MTL"}));
        
        String replyStr = reply.getPayloadAsString();
        assertNotNull(replyStr);
        assertTrue("'>1</double>' not found in reply: " + replyStr, replyStr.indexOf(">1</double>") > -1);
    }

}


