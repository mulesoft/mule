/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.scripting;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class ScriptingExampleTestCase extends FunctionalTestCase
{    
    protected String getConfigResources()
    {
        return "scripting-config.xml";
    }

    public void testScriptingExample() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage reply = client.send("vm://scripting", new DefaultMuleMessage("mule"));
        
        assertNotNull(reply);
        assertNotNull(reply.getPayload());
        assertEquals("[Hello, :-)]", reply.getPayloadAsString()); 
    }
}


