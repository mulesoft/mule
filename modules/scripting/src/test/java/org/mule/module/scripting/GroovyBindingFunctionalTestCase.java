/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.scripting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
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
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("client_request", "Important Message", null);
        MuleMessage response = client.request("client_response", 2000);
        assertNotNull(response);
        assertEquals("Important Message Received by Callout1 Received by Callout2", response.getPayloadAsString());
    }

}
