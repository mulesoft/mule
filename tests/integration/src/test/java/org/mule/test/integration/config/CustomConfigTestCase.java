/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.config;

import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.transformer.UMOTransformer;

public class CustomConfigTestCase extends FunctionalTestCase
{
    public CustomConfigTestCase()
    {
        this.setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "org/mule/test/integration/config/custom-config.xml";
    }

    public void testCustomEndpointConfig() throws Exception
    {
        UMOImmutableEndpoint ep = managementContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(
            "fooEndpoint");
        assertNotNull("fooEndpoint should not be null", ep);
        TestFilter tf = (TestFilter)ep.getFilter();
        assertNotNull("the filter on the endpoint should not be null", tf);
        assertEquals(tf.getFoo(), "goo");
        assertEquals(tf.getBar(), 12);
    }

    public void testCustomConnectorConfig() throws Exception
    {
        TestConnector cnn = (TestConnector)managementContext.getRegistry().lookupConnector("customConnector");
        assertNotNull("customConnector should not be null", cnn);
        assertEquals(cnn.getSomeProperty(), "foo");

        //Test exception strategy
        assertNotNull(cnn.getExceptionListener());
        assertTrue(cnn.getExceptionListener() instanceof TestExceptionStrategy);
        assertEquals("bar", ((TestExceptionStrategy)cnn.getExceptionListener()).getTestProperty());
    }

    public void testCustomTransformerConfig() throws Exception
    {
        UMOTransformer trans = managementContext.getRegistry().lookupTransformer("testTransformer");
        assertNotNull("testTransformer should not be null", trans);
        assertTrue("Transformer should be an instance of TestCompressionTransformer", trans instanceof TestCompressionTransformer);
        assertEquals(((TestCompressionTransformer)trans).getBeanProperty1(), "soo");
        assertEquals(((TestCompressionTransformer)trans).getBeanProperty2(), 12345);
    }

}
