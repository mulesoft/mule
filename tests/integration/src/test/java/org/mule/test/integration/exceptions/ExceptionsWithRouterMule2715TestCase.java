/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.transformer.TransformerMessagingException;
import org.mule.message.ExceptionMessage;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class ExceptionsWithRouterMule2715TestCase extends AbstractServiceAndFlowTestCase
{
    public static final String MESSAGE = "message";
    public static final long TIMEOUT = 5000L;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/exceptions/exceptions-with-router-mule-2715-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/exceptions/exceptions-with-router-mule-2715-flow.xml"}
        });
    }

    public ExceptionsWithRouterMule2715TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testWithRouter() throws Exception
    {
        doTest("with-router-in");
    }

    @Test
    public void testWithoutRouter() throws Exception
    {
        doTest("without-router-in");
    }

    protected void doTest(String path) throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://" + path, MESSAGE, null);
        MuleMessage response = client.request("vm://error", TIMEOUT);
        assertNotNull(response);
        assertTrue(response.getPayload() instanceof ExceptionMessage);
        assertTrue(((ExceptionMessage) response.getPayload()).getException() instanceof TransformerMessagingException);
    }
}
