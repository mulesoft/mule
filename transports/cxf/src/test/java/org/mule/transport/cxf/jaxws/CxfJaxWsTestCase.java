/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.jaxws;

import org.mule.api.MuleMessage;
import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class CxfJaxWsTestCase extends FunctionalTestCase
{
    public void testEchoService() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("cxf:http://localhost:63081/services/Echo?method=echo", "Hello!",
            null);
        assertEquals("Hello!", result.getPayload());
    }

    protected String getConfigResources()
    {
        return "jaxws-conf.xml";
    }

}
