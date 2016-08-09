/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4;

import org.mule.compatibility.core.api.config.MuleEndpointProperties;
import org.mule.compatibility.core.api.endpoint.EndpointFactory;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.config.builders.TransportsConfigurationBuilder;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.construct.Flow;
import org.mule.tck.MuleEndpointTestUtils;
import org.mule.tck.testmodels.mule.TestConnector;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public abstract class AbstractMuleContextEndpointTestCase extends AbstractMuleContextTestCase {

  public static InboundEndpoint getTestInboundEndpoint(String name) throws Exception {
    return MuleEndpointTestUtils.getTestInboundEndpoint(name, muleContext);
  }

  public static OutboundEndpoint getTestOutboundEndpoint(String name) throws Exception {
    return MuleEndpointTestUtils.getTestOutboundEndpoint(name, muleContext);
  }

  public static InboundEndpoint getTestInboundEndpoint(MessageExchangePattern mep) throws Exception {
    return MuleEndpointTestUtils.getTestInboundEndpoint(mep, muleContext);
  }

  public static InboundEndpoint getTestTransactedInboundEndpoint(MessageExchangePattern mep) throws Exception {
    return MuleEndpointTestUtils.getTestTransactedInboundEndpoint(mep, muleContext);
  }

  public static InboundEndpoint getTestInboundEndpoint(String name, String uri) throws Exception {
    return MuleEndpointTestUtils.getTestInboundEndpoint(name, muleContext, uri, null, null, null, null);
  }

  public static OutboundEndpoint getTestOutboundEndpoint(String name, String uri) throws Exception {
    return MuleEndpointTestUtils.getTestOutboundEndpoint(name, muleContext, uri, null, null, null);
  }

  public static InboundEndpoint getTestInboundEndpoint(String name, List<Transformer> transformers) throws Exception {
    return MuleEndpointTestUtils.getTestInboundEndpoint(name, muleContext, null, transformers, null, null, null);
  }

  public static OutboundEndpoint getTestOutboundEndpoint(String name, List<Transformer> transformers) throws Exception {
    return MuleEndpointTestUtils.getTestOutboundEndpoint(name, muleContext, null, transformers, null, null);
  }

  public static InboundEndpoint getTestInboundEndpoint(String name, String uri, List<Transformer> transformers, Filter filter,
                                                       Map<String, Serializable> properties, Connector connector)
      throws Exception {
    return MuleEndpointTestUtils.getTestInboundEndpoint(name, muleContext, uri, transformers, filter, properties, connector);
  }

  public static OutboundEndpoint getTestOutboundEndpoint(String name, String uri, List<Transformer> transformers, Filter filter,
                                                         Map<String, Serializable> properties)
      throws Exception {
    return MuleEndpointTestUtils.getTestOutboundEndpoint(name, muleContext, uri, transformers, filter, properties);
  }

  public static OutboundEndpoint getTestOutboundEndpoint(String name, String uri, List<Transformer> transformers, Filter filter,
                                                         Map<String, Serializable> properties, Connector connector)
      throws Exception {
    return MuleEndpointTestUtils.getTestOutboundEndpoint(name, muleContext, uri, transformers, filter, properties, connector);
  }

  public static MuleEvent getTestEvent(Object data, InboundEndpoint endpoint) throws Exception {
    return MuleEndpointTestUtils.getTestEvent(data, endpoint, muleContext);
  }

  public static MuleEvent getTestEvent(Object data, Flow flow, InboundEndpoint endpoint) throws Exception {
    return MuleEndpointTestUtils.getTestEvent(data, flow, endpoint, muleContext);
  }

  public static TestConnector getTestConnector() throws Exception {
    return MuleEndpointTestUtils.getTestConnector(muleContext);
  }

  protected EndpointFactory getEndpointFactory() {
    return (EndpointFactory) muleContext.getRegistry().lookupObject(MuleEndpointProperties.OBJECT_MULE_ENDPOINT_FACTORY);
  }

  @Override
  protected ConfigurationBuilder getBuilder() throws Exception {
    return new TransportsConfigurationBuilder();
  }

}
