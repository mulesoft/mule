/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint;

import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.tck.testmodels.mule.TestInboundTransformer;
import org.mule.tck.testmodels.mule.TestOutboundTransformer;
import org.mule.tck.testmodels.mule.TestResponseTransformer;
import org.mule.transaction.MuleTransactionConfig;
import org.mule.transformer.TransformerUtils;
import org.mule.transport.SingleAttemptConnectionStrategy;
import org.mule.util.ObjectNameHelper;

public class EndpointURIEndpointBuilderTestCase extends AbstractMuleTestCase
{

    public void testBuildInboundEndpoint() throws MuleException
    {
        String uri = "test://address";
        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uri, muleContext);
        try
        {
            ImmutableEndpoint ep = endpointBuilder.buildInboundEndpoint();
            assertTrue(ep.isInbound());
            assertFalse(ep.isOutbound());
            assertTrue(ep.isInbound());
            assertTrue(TransformerUtils.isDefined(ep.getTransformers()));
            assertTrue(ep.getTransformers().get(0) instanceof TestInboundTransformer);
            assertTrue(TransformerUtils.isDefined(ep.getResponseTransformers()));
            assertTrue(ep.getResponseTransformers().get(0) instanceof TestResponseTransformer);
            testDefaultCommonEndpointAttributes(ep);
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testBuildOutboundEndpoint() throws MuleException
    {
        String uri = "test://address";
        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uri, muleContext);
        try
        {
            ImmutableEndpoint ep = endpointBuilder.buildOutboundEndpoint();
            assertTrue(ep.isOutbound());
            assertTrue(ep.isOutbound());
            assertFalse(ep.isInbound());
            assertTrue(TransformerUtils.isDefined(ep.getTransformers()));
            assertTrue(ep.getTransformers().get(0) instanceof TestOutboundTransformer);
            assertTrue(TransformerUtils.isDefined(ep.getResponseTransformers()));
            assertTrue(ep.getResponseTransformers().get(0) instanceof TestResponseTransformer);
            testDefaultCommonEndpointAttributes(ep);
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getStackTrace());
        }
    }

    // TODO DF: Test more than defaults with tests using builder to set non-default values

    public void testDefaultCommonEndpointAttributes(ImmutableEndpoint ep)
    {
        assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
        assertEquals(muleContext.getRegistry().getConfiguration().getDefaultSynchronousEventTimeout(),
            ep.getRemoteSyncTimeout());
        assertEquals(muleContext.getRegistry().getConfiguration().isDefaultSynchronousEndpoints(),
            ep.isSynchronous());
        assertEquals(false, ep.isRemoteSync());
        assertTrue(ep.getConnectionStrategy() instanceof SingleAttemptConnectionStrategy);
        assertTrue(ep.getTransactionConfig() instanceof MuleTransactionConfig);
        assertTrue(ep.getTransactionConfig() instanceof MuleTransactionConfig);
        assertEquals(null, ep.getSecurityFilter());
        assertTrue(ep.getConnector() instanceof TestConnector);
        assertEquals(ObjectNameHelper.getEndpointName(ep), ep.getName());
        assertFalse(ep.isDeleteUnacceptedMessages());
        assertEquals(muleContext.getRegistry().getConfiguration().getDefaultEncoding(), ep.getEncoding());
        assertEquals(null, ep.getFilter());
        assertEquals(ImmutableEndpoint.INITIAL_STATE_STARTED, ep.getInitialState());
    }
    
    public void testHasSetEncodingMethod() throws EndpointException, SecurityException, NoSuchMethodException
    {
        String uri = "test://address";
        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uri, muleContext);
        assertNotNull(endpointBuilder.getClass().getMethod("setEncoding", new Class[]{String.class}));
    }
    
}
