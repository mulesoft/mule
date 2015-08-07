/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transport.cxf;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.processor.MessageProcessor;
import org.mule.endpoint.AbstractEndpointBuilder;
import org.mule.module.cxf.CxfOutboundMessageProcessor;
import org.mule.module.cxf.config.FlowConfiguringMessageProcessor;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.List;
import java.util.Map;

import org.apache.cxf.endpoint.Client;
import org.junit.Test;

@Ignore("BL-38 Need to port for CXF changes")
public class CxfClientPassivateTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/transport/cxf/cxf-memoryleak-config.xml";
    }

    /**
     * Test that CxfMessageDispatcher's passivate method cleans up the CXF
     * client request and response contexts. This is needed in order to release
     * the memory occupied by that objects, which could be a lot of wasted
     * memory because of the number of CxfMessageDispatchers that live in the
     * dispatcher pool. See MULE-4899 for more details.
     */
    @Test
    public void testPassivateCleansClientRequestAndResponseContext() throws Exception
    {
        MuleClient muleClient = muleContext.getClient();

        // Sends data to process
        muleClient.send("vm://in", TEST_MESSAGE, null);

        // Waits for a response
        MuleMessage message = muleClient.request("vm://out", 5000);
        assertNotNull(message);

        CxfOutboundMessageProcessor processor = getOutboundMessageProcessor();

        Client client = processor.getClient();

        final Map<String, Object> requestContext = client.getRequestContext();
        assertTrue("Request context should be empty", requestContext.isEmpty());

        final Map<String, Object> responseContext = client.getResponseContext();
        assertTrue("Response context should be empty", responseContext.isEmpty());
    }

    private CxfOutboundMessageProcessor getOutboundMessageProcessor()
    {
        AbstractEndpointBuilder epbuilder = (AbstractEndpointBuilder) muleContext.getRegistry().lookupEndpointBuilder("clientEndpoint");

        List<MessageProcessor> mps = epbuilder.getMessageProcessors();
        return (CxfOutboundMessageProcessor) ((FlowConfiguringMessageProcessor)mps.get(0)).getWrappedMessageProcessor();
    }
}
