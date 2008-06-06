/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.scripting;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class GroovyScriptFunctionalTestCase extends FunctionalTestCase
{
    //@Override
    protected String getConfigResources()
    {
        return "groovy-component-config.xml";
    }

    public void testInlineScript() throws Exception
    {
        MuleClient client = new MuleClient();
        client.send("vm://in1", "Important Message", null);
        MuleMessage response = client.request("vm://out1", 1000);
        assertNotNull(response);
        assertEquals("Important Message Received", response.getPayloadAsString());
    }
    
    public void testFileBasedScript() throws Exception
    {
        MuleClient client = new MuleClient();
        client.send("vm://in2", "Important Message", null);
        MuleMessage response = client.request("vm://out2", 1000);
        assertNotNull(response);
        assertEquals("Important Message Received", response.getPayloadAsString());
    }
    
    public void testReferencedScript() throws Exception
    {
        MuleClient client = new MuleClient();
        client.send("vm://in3", "Important Message", null);
        MuleMessage response = client.request("vm://out3", 1000);
        assertNotNull(response);
        assertEquals("Important Message Received", response.getPayloadAsString());
    }    

    public void testScriptVariables() throws Exception
    {
        MuleClient client = new MuleClient();
        client.send("vm://in4", "Important Message", null);
        MuleMessage response = client.request("vm://out4", 1000);
        assertNotNull(response);
        assertEquals("Important Message Received A-OK", response.getPayloadAsString());
    }    
}


