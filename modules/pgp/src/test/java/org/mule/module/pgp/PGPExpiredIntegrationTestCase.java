/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.pgp;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

public class PGPExpiredIntegrationTestCase extends AbstractServiceAndFlowTestCase
{

    public PGPExpiredIntegrationTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "pgp-expired-integration-mule-config-service.xml"},
            {ConfigVariant.FLOW, "pgp-expired-integration-mule-config-flow.xml"}
        });
    }

    @Test
    public void testEncryptDecrypt() throws Exception
    {
        String payload = "this is a super simple test. Hope it works!!!";
        MuleClient client = new MuleClient(muleContext);
        
        MuleMessage exceptionMessage = client.send("vm://in", new DefaultMuleMessage(payload, muleContext));
        
        assertNotNull(exceptionMessage);
        
        MuleMessage message = client.request("vm://out", 1000);
        assertNull(message);
    }

}
