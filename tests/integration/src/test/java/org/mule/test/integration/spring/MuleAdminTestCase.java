/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.spring;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.module.client.RemoteDispatcher;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class MuleAdminTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/spring/mule-admin-spring.xml";
    }

    @Test
    public void testMuleAdminChannelInSpring() throws Exception
    {
        MuleClient mc = new MuleClient(muleContext);
        RemoteDispatcher rd = mc.getRemoteDispatcher("tcp://localhost:" + dynamicPort.getNumber());
        MuleMessage result = rd.sendToRemoteComponent("appleComponent", "string", null);
        assertNotNull(result);
    }

}
