/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.spring;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.module.client.RemoteDispatcher;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertNotNull;

public class MuleAdminTestCase extends AbstractServiceAndFlowTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/spring/mule-admin-spring-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/spring/mule-admin-spring-flow.xml"}});
    }

    public MuleAdminTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testMuleAdminChannelInSpring() throws Exception
    {
        MuleClient mc = new MuleClient(muleContext);
        RemoteDispatcher rd = mc.getRemoteDispatcher("tcp://localhost:" + port1.getNumber());
        MuleMessage result = rd.sendToRemoteComponent("appleComponent", "string", null);
        assertNotNull(result);
    }
}
