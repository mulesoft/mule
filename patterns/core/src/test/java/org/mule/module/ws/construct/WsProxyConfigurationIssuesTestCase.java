/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.construct;

import org.mule.MessageExchangePattern;
import org.mule.api.construct.FlowConstructInvalidException;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.net.URI;
import java.util.concurrent.Callable;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WsProxyConfigurationIssuesTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testNullMessageSource()
    {
        runTestFailingWithExpectedFlowConstructInvalidException(new Callable<WSProxy>()
        {
            public WSProxy call() throws Exception
            {
                return new WSProxy("testNullMessageSource", muleContext, null,
                    MuleTestUtils.getTestOutboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE,
                        muleContext));
            }
        });
    }

    @Test
    public void testNullOutboundEndpoint()
    {
        runTestFailingWithExpectedFlowConstructInvalidException(new Callable<WSProxy>()
        {
            public WSProxy call() throws Exception
            {
                return new WSProxy("testNullOutboundEndpoint", muleContext,
                    getTestInboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE), null);
            }
        });
    }

    @Test
    public void testNullOutboundEndpointWithWsdl()
    {
        runTestFailingWithExpectedFlowConstructInvalidException(new Callable<WSProxy>()
        {
            public WSProxy call() throws Exception
            {
                return new WSProxy("testNullOutboundEndpointWithWsdl", muleContext,
                    getTestInboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE), null, "fake_wsdl");
            }
        });
    }

    @Test
    public void testBlankWsdlContents()
    {
        runTestFailingWithExpectedFlowConstructInvalidException(new Callable<WSProxy>()
        {
            public WSProxy call() throws Exception
            {
                return new WSProxy("testBlankWsdlContents", muleContext,
                    getTestInboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE),
                    MuleTestUtils.getTestOutboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE,
                        muleContext), "");
            }
        });
    }

    @Test
    public void testNullWsdlUri()
    {
        runTestFailingWithExpectedFlowConstructInvalidException(new Callable<WSProxy>()
        {
            public WSProxy call() throws Exception
            {
                return new WSProxy("testNullWsdlUrl", muleContext,
                    getTestInboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE),
                    MuleTestUtils.getTestOutboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE,
                        muleContext), (URI) null);
            }
        });
    }

    @Test
    public void testOneWayInboundEndpoint()
    {
        runTestFailingWithExpectedFlowConstructInvalidException(new Callable<WSProxy>()
        {
            public WSProxy call() throws Exception
            {
                return new WSProxy("testOneWayInboundEndpoint", muleContext,
                    getTestInboundEndpoint(MessageExchangePattern.ONE_WAY),
                    MuleTestUtils.getTestOutboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE,
                        muleContext));
            }
        });
    }

    @Test
    public void testOneWayOutboundEndpoint()
    {
        runTestFailingWithExpectedFlowConstructInvalidException(new Callable<WSProxy>()
        {
            public WSProxy call() throws Exception
            {
                return new WSProxy("testOneWayOutboundEndpoint", muleContext,
                    getTestInboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE),
                    MuleTestUtils.getTestOutboundEndpoint(MessageExchangePattern.ONE_WAY, muleContext));
            }
        });
    }

    private void runTestFailingWithExpectedFlowConstructInvalidException(Callable<WSProxy> failingStatement)
    {
        try
        {
            failingStatement.call().validateConstruct();
            fail("should have got a FlowConstructInvalidException");
        }
        catch (final Exception e)
        {
            assertTrue(e instanceof FlowConstructInvalidException);
        }
    }
}
