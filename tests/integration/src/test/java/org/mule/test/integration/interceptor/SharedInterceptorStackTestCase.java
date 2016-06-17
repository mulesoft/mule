/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.interceptor;

import static org.junit.Assert.assertEquals;

import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.interceptor.Interceptor;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessor;

import org.junit.Test;

public class SharedInterceptorStackTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "shared-interceptor-stack-flow.xml";
    }

    @Test
    public void testSharedInterceptorOnServiceOne() throws Exception
    {
        MuleMessage response = flowRunner("serviceOne").withPayload(getTestMuleMessage(TEST_MESSAGE)).run().getMessage();
        assertEquals(TEST_MESSAGE + " CustomInterceptor ComponentOne", response.getPayload());
    }

    @Test
    public void testSharedInterceptorOnServiceTwo() throws Exception
    {
        MuleMessage response = flowRunner("serviceTwo").withPayload(getTestMuleMessage(TEST_MESSAGE)).run().getMessage();
        assertEquals(TEST_MESSAGE + " CustomInterceptor ComponentTwo", response.getPayload());
    }

    public static class CustomInterceptor extends AbstractInterceptingMessageProcessor implements Interceptor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            MuleMessage message = event.getMessage();
            String payload = message.getPayload().toString();
            event.setMessage(new DefaultMuleMessage(payload + " CustomInterceptor", event.getMessage(), event.getMuleContext()));
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
