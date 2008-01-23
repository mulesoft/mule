/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file;

import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.tck.FunctionalTestCase;
import org.mule.transformer.TransformerUtils;
import org.mule.transformer.simple.ByteArrayToSerializable;
import org.mule.transformer.simple.SerializableToByteArray;
import org.mule.transport.AbstractConnector;
import org.mule.transport.file.FileConnector;

public class ConnectorServiceOverridesTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "test-connector-config.xml";
    }

    public void testServiceOverrides() throws InterruptedException
    {
        FileConnector c = (FileConnector) muleContext.getRegistry().lookupConnector("fileConnector2");
        assertNotNull(c);
        assertNotNull(c.getServiceOverrides());
        assertEquals("org.mule.transformer.simple.ByteArrayToSerializable", c.getServiceOverrides().get(
            "inbound.transformer"));
        assertNotNull(TransformerUtils.firstOrNull(c.getDefaultInboundTransformers()));
        assertNotNull(TransformerUtils.firstOrNull(c.getDefaultOutboundTransformers()));
        assertTrue(TransformerUtils.firstOrNull(c.getDefaultInboundTransformers()) instanceof ByteArrayToSerializable);
        assertTrue(TransformerUtils.firstOrNull(c.getDefaultOutboundTransformers()) instanceof SerializableToByteArray);
    }

    public void testServiceOverrides2() throws InterruptedException
    {
        FileConnector c = (FileConnector) muleContext.getRegistry().lookupConnector("fileConnector1");
        assertNotNull(c);
        assertNull(c.getServiceOverrides());

        c = (FileConnector) muleContext.getRegistry().lookupConnector("fileConnector2");
        assertNotNull(c);
        assertNotNull(c.getServiceOverrides());

        c = (FileConnector) muleContext.getRegistry().lookupConnector("fileConnector3");
        assertNotNull(c);
        assertNull(c.getServiceOverrides());
    }

    public void testServiceOverrides3() throws InterruptedException, MuleException
    {
        // EndpointURI uri = new MuleEndpointURI("file:///temp?connector=fileConnector1");
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "file:///temp?connector=fileConnector1");

        assertNotNull(endpoint);
        assertNotNull(endpoint.getConnector());
        assertNull(((AbstractConnector) endpoint.getConnector()).getServiceOverrides());

        FileConnector c = (FileConnector) muleContext.getRegistry().lookupConnector("fileConnector2");
        assertNotNull(c);
        assertNotNull(c.getServiceOverrides());

        EndpointBuilder builder = new EndpointURIEndpointBuilder("file:///temp?connector=fileConnector1",
            muleContext);
        builder.setConnector(c);
        endpoint = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(builder);
        assertNotNull(((AbstractConnector) endpoint.getConnector()).getServiceOverrides());

        EndpointBuilder builder2 = new EndpointURIEndpointBuilder("file:///temp?connector=fileConnector3",
            muleContext);
        builder.setConnector(c);
        endpoint = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(builder2);
        assertNull(((AbstractConnector) endpoint.getConnector()).getServiceOverrides());

        EndpointBuilder builder3 = new EndpointURIEndpointBuilder("file:///temp?connector=fileConnector2",
            muleContext);
        builder.setConnector(c);
        endpoint = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(builder3);
        assertNotNull(((AbstractConnector) endpoint.getConnector()).getServiceOverrides());

    }
}
