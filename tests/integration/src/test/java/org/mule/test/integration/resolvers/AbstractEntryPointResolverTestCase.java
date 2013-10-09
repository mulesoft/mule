/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.resolvers;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public abstract class AbstractEntryPointResolverTestCase extends AbstractServiceAndFlowTestCase
{
    public AbstractEntryPointResolverTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    protected void doTest(String path, Object payload, String result) throws Exception
    {
        doTest(path, payload, result, null);
    }

    protected void doTest(String path, Object payload, String result, Map properties) throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("vm://" + path, payload, properties);
        assertEquals(result, response.getPayloadAsString());
    }
}
