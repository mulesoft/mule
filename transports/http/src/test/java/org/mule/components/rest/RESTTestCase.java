/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.components.rest;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

public class RESTTestCase extends FunctionalTestCase
{    
    protected String getConfigResources()
    {
        return "rest-functional-test.xml";
    }
    
    public RESTTestCase() throws UMOException
    {
        super();
        this.setDisposeManagerPerSuite(true);
    }
    
    public void testRest1ParamPost() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage reply  = client.send("vm://in1", new MuleMessage("IBM"));
        
        assertNotNull(reply);
        assertNotNull(reply.getPayloadAsString());
        assertTrue(reply.getPayloadAsString().indexOf("Symbol&gt;IBM&lt;") > -1);
    }
    
    public void testRest2ParamsPost() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage reply  = client.send("vm://in2", new MuleMessage(new Object[]{"MTL","MTL"}));
        
        assertNotNull(reply.getPayloadAsString());
        assertTrue(reply.getPayloadAsString().indexOf(">1</double>") > -1);
    }
    
    public void testRest1ParamGet() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage reply  = client.send("vm://in3", new MuleMessage(new Object[]{"IBM"}));
        
        assertNotNull(reply);
        assertNotNull(reply.getPayloadAsString());
        assertTrue(reply.getPayloadAsString().indexOf("Symbol&gt;IBM&lt;") > -1);
    }
    
    public void testRest2ParamsGet() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage reply  = client.send("vm://in4", new MuleMessage(new Object[]{"MTL","MTL"}));
        
        assertNotNull(reply.getPayloadAsString());
        assertTrue(reply.getPayloadAsString().indexOf(">1</double>") > -1);
    }

}


