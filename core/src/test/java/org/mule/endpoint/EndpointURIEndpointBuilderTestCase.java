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
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.retry.policies.NoRetryPolicyTemplate;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.tck.testmodels.mule.TestInboundTransformer;
import org.mule.tck.testmodels.mule.TestOutboundTransformer;
import org.mule.tck.testmodels.mule.TestResponseTransformer;
import org.mule.transaction.MuleTransactionConfig;
import org.mule.util.ObjectNameHelper;

public class EndpointURIEndpointBuilderTestCase extends AbstractMuleTestCase
{
    public void testBuildInboundEndpoint() throws Exception
    {
        String uri = "test://address";
        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uri, muleContext);
        ImmutableEndpoint ep = endpointBuilder.buildInboundEndpoint();
        assertTrue(ep instanceof InboundEndpoint);
        assertFalse(ep instanceof OutboundEndpoint);
        assertNotNull(ep.getTransformers());
        assertEquals(1, ep.getTransformers().size());
        assertTrue(ep.getTransformers().get(0) instanceof TestInboundTransformer);
        assertNotNull(ep.getResponseTransformers());
        assertEquals(1, ep.getResponseTransformers().size());
        assertTrue(ep.getResponseTransformers().get(0) instanceof TestResponseTransformer);
        testDefaultCommonEndpointAttributes(ep);
    }

    public void testBuildOutboundEndpoint() throws MuleException
    {
        String uri = "test://address";
        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uri, muleContext);
        try
        {
            ImmutableEndpoint ep = endpointBuilder.buildOutboundEndpoint();
            assertFalse(ep instanceof InboundEndpoint);
            assertTrue(ep instanceof OutboundEndpoint);
            assertTrue(ep.getTransformers() != null);
            assertTrue(ep.getTransformers().get(0) instanceof TestOutboundTransformer);
            assertTrue(ep.getResponseTransformers().isEmpty());
            testDefaultCommonEndpointAttributes(ep);
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getStackTrace());
        }
    }

    // TODO DF: Test more than defaults with tests using builder to set non-default
    // values

    protected void testDefaultCommonEndpointAttributes(ImmutableEndpoint ep)
    {
        assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
        assertEquals(muleContext.getConfiguration().getDefaultSynchronousEventTimeout(), ep.getRemoteSyncTimeout());
        assertEquals(muleContext.getConfiguration().isDefaultSynchronousEndpoints()
                     || muleContext.getConfiguration().isDefaultRemoteSync(), ep.isSynchronous());
        assertEquals(muleContext.getConfiguration().isDefaultRemoteSync(), ep.isRemoteSync());
        assertTrue("ep.getRetryPolicyTemplate() = " + ep.getRetryPolicyTemplate().getClass(), ep.getRetryPolicyTemplate() instanceof NoRetryPolicyTemplate);
        assertTrue(ep.getTransactionConfig() instanceof MuleTransactionConfig);
        assertTrue(ep.getTransactionConfig() instanceof MuleTransactionConfig);
        assertEquals(null, ep.getSecurityFilter());
        assertTrue(ep.getConnector() instanceof TestConnector);
        assertEquals(ObjectNameHelper.getEndpointName(ep.getEndpointURI()), ep.getName());
        assertFalse(ep.isDeleteUnacceptedMessages());
        assertEquals(muleContext.getConfiguration().getDefaultEncoding(), ep.getEncoding());
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
