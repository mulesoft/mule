/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.soap.axis;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class AxisConnectorHttpsTestCase extends FunctionalTestCase
{
    
    public void testHttpsConnection() throws Exception{
        MuleClient client = new MuleClient();
        UMOMessage m = client.send("axis:https://localhost:82/TestUMO?method=echo",new MuleMessage("hello"));
        assertNotNull(m);
    }
    
    protected String getConfigResources()
    {
        return "org/mule/test/integration/providers/soap/axis/axis-https-connector-config.xml";
    }
    
}


