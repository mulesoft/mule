/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms.integration;

import static org.mule.functional.functional.FlowAssert.verify;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;

import java.nio.charset.Charset;

import org.junit.Test;

/**
 * Tests the correct propagation of the correlation id property within the JMS transport. This test is related to MULE-6577.
 */
public class JmsCorrelationIdPropagationTestCase extends AbstractJmsFunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "integration/jms-correlation-id-propagation.xml";
    }

    @Test
    public void testMuleCorrelationIdPropagation() throws Exception
    {
        runFlow("withMuleCorrelationId");
        verifyPropagation();
    }

    @Test
    public void testCustomCorrelationIdPropagation() throws Exception
    {
        runFlow("withCustomCorrelationId");
        verifyPropagation();
    }

    @Test
    public void testNoCorrelationIdPropagation() throws Exception
    {
        flowRunner("withNoCorrelationId").withPayload(MuleMessage.builder().payload(TEST_PAYLOAD).id("custom-cid").build()).run();
        verifyPropagation();
    }

    protected void verifyPropagation() throws Exception
    {
        verify("withCorrelationIdBridge");
        verify("withCorrelationIdOut");
    }

    public static class SetCorrelationIdTransformer extends AbstractMessageTransformer
    {

        @Override
        public Object transformMessage(MuleEvent event, Charset outputEncoding) throws TransformerException
        {
            final MuleMessage message = MuleMessage.builder(event.getMessage())
                                                   .correlationId(getCid())
                                                   .build();
            event.setMessage(message);
            return message;
        }

        protected String getCid()
        {
            return "custom-cid";
        }
    }

    public static class SetCorrelationId2Transformer extends SetCorrelationIdTransformer
    {
        @Override
        protected String getCid()
        {
            return "custom-cid-2";
        }
    }

    public static class SetCorrelationId3Transformer extends SetCorrelationIdTransformer
    {
        @Override
        protected String getCid()
        {
            return "custom-cid-3";
        }
    }

}
