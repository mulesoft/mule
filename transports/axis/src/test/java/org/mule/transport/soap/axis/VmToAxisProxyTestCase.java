/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class VmToAxisProxyTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
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

        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("vm://proxy", "ibm", null);
        assertNotNull(result);
    }

}
