/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
