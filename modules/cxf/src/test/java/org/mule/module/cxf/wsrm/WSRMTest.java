/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.wsrm;

import static org.junit.Assert.assertEquals;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.client.DefaultLocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class WSRMTest extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Override
    protected String getConfigFile()
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


