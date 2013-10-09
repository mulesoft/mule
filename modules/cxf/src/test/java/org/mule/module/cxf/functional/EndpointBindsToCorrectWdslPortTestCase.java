/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public EndpointBindsToCorrectWdslPortTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/module/cxf/functional/endpoint-binds-to-correct-wdsl-port-service.xml"},
            {ConfigVariant.FLOW, "org/mule/module/cxf/functional/endpoint-binds-to-correct-wdsl-port-flow.xml"}
        });
    }      

    @Test
    public void testThatTheCorrectSoapPortIsChosen() throws Exception
    {
        DefaultInboundEndpoint inboundEndpoint;
        
        if (variant.equals(ConfigVariant.FLOW))
        {
            final Flow flow = muleContext.getRegistry().lookupObject("CXFProxyService");
            inboundEndpoint = (DefaultInboundEndpoint) flow.getMessageSource();                        
        }
        else
        {
            final Service service = muleContext.getRegistry().lookupService("CXFProxyService");
            ServiceCompositeMessageSource messageSource = (ServiceCompositeMessageSource) service.getMessageSource();

            List<InboundEndpoint> endpoints = messageSource.getEndpoints();
            inboundEndpoint = (DefaultInboundEndpoint) endpoints.get(0);            
        }               
        
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
