/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.endpoint;

import org.mule.impl.MuleTransactionConfig;
import org.mule.providers.SingleAttemptConnectionStrategy;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.tck.testmodels.mule.TestInboundTransformer;
import org.mule.tck.testmodels.mule.TestOutboundTransformer;
import org.mule.tck.testmodels.mule.TestResponseTransformer;
import org.mule.transformers.TransformerUtils;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOEndpointBuilder;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.util.ObjectNameHelper;

public class EndpointURIEndpointBuilderTestCase extends AbstractMuleTestCase
{

    public void testBuildInboundEndpoint() throws UMOException
    {
        String uri = "test://address";
        UMOEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uri, managementContext);
        try
        {
            UMOImmutableEndpoint ep = endpointBuilder.buildInboundEndpoint();
            assertEquals(UMOImmutableEndpoint.ENDPOINT_TYPE_RECEIVER, ep.getType());
            assertFalse(ep.canSend());
            assertTrue(ep.canReceive());
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

    public void testBuildOutboundEndpoint() throws UMOException
    {
        String uri = "test://address";
        UMOEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uri, managementContext);
        try
        {
            UMOImmutableEndpoint ep = endpointBuilder.buildOutboundEndpoint();
            assertEquals(UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER, ep.getType());
            assertTrue(ep.canSend());
            assertFalse(ep.canReceive());
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

    public void testBuildResponseEndpoint() throws UMOException
    {
        String uri = "test://address";
        UMOEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uri, managementContext);
        try
        {
            UMOImmutableEndpoint ep = endpointBuilder.buildResponseEndpoint();
            assertEquals(UMOImmutableEndpoint.ENDPOINT_TYPE_RESPONSE, ep.getType());
            assertFalse(ep.canSend());
            assertFalse(ep.canReceive());
            assertTrue(TransformerUtils.isDefined(ep.getTransformers()));
            assertTrue(ep.getTransformers().get(0) instanceof TestInboundTransformer);
            assertFalse(TransformerUtils.isDefined(ep.getResponseTransformers()));
            testDefaultCommonEndpointAttributes(ep);
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getStackTrace());
        }
    }

    public void testDefaultCommonEndpointAttributes(UMOImmutableEndpoint ep)
    {
        assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
        assertEquals(managementContext.getRegistry().getConfiguration().getDefaultSynchronousEventTimeout(),
            ep.getRemoteSyncTimeout());
        assertEquals(managementContext.getRegistry().getConfiguration().isDefaultSynchronousEndpoints(),
            ep.isSynchronous());
        assertEquals(false, ep.isRemoteSync());
        assertTrue(ep.getConnectionStrategy() instanceof SingleAttemptConnectionStrategy);
        assertTrue(ep.getTransactionConfig() instanceof MuleTransactionConfig);
        assertTrue(ep.getTransactionConfig() instanceof MuleTransactionConfig);
        assertEquals(null, ep.getSecurityFilter());
        assertTrue(ep.getConnector() instanceof TestConnector);
        assertEquals(ObjectNameHelper.getEndpointName(ep), ep.getName());
        assertFalse(ep.isDeleteUnacceptedMessages());
        assertEquals(managementContext.getRegistry().getConfiguration().getDefaultEncoding(), ep.getEncoding());
        assertEquals(null, ep.getFilter());
        assertEquals(UMOImmutableEndpoint.INITIAL_STATE_STARTED, ep.getInitialState());
    }
    
    public void testHasSetEncodingMethod() throws EndpointException, SecurityException, NoSuchMethodException
    {
        String uri = "test://address";
        UMOEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uri, managementContext);
        assertNotNull(endpointBuilder.getClass().getMethod("setEncoding", new Class[]{String.class}));
    }
    
}
