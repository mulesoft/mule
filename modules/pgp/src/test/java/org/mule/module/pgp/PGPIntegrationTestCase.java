/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PGPIntegrationTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "pgp-integration-mule-config.xml";
    }

    @Test
    public void testEncryptDecrypt() throws Exception
    {
        String payload = "this is a super simple test. Hope it works!!!";
        MuleClient client = new MuleClient(muleContext);
        
        client.send("vm://in", new DefaultMuleMessage(payload, muleContext));
        MuleMessage message = client.request("vm://out", 5000);
        assertEquals(payload, message.getPayloadAsString());
    }

}
