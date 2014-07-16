/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class VmToAxisProxyTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "vm-to-axis-proxy-mule-config.xml";
    }

    @Test
    public void testWSProxy() throws Exception
    {
        if (isOffline("org.mule.transport.soap.axis.VmToAxisProxyTestCase.testWSProxy()"))
        {
            return;
        }

        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("vm://proxy", "ibm", null);
        assertNotNull(result);
    }
}
