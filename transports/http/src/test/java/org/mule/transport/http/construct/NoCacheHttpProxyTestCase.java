/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.construct;

import static org.junit.Assert.assertEquals;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.Connector;
import org.mule.construct.AbstractFlowConstruct;
import org.mule.construct.AbstractFlowConstuctTestCase;
import org.mule.tck.MuleTestUtils;

import java.util.Collections;

public class NoCacheHttpProxyTestCase extends AbstractFlowConstuctTestCase
{
    protected Connector testConnector;
    private HttpProxy httpProxy;

    @SuppressWarnings("unchecked")
    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        final OutboundEndpoint testOutboundEndpoint = MuleTestUtils.getTestOutboundEndpoint(
            MessageExchangePattern.REQUEST_RESPONSE, muleContext);
        testConnector = testOutboundEndpoint.getConnector();
        muleContext.getRegistry().registerConnector(testConnector);
        testConnector.start();

        httpProxy = new HttpProxy("no-cache-http-proxy", muleContext, directInboundMessageSource,
            testOutboundEndpoint, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null);
    }

    @Override
    protected AbstractFlowConstruct getFlowConstruct() throws Exception
    {
        return httpProxy;
    }

    private void startHttpProxy() throws InitialisationException, MuleException
    {
        httpProxy.initialise();
        httpProxy.start();
    }

    public void testProcess() throws Exception
    {
        startHttpProxy();

        final MuleEvent response = directInboundMessageSource.process(MuleTestUtils.getTestEvent(
            "hello", muleContext));

        assertEquals("hello", response.getMessageAsString());
    }
}
