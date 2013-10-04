/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf.functional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.message.ExceptionMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transformer.AbstractTransformer;

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

public class CxfClientExceptionStrategyTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "cxf-client-exception-strategy-flow.xml";
    }

    @Test
    public void testCxfClientExceptionStrategy() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("hello", (Map<String,Object>)null, muleContext);
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://helloClient", request);

        MuleMessage out = client.request("vm://out", org.mule.tck.FunctionalTestCase.RECEIVE_TIMEOUT);

        assertNotNull(out);
        assertTrue(out.getPayload() instanceof ExceptionMessage);
        assertTrue(((String)((ExceptionMessage) out.getPayload()).getPayload()).contains("APPEND"));
    }

    public static class ThrowExceptionTransformer extends AbstractTransformer
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            throw new MessagingException(event, new Throwable("Error transforming message"));
        }

        @Override
        protected Object doTransform(Object src, String enc) throws TransformerException
        {
            return src;
        }
    }

}
