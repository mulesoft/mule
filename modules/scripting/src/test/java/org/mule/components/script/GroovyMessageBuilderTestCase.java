/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.components.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
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
        MuleClient client = muleContext.getClient();
        MuleMessage m = client.send("groovy1.endpoint", "Test:", null);
        assertNotNull(m);
        assertEquals("Test: A B Received", m.getPayloadAsString());
    }
}
