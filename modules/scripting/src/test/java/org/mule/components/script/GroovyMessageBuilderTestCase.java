/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.components.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class GroovyMessageBuilderTestCase extends AbstractServiceAndFlowTestCase
{    
    public GroovyMessageBuilderTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "groovy-messagebuilder-config-service.xml"},
            {ConfigVariant.FLOW, "groovy-messagebuilder-config-flow.xml"}
        });
    }      
    
    @Test
    public void testFunctionBehaviour() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage m = client.send("groovy1.endpoint", "Test:", null);
        assertNotNull(m);
        assertEquals("Test: A B Received", m.getPayloadAsString());
    }

}
