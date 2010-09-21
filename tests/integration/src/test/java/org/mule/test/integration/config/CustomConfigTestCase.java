/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.config;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.transformer.Transformer;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;

public class CustomConfigTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integration/config/custom-config.xml";
    }

    public void testCustomEndpointConfig() throws Exception
    {
        ImmutableEndpoint ep = muleContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(
            "fooEndpoint");
        assertNotNull("fooEndpoint should not be null", ep);
        TestFilter tf = (TestFilter)ep.getFilter();
        assertNotNull("the filter on the endpoint should not be null", tf);
        assertEquals(tf.getFoo(), "goo");
        assertEquals(tf.getBar(), 12);
    }

    public void testCustomConnectorConfig() throws Exception
    {
        TestConnector cnn = (TestConnector)muleContext.getRegistry().lookupConnector("customConnector");
        assertNotNull("customConnector should not be null", cnn);
        assertEquals(cnn.getSomeProperty(), "foo");

        //Test exception strategy
        MessagingExceptionHandler es = muleContext.getRegistry().lookupModel("main").getExceptionListener();
        assertNotNull(es);
        assertTrue(es instanceof TestExceptionStrategy);
        assertEquals("bar", ((TestExceptionStrategy) es).getTestProperty());
    }

    public void testCustomTransformerConfig() throws Exception
    {
        Transformer trans = muleContext.getRegistry().lookupTransformer("testTransformer");
        assertNotNull("testTransformer should not be null", trans);
        assertTrue("Transformer should be an instance of TestCompressionTransformer", trans instanceof TestCompressionTransformer);
        assertEquals(((TestCompressionTransformer)trans).getBeanProperty1(), "soo");
        assertEquals(((TestCompressionTransformer)trans).getBeanProperty2(), 12345);
    }

}
