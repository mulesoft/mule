/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.filters;

import org.mule.config.ConfigurationException;
import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.routing.outbound.FilteringOutboundRouter;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class OGNLFilterTestCase extends FunctionalTestCase
{
    public static final String DEFAULT_INPUT_QUEUE = "vm://in";
    public static final String DEFUALT_OUTPUT_QUEUE = "vm://out";
    public static final String FIRST_MESSAGE = "foo";
    public static final String SECOND_MESSAGE = "foobar";
    public static final String THIRD_MESSAGE = "INPUT MESSAGE";
    public static final long TIMEOUT = 2000;
    public static final String OGNL_EXSPRESSION = " equals(\"foo\") || content.endsWith(\"bar\") ";
    public static final String SERVICE_NAME = "OGNLServiceWrapper1";

    protected String getConfigResources()
    {
        return "ognl-functional-test.xml";
    }

    private OGNLFilter filter;

    // @Override
    protected void doSetUp() throws Exception
    {
        filter = new OGNLFilter();
    }

    // @Override
    protected void doTearDown() throws Exception
    {
        filter = null;
    }

    public void testNewFilter()
    {
        assertFalse(filter.accept(null));
    }

    public void testNoExpressionEmptyMessage()
    {
        UMOMessage message = new MuleMessage(null);
        assertFalse(filter.accept(message));
    }

    public void testNoExpressionValidMessage()
    {
        UMOMessage message = new MuleMessage("foo");
        assertFalse(filter.accept(message));
    }

    public void testNamespaceHandler()
    {
        String expression =
                ((OGNLFilter) ((FilteringOutboundRouter) managementContext.getRegistry().
                        lookupComponent(SERVICE_NAME).getOutboundRouter().getRouters().get(0)).
                        getFilter()).getExpression();

        assertEquals(expression, OGNL_EXSPRESSION);
    }

    public void testFunctionalTest() throws Exception
    {
        MuleClient client = new MuleClient();
        try
        {
            client.dispatch(DEFAULT_INPUT_QUEUE, FIRST_MESSAGE, null);
            UMOMessage message = client.receive(DEFUALT_OUTPUT_QUEUE, TIMEOUT);
            assertNotNull(message);
            assertNotNull(message.getPayload());
            assertNull(message.getExceptionPayload());
            assertEquals(FIRST_MESSAGE, message.getPayload());

            Dummy payload = new Dummy();
            payload.setContent(SECOND_MESSAGE);
            client.dispatch(DEFAULT_INPUT_QUEUE, new MuleMessage(payload));
            message = client.receive(DEFUALT_OUTPUT_QUEUE, TIMEOUT);
            assertNotNull(message);
            assertNotNull(message.getPayload());
            assertNull(message.getExceptionPayload());
            assertEquals(SECOND_MESSAGE, ((Dummy) message.getPayload()).getContent());

            client.dispatch(DEFAULT_INPUT_QUEUE, THIRD_MESSAGE, null);
            message = client.receive(DEFUALT_OUTPUT_QUEUE, TIMEOUT);
            assertNull(message);
        }
        finally
        {
            client.dispose();
        }

    }


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
