/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.functional;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.service.Service;
import org.mule.endpoint.DefaultInboundEndpoint;
import org.mule.module.cxf.CxfInboundMessageProcessor;
import org.mule.module.cxf.config.FlowConfiguringMessageProcessor;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.List;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.service.model.EndpointInfo;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EndpointBindsToCorrectWdslPortTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "org/mule/module/cxf/functional/endpoint-binds-to-correct-wdsl-port.xml";
    }

    @Test
    public void testThatTheCorrectSoapPortIsChosen() throws Exception
    {
        final Service service = muleContext.getRegistry().lookupService("CXFProxyService");
        ServiceCompositeMessageSource messageSource = (ServiceCompositeMessageSource) service.getMessageSource();

        List<InboundEndpoint> endpoints = messageSource.getEndpoints();
        DefaultInboundEndpoint inboundEndpoint = (DefaultInboundEndpoint) endpoints.get(0);
        List<MessageProcessor> processors = inboundEndpoint.getMessageProcessors();
        FlowConfiguringMessageProcessor wrapper = (FlowConfiguringMessageProcessor) processors.get(0);
        CxfInboundMessageProcessor cxfProcessor = (CxfInboundMessageProcessor) wrapper.getWrappedMessageProcessor();
        Server server = cxfProcessor.getServer();
        EndpointInfo endpointInfo = server.getEndpoint().getEndpointInfo();

        assertEquals(
            "The local part of the endpoing name must be the one supplied as the endpointName parameter on the cxf:inbound-endpoint",
            "ListsSoap", endpointInfo.getName().getLocalPart());
    }

}
