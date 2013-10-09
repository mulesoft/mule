/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.scripting.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
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
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://in1", "hello", null);
        MuleMessage response = client.request("vm://out1", RECEIVE_TIMEOUT);
        assertNotNull(response);
        assertEquals("hello", response.getPayload());

        client.dispatch("vm://in1", "1", null);
        response = client.request("vm://out1", RECEIVE_TIMEOUT);
        assertNull(response);
    }
}
