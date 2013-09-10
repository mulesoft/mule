/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.config.ConfigurationException;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.construct.Flow;
import org.mule.module.ognl.filters.OGNLFilter;
import org.mule.routing.MessageFilter;
import org.mule.routing.outbound.FilteringOutboundRouter;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class OGNLFilterTestCase extends AbstractServiceAndFlowTestCase
{
    public static final String DEFAULT_INPUT_QUEUE = "vm://in";
    public static final String DEFUALT_OUTPUT_QUEUE = "vm://out";
    public static final String FIRST_MESSAGE = "foo";
    public static final String SECOND_MESSAGE = "foobar";
    public static final String THIRD_MESSAGE = "INPUT MESSAGE";
    public static final long TIMEOUT = 5000;
    public static final String OGNL_EXSPRESSION = " equals(\"foo\") || content.endsWith(\"bar\") ";
    public static final String SERVICE_NAME = "OGNLServiceWrapper1";

    private OGNLFilter filter;

    public OGNLFilterTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "ognl-functional-test-service.xml"},
            {ConfigVariant.FLOW, "ognl-functional-test-flow.xml"}});
    }

    @Override
    protected void doSetUp() throws Exception
    {
        filter = new OGNLFilter();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        filter = null;
    }

    @Test
    public void testNewFilter()
    {
        assertFalse(filter.accept(null));
    }

    @Test
    public void testNoExpressionEmptyMessage()
    {
        MuleMessage message = new DefaultMuleMessage(null, muleContext);
        assertFalse(filter.accept(message));
    }

    @Test
    public void testNoExpressionValidMessage()
    {
        MuleMessage message = new DefaultMuleMessage("foo", muleContext);
        assertFalse(filter.accept(message));
    }

    @Test
    public void testNamespaceHandler()
    {
        String expression;

        if (variant.equals(ConfigVariant.SERVICE))
        {
            expression = ((OGNLFilter) ((FilteringOutboundRouter) ((OutboundRouterCollection) muleContext.getRegistry()
                .lookupService(SERVICE_NAME)
                .getOutboundMessageProcessor()).getRoutes().get(0)).getFilter()).getExpression();
        }
        else
        {
            expression = ((OGNLFilter) ((MessageFilter) ((Flow) muleContext.getRegistry()
                .lookupFlowConstruct(SERVICE_NAME)).getMessageProcessors().get(0)).getFilter()).getExpression();
        }

        assertEquals(expression, OGNL_EXSPRESSION);
    }

    @Test
    public void testFunctionalTest() throws Exception
    {
        MuleClient client = muleContext.getClient();

        client.dispatch(DEFAULT_INPUT_QUEUE, FIRST_MESSAGE, null);
        MuleMessage message = client.request(DEFUALT_OUTPUT_QUEUE, TIMEOUT);
        assertNotNull(message);
        assertNotNull(message.getPayload());
        assertNull(message.getExceptionPayload());
        assertEquals(FIRST_MESSAGE, message.getPayload());

        Dummy payload = new Dummy();
        payload.setContent(SECOND_MESSAGE);
        client.dispatch(DEFAULT_INPUT_QUEUE, new DefaultMuleMessage(payload, muleContext));
        message = client.request(DEFUALT_OUTPUT_QUEUE, TIMEOUT);
        assertNotNull(message);
        assertNotNull(message.getPayload());
        assertNull(message.getExceptionPayload());
        assertEquals(SECOND_MESSAGE, ((Dummy)message.getPayload()).getContent());

        client.dispatch(DEFAULT_INPUT_QUEUE, THIRD_MESSAGE, null);
        message = client.request(DEFUALT_OUTPUT_QUEUE, TIMEOUT);
        assertNull(message);
    }

    @Test
    public void testFunctionalTestUsingExpressionFilter() throws Exception
    {
        MuleClient client = muleContext.getClient();

        client.dispatch("vm://in2", FIRST_MESSAGE, null);
        MuleMessage message = client.request("vm://out2", TIMEOUT);
        assertNotNull(message);
        assertNotNull(message.getPayload());
        assertNull(message.getExceptionPayload());
        assertEquals(FIRST_MESSAGE, message.getPayload());

        Dummy payload = new Dummy();
        payload.setContent(SECOND_MESSAGE);
        client.dispatch("vm://in2", new DefaultMuleMessage(payload, muleContext));
        message = client.request("vm://out2", TIMEOUT);
        assertNotNull(message);
        assertNotNull(message.getPayload());
        assertNull(message.getExceptionPayload());
        assertEquals(SECOND_MESSAGE, ((Dummy)message.getPayload()).getContent());

        client.dispatch("vm://in2", THIRD_MESSAGE, null);
        message = client.request("vm://out2", TIMEOUT);
        assertNull(message);
    }

    @Test
    public void testInvalidObjectExpression()
    {
        try
        {
            filter.setExpression("foo:bar");
            fail("should have failed with ConfigurationException");
        }
        catch (ConfigurationException configex)
        {
            // expected
        }

        // make sure the filter is still unconfigured
        assertNull(filter.getExpression());
    }

    // a simple POJO for testing object expressions
    private static class Dummy
    {
        private int id;
        private String content;

        public Dummy()
        {
            super();
        }

        /**
         * @return Returns the content.
         */
        public String getContent()
        {
            return content;
        }

        /**
         * @param content The content to set.
         */
        public void setContent(String content)
        {
            this.content = content;
        }

        /**
         * @return Returns the id.
         */
        public int getId()
        {
            return id;
        }

        /**
         * @param id The id to set.
         */
        public void setId(int id)
        {
            this.id = id;
        }
    }
}
