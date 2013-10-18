/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.interceptor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.interceptor.Interceptor;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;

public class SharedInterceptorStackTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "shared-interceptor-stack-service.xml"},
            {ConfigVariant.FLOW, "shared-interceptor-stack-flow.xml"}
        });
    }

    public SharedInterceptorStackTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testSharedInterceptorOnServiceOne() throws MuleException
    {
        MuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://stackOne", TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE + " CustomInterceptor ComponentOne", response.getPayload());
    }

    @Test
    public void testSharedInterceptorOnServiceTwo() throws MuleException
    {
        MuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://stackTwo", TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE + " CustomInterceptor ComponentTwo", response.getPayload());
    }

    public static class CustomInterceptor extends AbstractInterceptingMessageProcessor implements Interceptor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            MuleMessage message = event.getMessage();
            String payload = message.getPayload().toString();
            message.setPayload(payload + " CustomInterceptor");
            return processNext(event);
        }
    }

    public static class CustomComponent
    {
        private String appendString;

        public String process(String input)
        {
            return input + appendString;
        }

        public void setAppendString(String string)
        {
            this.appendString = string;
        }
    }
}
