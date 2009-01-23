/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class ProtocolTestCase extends FunctionalTestCase
{
    
    public void testEchoService() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("cxf:http://localhost:63081/services/Echo?method=echo", "Hello!",
            null);
        assertEquals("Hello Transformed!", result.getPayload());
    }

    @Override
    protected String getConfigResources()
    {
        return "protocol-conf.xml";
    }


}
