/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transport;


import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.Connector;
import org.mule.api.transport.ReplyToHandler;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transformer.simple.ByteArrayInputStreamTransformersTestCase;
import org.mule.transport.AbstractConnector;
import org.mule.transport.DefaultReplyToHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;

public class ReplyToSerializationTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/transport/reply-to-serialization.xml";
    }

    @Test
    public void testSerialization() throws Exception
    {
        EndpointBuilder jmsEndpointBuilder = muleContext.getRegistry().lookupEndpointBuilder("jmsEndpoint");
        EndpointBuilder vmEndpointBuilder = muleContext.getRegistry().lookupEndpointBuilder("vmEndpoint");
        EndpointBuilder ajaxEndpointBuilder = muleContext.getRegistry().lookupEndpointBuilder("ajaxEndpoint");

        InboundEndpoint jmsEndpoint = jmsEndpointBuilder.buildInboundEndpoint();
        Connector jmsConnector = jmsEndpoint.getConnector();
        InboundEndpoint vmEndpoint = vmEndpointBuilder.buildInboundEndpoint();
        Connector vmConnector = vmEndpoint.getConnector();
        InboundEndpoint ajaxEndpoint = ajaxEndpointBuilder.buildInboundEndpoint();
        Connector ajaxConnector = ajaxEndpoint.getConnector();

        DefaultReplyToHandler jmsHandler = (DefaultReplyToHandler) ((AbstractConnector)jmsConnector).getReplyToHandler(jmsEndpoint);
        DefaultReplyToHandler vmHandler = (DefaultReplyToHandler) ((AbstractConnector)vmConnector).getReplyToHandler(vmEndpoint);
        DefaultReplyToHandler ajaxHandler = (DefaultReplyToHandler) ((AbstractConnector)ajaxConnector).getReplyToHandler(ajaxEndpoint);

        DefaultReplyToHandler jmsHandler2 = serialize(jmsHandler);
        DefaultReplyToHandler vmHandler2 = serialize(vmHandler);
        DefaultReplyToHandler ajaxHandler2 = serialize(ajaxHandler);

        assertEquals(jmsHandler.getConnector(), jmsHandler2.getConnector());
        assertEquals(vmHandler.getConnector(), vmHandler2.getConnector());
        assertEquals(ajaxHandler.getConnector(), ajaxHandler2.getConnector());
    }

    private DefaultReplyToHandler serialize(ReplyToHandler handler) throws IOException, ClassNotFoundException, MuleException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(handler);
        oos.flush();
        DefaultReplyToHandler serialized = (DefaultReplyToHandler) new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject();
        serialized.initAfterDeserialisation(muleContext);
        return serialized;
    }

}
