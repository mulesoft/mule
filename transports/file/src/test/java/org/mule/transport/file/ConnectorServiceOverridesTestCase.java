/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transformer.TransformerUtils;
import org.mule.transformer.simple.ByteArrayToSerializable;
import org.mule.transformer.simple.SerializableToByteArray;
import org.mule.transport.AbstractConnector;

import org.junit.Test;

public class ConnectorServiceOverridesTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "test-connector-config.xml";
    }

    @Test
    public void testServiceOverrides() throws InterruptedException
    {
        FileConnector c = (FileConnector) muleContext.getRegistry().lookupConnector("fileConnector2");
        assertNotNull(c);
        assertNotNull(c.getServiceOverrides());
        assertEquals("org.mule.transformer.simple.ByteArrayToSerializable", c.getServiceOverrides().get(
            "inbound.transformer"));
        assertNotNull(TransformerUtils.firstOrNull(c.getDefaultInboundTransformers(null)));
        assertNotNull(TransformerUtils.firstOrNull(c.getDefaultOutboundTransformers(null)));
        assertTrue(TransformerUtils.firstOrNull(c.getDefaultInboundTransformers(null)) instanceof ByteArrayToSerializable);
        assertTrue(TransformerUtils.firstOrNull(c.getDefaultOutboundTransformers(null)) instanceof SerializableToByteArray);
    }

    @Test
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

    @Test
    public void testServiceOverrides3() throws InterruptedException, MuleException
    {
        // EndpointURI uri = new MuleEndpointURI("file:///temp?connector=fileConnector1");
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint(
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
        endpoint = muleContext.getEndpointFactory().getInboundEndpoint(builder);
        assertNotNull(((AbstractConnector) endpoint.getConnector()).getServiceOverrides());

        EndpointBuilder builder2 = new EndpointURIEndpointBuilder("file:///temp?connector=fileConnector3",
            muleContext);
        builder.setConnector(c);
        endpoint = muleContext.getEndpointFactory().getInboundEndpoint(builder2);
        assertNull(((AbstractConnector) endpoint.getConnector()).getServiceOverrides());

        EndpointBuilder builder3 = new EndpointURIEndpointBuilder("file:///temp?connector=fileConnector2",
            muleContext);
        builder.setConnector(c);
        endpoint = muleContext.getEndpointFactory().getInboundEndpoint(builder3);
        assertNotNull(((AbstractConnector) endpoint.getConnector()).getServiceOverrides());
    }
}
