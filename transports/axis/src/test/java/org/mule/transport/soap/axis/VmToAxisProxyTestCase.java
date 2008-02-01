/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.axis;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class VmToAxisProxyTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "vm-to-axis-proxy-mule-config.xml";
    }

    public void testWSProxy() throws Exception
    {
        if (isOffline("org.mule.transport.soap.axis.VmToAxisProxyTestCase.testWSProxy()"))
        {
            return;
        }

        MuleClient client = new MuleClient();
        MuleMessage result = client.send("vm://proxy", "ibm", null);
        assertNotNull(result);
    }

}
