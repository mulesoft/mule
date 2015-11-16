/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transformer.TransformerException;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.transformer.AbstractMessageTransformer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;

import org.junit.Test;

public class EventMetaDataPropagationTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/event-metadata-propagation-config-flow.xml";
    }

    @Test
    public void testEventMetaDataPropagation() throws MuleException
    {
        Flow flow = muleContext.getRegistry().lookupObject("component1");
        MuleEvent event = new DefaultMuleEvent(new DefaultMuleMessage("Test MuleEvent", muleContext),
                                               ((InboundEndpoint) flow.getMessageSource()), flow);
        flow.process(event);
    }

    public static class DummyComponent implements Callable
    {
        @Override
        public Object onCall(MuleEventContext context) throws Exception
        {
            if ("component1".equals(context.getFlowConstruct().getName()))
            {
                Map<String, Object> props = new HashMap<String, Object>();
                props.put("stringParam", "param1");
                props.put("objectParam", new Apple());
                props.put("doubleParam", 12345.6);
                props.put("integerParam", 12345);
                props.put("longParam", (long) 123456789);
                props.put("booleanParam", Boolean.TRUE);
                MuleMessage msg = new DefaultMuleMessage(context.getMessageAsString(), props, muleContext);
                msg.addOutboundAttachment("test1", new DataHandler(new DataSource()
                {
                    @Override
                    public InputStream getInputStream() throws IOException
                    {
                        return null;
                    }

                    @Override
                    public OutputStream getOutputStream() throws IOException
                    {
                        return null;
                    }

                    @Override
                    public String getContentType()
                    {
                        return "text/plain";
                    }

                    @Override
                    public String getName()
                    {
                        return "test1";
                    }
                }));
                return msg;
            }
            else
            {
                MuleMessage msg = context.getMessage();
                assertEquals("param1", msg.getInboundProperty("stringParam"));
                final Object o = msg.getInboundProperty("objectParam");
                assertTrue(o instanceof Apple);
                assertEquals(12345.6, 12345.6, msg.<Double> getInboundProperty("doubleParam", 0d));
                assertEquals(12345, msg.<Integer> getInboundProperty("integerParam", 0).intValue());
                assertEquals(123456789, msg.<Long> getInboundProperty("longParam", 0L).longValue());
                assertEquals(Boolean.TRUE, msg.getInboundProperty("booleanParam", Boolean.FALSE));
                assertNotNull(msg.getInboundAttachment("test1"));
            }
            return null;
        }
    }

    /**
     * Extend AbstractMessageAwareTransformer, even though it's deprecated, to ensure
     * that it keeps working for compatibility with older user-written transformers.
     */
    public static class DummyTransformer extends AbstractMessageTransformer
    {

        @Override
        public Object transformMessage(MuleMessage msg, String outputEncoding) throws TransformerException
        {
            assertEquals("param1", msg.getOutboundProperty("stringParam"));
            final Object o = msg.getOutboundProperty("objectParam");
            assertTrue(o instanceof Apple);
            assertEquals(12345.6, 12345.6, msg.<Double> getOutboundProperty("doubleParam", 0d));
            assertEquals(12345, msg.<Integer> getOutboundProperty("integerParam", 0).intValue());
            assertEquals(123456789, msg.<Long> getOutboundProperty("longParam", 0L).longValue());
            assertEquals(Boolean.TRUE, msg.getOutboundProperty("booleanParam", Boolean.FALSE));
            return msg;
        }
    }
}
