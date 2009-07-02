/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.scripting;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

public abstract class AbstractPoundsScriptingExampleTestCase extends AbstractScriptingExampleTestCase
{    
    protected String getCurrency()
    {
        return "GBP";
    }
    
    public void testChangeAlgorithm() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage reply = client.send("vm://input", new Double(3.88), null);
        
        assertNotNull(reply);
        assertNotNull(reply.getPayload());
        assertEquals("[1 two_pounds, 1 pounds, 1 fifty_pence, 1 twenty_pence, 1 ten_pence, 1 five_pence, 1 two_pence, 1 pennies]", reply.getPayloadAsString());
    }

    public void testAccumulator() throws Exception
    {
        MuleClient client = new MuleClient();
        client.send("vm://input", new Double(1.08), null);
        client.send("vm://input", new Double(1.80), null);
        MuleMessage reply = client.send("vm://input", new Double(1.00), null);
        
        assertNotNull(reply);
        assertNotNull(reply.getPayload());
        assertEquals("[1 two_pounds, 1 pounds, 1 fifty_pence, 1 twenty_pence, 1 ten_pence, 1 five_pence, 1 two_pence, 1 pennies]", reply.getPayloadAsString());
    }
}
