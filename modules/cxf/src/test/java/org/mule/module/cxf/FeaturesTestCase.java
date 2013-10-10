/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.cxf.builder.WebServiceMessageProcessorBuilder;
import org.mule.module.cxf.config.FlowConfiguringMessageProcessor;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.List;

import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.feature.LoggingFeature;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FeaturesTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
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
