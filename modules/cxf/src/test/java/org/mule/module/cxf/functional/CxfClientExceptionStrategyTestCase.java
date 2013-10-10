/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
