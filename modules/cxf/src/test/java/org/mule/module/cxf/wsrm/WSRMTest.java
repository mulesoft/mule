/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.wsrm;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.client.DefaultLocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WSRMTest extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Override
    protected String getConfigResources()
    {
        return "org/mule/module/cxf/wsrm/wsrm-conf.xml";
    }

    @Test
    public void testAnonymous() throws Exception
    {
        MuleClient client = new DefaultLocalMuleClient(muleContext);
        MuleMessage result = client.send("anonymousReplyClientEndpoint", new DefaultMuleMessage("test", muleContext));
        assertEquals("Hello test", result.getPayloadAsString());
    }

    @Test
    public void testDecoupled() throws Exception
    {
        MuleClient client = new DefaultLocalMuleClient(muleContext);
        MuleMessage result = client.send("decoupledClientEndpoint", new DefaultMuleMessage("test", muleContext));
        assertEquals("Hello test", result.getPayloadAsString());
    }

}


