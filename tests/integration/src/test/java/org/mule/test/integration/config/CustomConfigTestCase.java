/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.tck.testmodels.mule.TestConnector;

import org.junit.Test;

public class CustomConfigTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
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
