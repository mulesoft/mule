/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.mule.compatibility.core.api.config.MuleEndpointProperties;
import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.EndpointFactory;
import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.compatibility.core.transport.AbstractConnector;
import org.mule.compatibility.core.transport.service.DefaultEndpointAwareTransformer;
import org.mule.compatibility.transport.file.FileConnector;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.transformer.TransformerUtils;
import org.mule.runtime.core.transformer.simple.ByteArrayToSerializable;
import org.mule.runtime.core.transformer.simple.SerializableToByteArray;

import org.junit.Test;

public class ConnectorServiceOverridesTestCase extends FunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "test-connector-config.xml";
  }

  @Test
  public void testServiceOverrides() throws InterruptedException {
    FileConnector c = (FileConnector) muleContext.getRegistry().lookupObject("fileConnector2");
    assertNotNull(c);
    assertNotNull(c.getServiceOverrides());
    assertEquals("org.mule.runtime.core.transformer.simple.ByteArrayToSerializable",
                 c.getServiceOverrides().get("inbound.transformer"));
    assertNotNull(TransformerUtils.firstOrNull(c.getDefaultInboundTransformers(null)));
    assertNotNull(TransformerUtils.firstOrNull(c.getDefaultOutboundTransformers(null)));
    assertThat(TransformerUtils.firstOrNull(c.getDefaultInboundTransformers(null)),
               instanceOf(DefaultEndpointAwareTransformer.class));
    assertThat(((DefaultEndpointAwareTransformer) TransformerUtils.firstOrNull(c.getDefaultInboundTransformers(null)))
        .getTransformer(), instanceOf(ByteArrayToSerializable.class));
    assertThat(TransformerUtils.firstOrNull(c.getDefaultOutboundTransformers(null)),
               instanceOf(DefaultEndpointAwareTransformer.class));
    assertThat(((DefaultEndpointAwareTransformer) TransformerUtils.firstOrNull(c.getDefaultOutboundTransformers(null)))
        .getTransformer(), instanceOf(SerializableToByteArray.class));
  }

  @Test
  public void testServiceOverrides2() throws InterruptedException {
    FileConnector c = (FileConnector) muleContext.getRegistry().lookupObject("fileConnector1");
    assertNotNull(c);
    assertNull(c.getServiceOverrides());

    c = (FileConnector) muleContext.getRegistry().lookupObject("fileConnector2");
    assertNotNull(c);
    assertNotNull(c.getServiceOverrides());

    c = (FileConnector) muleContext.getRegistry().lookupObject("fileConnector3");
    assertNotNull(c);
    assertNull(c.getServiceOverrides());
  }

  @Test
  public void testServiceOverrides3() throws InterruptedException, MuleException {
    // EndpointURI uri = new MuleEndpointURI("file:///temp?connector=fileConnector1");
    ImmutableEndpoint endpoint = getEndpointFactory().getInboundEndpoint("file:///temp?connector=fileConnector1");

    assertNotNull(endpoint);
    assertNotNull(endpoint.getConnector());
    assertNull(((AbstractConnector) endpoint.getConnector()).getServiceOverrides());

    FileConnector c = (FileConnector) muleContext.getRegistry().lookupObject("fileConnector2");
    assertNotNull(c);
    assertNotNull(c.getServiceOverrides());

    EndpointBuilder builder = new EndpointURIEndpointBuilder("file:///temp?connector=fileConnector1", muleContext);
    builder.setConnector(c);
    endpoint = getEndpointFactory().getInboundEndpoint(builder);
    assertNotNull(((AbstractConnector) endpoint.getConnector()).getServiceOverrides());

    EndpointBuilder builder2 = new EndpointURIEndpointBuilder("file:///temp?connector=fileConnector3", muleContext);
    builder.setConnector(c);
    endpoint = getEndpointFactory().getInboundEndpoint(builder2);
    assertNull(((AbstractConnector) endpoint.getConnector()).getServiceOverrides());

    EndpointBuilder builder3 = new EndpointURIEndpointBuilder("file:///temp?connector=fileConnector2", muleContext);
    builder.setConnector(c);
    endpoint = getEndpointFactory().getInboundEndpoint(builder3);
    assertNotNull(((AbstractConnector) endpoint.getConnector()).getServiceOverrides());
  }

  public EndpointFactory getEndpointFactory() {
    return (EndpointFactory) muleContext.getRegistry().lookupObject(MuleEndpointProperties.OBJECT_MULE_ENDPOINT_FACTORY);
  }
}
