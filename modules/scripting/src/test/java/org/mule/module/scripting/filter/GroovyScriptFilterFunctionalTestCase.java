/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class GroovyScriptFilterFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    public GroovyScriptFilterFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        // Groovy really hammers the startup time since it needs to create the
        // interpreter on every start
        setDisposeContextPerClass(true);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "groovy-filter-config-service.xml"},
            {ConfigVariant.FLOW, "groovy-filter-config-flow.xml"}
        });
    }

    @Test
    public void testFilterScript() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://in1", "hello", null);
        MuleMessage response = client.request("vm://out1", RECEIVE_TIMEOUT);
        assertNotNull(response);
        assertEquals("hello", response.getPayload());

        client.dispatch("vm://in1", "1", null);
        response = client.request("vm://out1", RECEIVE_TIMEOUT);
        assertNull(response);
    }
}
