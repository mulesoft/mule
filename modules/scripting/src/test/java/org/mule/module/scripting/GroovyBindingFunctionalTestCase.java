/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;


public class GroovyBindingFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    public GroovyBindingFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "groovy-binding-config-service.xml"},
            {ConfigVariant.FLOW, "groovy-binding-config-flow.xml"}
        });
    }

    @Test
    public void testBindingCallout() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("client_request", "Important Message", null);
        MuleMessage response = client.request("client_response", 2000);
        assertNotNull(response);
        assertEquals("Important Message Received by Callout1 Received by Callout2", response.getPayloadAsString());
    }
}
