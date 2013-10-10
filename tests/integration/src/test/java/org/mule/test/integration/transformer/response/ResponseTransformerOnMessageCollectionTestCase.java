/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.transformer.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class ResponseTransformerOnMessageCollectionTestCase extends AbstractServiceAndFlowTestCase
{

    public ResponseTransformerOnMessageCollectionTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ConfigVariant.SERVICE, "org/mule/test/integration/transformer/response/response-transformer-on-message-collection-service-config.xml"},
                {ConfigVariant.FLOW, "org/mule/test/integration/transformer/response/response-transformer-on-message-collection-flow-config.xml"}
        });
    }

    @Test
    public void transformedDataIsNotLost() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://testInput", TEST_MESSAGE, null);

        assertEquals("foo", response.getPayload());
        assertFalse(response instanceof MuleMessageCollection);
    }
}
