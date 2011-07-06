/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.spring;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.module.client.RemoteDispatcher;
import org.mule.tck.IntegrationDynamicPortTestCase;

public class MuleAdminTestCase extends IntegrationDynamicPortTestCase
{
    public MuleAdminTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);

    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE,
            "org/mule/test/integration/spring/mule-admin-spring.xml"}

        });
    }

    @Test
    public void testMuleAdminChannelInSpring() throws Exception
    {
        MuleClient mc = new MuleClient(muleContext);
        RemoteDispatcher rd = mc.getRemoteDispatcher("tcp://localhost:" + getPorts().get(0));
        MuleMessage result = rd.sendToRemoteComponent("appleComponent", "string", null);
        assertNotNull(result);
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 1;
    }
}
