/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.functional;

import static org.junit.Assert.assertEquals;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.service.Service;
import org.mule.construct.Flow;
import org.mule.endpoint.DefaultInboundEndpoint;
import org.mule.module.cxf.CxfInboundMessageProcessor;
import org.mule.module.cxf.config.FlowConfiguringMessageProcessor;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.service.model.EndpointInfo;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class EndpointBindsToCorrectWdslPortTestCase extends AbstractServiceAndFlowTestCase
{

    private static final String FLOW_HTTPN = "org/mule/module/cxf/functional/endpoint-binds-to-correct-wdsl-port-flow-httpn.xml";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public EndpointBindsToCorrectWdslPortTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ConfigVariant.SERVICE, "org/mule/module/cxf/functional/endpoint-binds-to-correct-wdsl-port-service.xml"},
                {ConfigVariant.FLOW, "org/mule/module/cxf/functional/endpoint-binds-to-correct-wdsl-port-flow.xml"},
                {ConfigVariant.FLOW, FLOW_HTTPN}
        });
    }

    @Test
    public void testThatTheCorrectSoapPortIsChosen() throws Exception
    {
        FlowConfiguringMessageProcessor wrapper;

        if (variant.equals(ConfigVariant.FLOW))
        {
            final Flow flow = muleContext.getRegistry().lookupObject("CXFProxyService");
            if (FLOW_HTTPN.equals(configResources))
            {
                wrapper = (FlowConfiguringMessageProcessor) flow.getMessageProcessors().get(0);
            }
            else
            {
                DefaultInboundEndpoint inboundEndpoint = (DefaultInboundEndpoint) flow.getMessageSource();
                List<MessageProcessor> processors = inboundEndpoint.getMessageProcessors();
                wrapper = (FlowConfiguringMessageProcessor) processors.get(0);
            }
        }
        else
        {
            final Service service = muleContext.getRegistry().lookupService("CXFProxyService");
            ServiceCompositeMessageSource messageSource = (ServiceCompositeMessageSource) service.getMessageSource();

            List<InboundEndpoint> endpoints = messageSource.getEndpoints();
            DefaultInboundEndpoint inboundEndpoint = (DefaultInboundEndpoint) endpoints.get(0);
            List<MessageProcessor> processors = inboundEndpoint.getMessageProcessors();
            wrapper = (FlowConfiguringMessageProcessor) processors.get(0);
        }

        CxfInboundMessageProcessor cxfProcessor = (CxfInboundMessageProcessor) wrapper.getWrappedMessageProcessor();
        Server server = cxfProcessor.getServer();
        EndpointInfo endpointInfo = server.getEndpoint().getEndpointInfo();

        assertEquals(
                "The local part of the endpoing name must be the one supplied as the endpointName parameter on the cxf:inbound-endpoint",
                "ListsSoap", endpointInfo.getName().getLocalPart());
    }

}
