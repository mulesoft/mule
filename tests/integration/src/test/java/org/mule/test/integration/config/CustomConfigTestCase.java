/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.config;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.transformer.Transformer;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CustomConfigTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/config/custom-config.xml";
    }

    @Test
    public void testCustomEndpointConfig() throws Exception
    {
        ImmutableEndpoint ep = muleContext.getEndpointFactory().getOutboundEndpoint(
            "fooEndpoint");
        assertNotNull("fooEndpoint should not be null", ep);
        TestFilter tf = (TestFilter)ep.getFilter();
        assertNotNull("the filter on the endpoint should not be null", tf);
        assertEquals(tf.getFoo(), "goo");
        assertEquals(tf.getBar(), 12);
    }

    @Test
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

    @Test
    public void testCustomTransformerConfig() throws Exception
    {
        Transformer trans = muleContext.getRegistry().lookupTransformer("testTransformer");
        assertNotNull("testTransformer should not be null", trans);
        assertTrue("Transformer should be an instance of TestCompressionTransformer", trans instanceof TestCompressionTransformer);
        assertEquals(((TestCompressionTransformer)trans).getBeanProperty1(), "soo");
        assertEquals(((TestCompressionTransformer)trans).getBeanProperty2(), 12345);
    }

}
