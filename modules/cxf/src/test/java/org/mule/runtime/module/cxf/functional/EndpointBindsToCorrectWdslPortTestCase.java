/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.functional;

import static org.junit.Assert.assertEquals;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.module.cxf.CxfInboundMessageProcessor;
import org.mule.runtime.module.cxf.config.FlowConfiguringMessageProcessor;
import org.mule.tck.junit4.rule.DynamicPort;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.service.model.EndpointInfo;
import org.junit.Rule;
import org.junit.Test;

public class EndpointBindsToCorrectWdslPortTestCase extends FunctionalTestCase
{

    private static final String FLOW_HTTPN = "org/mule/runtime/module/cxf/functional/endpoint-binds-to-correct-wdsl-port-flow-httpn.xml";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return FLOW_HTTPN;
    }

    @Test
    public void testThatTheCorrectSoapPortIsChosen() throws Exception
    {
        FlowConfiguringMessageProcessor wrapper;

        final Flow flow = muleContext.getRegistry().lookupObject("CXFProxyService");
        wrapper = (FlowConfiguringMessageProcessor) flow.getMessageProcessors().get(0);

        CxfInboundMessageProcessor cxfProcessor = (CxfInboundMessageProcessor) wrapper.getWrappedMessageProcessor();
        Server server = cxfProcessor.getServer();
        EndpointInfo endpointInfo = server.getEndpoint().getEndpointInfo();

        assertEquals(
                "The local part of the endpoint name must be the one supplied as the endpointName parameter on the cxf:inbound-endpoint",
                "ListsSoap", endpointInfo.getName().getLocalPart());
    }
}
