/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.cxf.builder.WebServiceMessageProcessorBuilder;
import org.mule.module.cxf.config.FlowConfiguringMessageProcessor;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.List;

import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.feature.LoggingFeature;
import org.junit.Test;

public class FeaturesTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "features-test.xml";
    }

    @Test
    public void testFeatures() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupEndpointBuilder("endpoint").buildInboundEndpoint();
        assertNotNull(endpoint);
        
        List<MessageProcessor> mps = endpoint.getMessageProcessors();
        assertTrue(mps.get(0) instanceof FlowConfiguringMessageProcessor);
        
        FlowConfiguringMessageProcessor mp = (FlowConfiguringMessageProcessor) mps.get(0);
        WebServiceMessageProcessorBuilder builder = (WebServiceMessageProcessorBuilder) mp.getMessageProcessorBuilder();
        
        List<AbstractFeature> features = builder.getFeatures();
        assertNotNull(features);
        boolean found = false;
        for (AbstractFeature f : features)
        {
            if (f instanceof LoggingFeature)
            {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

}
