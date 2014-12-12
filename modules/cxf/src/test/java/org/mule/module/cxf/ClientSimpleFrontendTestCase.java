/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class ClientSimpleFrontendTestCase extends AbstractServiceAndFlowTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public ClientSimpleFrontendTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "aegis-conf-service.xml"},
            {ConfigVariant.FLOW, "aegis-conf-flow.xml"},
            {ConfigVariant.FLOW, "aegis-conf-flow-httpn.xml"}
        });
    }

    @Test
    public void testEchoWsdl() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("vm://test", "some payload", null);

        assertNotNull(result.getPayload());
        assertEquals("Hello some payload", result.getPayload());
    }
}


