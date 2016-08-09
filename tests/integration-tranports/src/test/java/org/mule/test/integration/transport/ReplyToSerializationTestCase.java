/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transport;


import static org.junit.Assert.assertEquals;
import static org.mule.compatibility.core.registry.MuleRegistryTransportHelper.lookupEndpointBuilder;

import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.connector.EndpointReplyToHandler;
import org.mule.compatibility.core.transport.AbstractConnector;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class ReplyToSerializationTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/transport/reply-to-serialization.xml";
  }

  @Test
  @Ignore("MULE-9307 - add behaviour to store config name to be used for reply to destination")
  public void testSerialization() throws Exception {
    EndpointBuilder jmsEndpointBuilder = lookupEndpointBuilder(muleContext.getRegistry(), "jmsEndpoint");
    EndpointBuilder vmEndpointBuilder = lookupEndpointBuilder(muleContext.getRegistry(), "vmEndpoint");

    InboundEndpoint jmsEndpoint = jmsEndpointBuilder.buildInboundEndpoint();
    Connector jmsConnector = jmsEndpoint.getConnector();
    InboundEndpoint vmEndpoint = vmEndpointBuilder.buildInboundEndpoint();
    Connector vmConnector = vmEndpoint.getConnector();

    EndpointReplyToHandler jmsHandler =
        (EndpointReplyToHandler) ((AbstractConnector) jmsConnector).getReplyToHandler(jmsEndpoint);
    EndpointReplyToHandler vmHandler = (EndpointReplyToHandler) ((AbstractConnector) vmConnector).getReplyToHandler(vmEndpoint);

    EndpointReplyToHandler jmsHandler2 = serialize(jmsHandler);
    EndpointReplyToHandler vmHandler2 = serialize(vmHandler);

    assertEquals(jmsHandler.getConnector(), jmsHandler2.getConnector());
    assertEquals(vmHandler.getConnector(), vmHandler2.getConnector());
  }

  private EndpointReplyToHandler serialize(ReplyToHandler handler) throws IOException, ClassNotFoundException, MuleException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(handler);
    oos.flush();
    EndpointReplyToHandler serialized =
        (EndpointReplyToHandler) new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject();
    serialized.initAfterDeserialisation(muleContext);
    return serialized;
  }

}
