/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.ExceptionPayload;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.util.ExceptionUtils;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class PGPExpiredIntegrationTestCase extends AbstractServiceAndFlowTestCase
{
    private static Throwable exceptionFromFlow = null;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "pgp-expired-integration-mule-config-service.xml"},
            {ConfigVariant.FLOW, "pgp-expired-integration-mule-config-flow.xml"}
        });
    }

    public PGPExpiredIntegrationTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testEncryptDecrypt() throws Exception
    {
        String payload = "this is a super simple test. Hope it works!!!";
        MuleClient client = muleContext.getClient();

        client.dispatch("vm://in", new DefaultMuleMessage(payload, muleContext));

        MuleMessage message = client.request("vm://out", 5000);
        assertNull(message);

        assertNotNull("flow's exception strategy should have caught an exception", exceptionFromFlow);
        InvalidPublicKeyException ipke =
            ExceptionUtils.getDeepestOccurenceOfType(exceptionFromFlow, InvalidPublicKeyException.class);
        assertNotNull("root cause must be a InvalidPublicKeyException", ipke);
        assertTrue(ipke.getMessage().contains("has expired"));
    }

    public static class ExceptionSaver implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            ExceptionPayload exceptionPayload = event.getMessage().getExceptionPayload();
            exceptionFromFlow = exceptionPayload.getException();

            return null;
        }
    }
}
