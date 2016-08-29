/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import static org.mule.compatibility.core.DefaultMuleEventEndpointUtils.populateFieldsFromInboundEndpoint;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import static org.mule.tck.MuleTestUtils.getTestSession;
import static org.mule.tck.junit4.AbstractMuleTestCase.TEST_CONNECTOR;

import org.mule.compatibility.core.api.config.MuleEndpointProperties;
import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.EndpointFactory;
import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.api.transport.MuleMessageFactory;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.compatibility.core.endpoint.MuleEndpointURI;
import org.mule.compatibility.core.transport.AbstractConnector;
import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.routing.MessageFilter;
import org.mule.runtime.core.transaction.MuleTransactionConfig;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.tck.testmodels.mule.TestTransactionFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MuleEndpointTestUtils {

  public static InboundEndpoint getTestInboundEndpoint(String name, final MuleContext context) throws Exception {
    return (InboundEndpoint) getTestEndpoint(name, null, null, null, null, context,
                                             builder -> getEndpointFactory(context.getRegistry()).getInboundEndpoint(builder),
                                             null);
  }

  public static OutboundEndpoint getTestOutboundEndpoint(String name, final MuleContext context) throws Exception {
    return (OutboundEndpoint) getTestEndpoint(name, null, null, null, null, context,
                                              builder -> getEndpointFactory(context.getRegistry()).getOutboundEndpoint(builder),
                                              null);
  }

  public static InboundEndpoint getTestInboundEndpoint(String name, final MuleContext context, String uri,
                                                       List<Transformer> transformers, Filter filter,
                                                       Map<String, Serializable> properties, Connector connector)
      throws Exception {
    return (InboundEndpoint) getTestEndpoint(name, uri, transformers, filter, properties, context,
                                             builder -> getEndpointFactory(context.getRegistry()).getInboundEndpoint(builder),
                                             connector);
  }

  public static OutboundEndpoint getTestOutboundEndpoint(String name, final MuleContext context, String uri,
                                                         List<Transformer> transformers, Filter filter,
                                                         Map<String, Serializable> properties)
      throws Exception {
    return (OutboundEndpoint) getTestEndpoint(name, uri, transformers, filter, properties, context,
                                              builder -> getEndpointFactory(context.getRegistry()).getOutboundEndpoint(builder),
                                              null);
  }

  public static InboundEndpoint getTestInboundEndpoint(String name, final MuleContext context, String uri,
                                                       List<Transformer> transformers, Filter filter,
                                                       Map<String, Serializable> properties)
      throws Exception {
    return (InboundEndpoint) getTestEndpoint(name, uri, transformers, filter, properties, context,
                                             builder -> getEndpointFactory(context.getRegistry()).getInboundEndpoint(builder),
                                             null);
  }

  public static OutboundEndpoint getTestOutboundEndpoint(String name, final MuleContext context, String uri,
                                                         List<Transformer> transformers, Filter filter,
                                                         Map<String, Serializable> properties, final Connector connector)
      throws Exception {
    return (OutboundEndpoint) getTestEndpoint(name, uri, transformers, filter, properties, context, builder -> {
      builder.setConnector(connector);
      return getEndpointFactory(context.getRegistry()).getOutboundEndpoint(builder);
    }, null);
  }

  public static OutboundEndpoint getTestOutboundEndpoint(final MessageExchangePattern mep, final MuleContext context, String uri,
                                                         final Connector connector)
      throws Exception {
    return (OutboundEndpoint) getTestEndpoint(null, uri, null, null, null, context, builder -> {
      builder.setConnector(connector);
      builder.setExchangePattern(mep);
      return getEndpointFactory(context.getRegistry()).getOutboundEndpoint(builder);
    }, null);
  }

  public static OutboundEndpoint getTestOutboundEndpoint(String name, final MessageExchangePattern mep, final MuleContext context)
      throws Exception {
    return (OutboundEndpoint) getTestEndpoint(name, null, null, null, null, context, builder -> {
      builder.setExchangePattern(mep);
      return getEndpointFactory(context.getRegistry()).getOutboundEndpoint(builder);
    }, null);
  }

  public static OutboundEndpoint getTestOutboundEndpoint(final MessageExchangePattern mep, final MuleContext context)
      throws Exception {
    return (OutboundEndpoint) getTestEndpoint(null, null, null, null, null, context, builder -> {
      builder.setExchangePattern(mep);
      return getEndpointFactory(context.getRegistry()).getOutboundEndpoint(builder);
    }, null);
  }

  public static InboundEndpoint getTestInboundEndpoint(String name, final MessageExchangePattern mep, final MuleContext context,
                                                       final Connector connector)
      throws Exception {
    return (InboundEndpoint) getTestEndpoint(name, null, null, null, null, context, builder -> {
      builder.setExchangePattern(mep);
      return getEndpointFactory(context.getRegistry()).getInboundEndpoint(builder);
    }, connector);
  }

  public static InboundEndpoint getTestInboundEndpoint(final MessageExchangePattern mep, final MuleContext context)
      throws Exception {
    return (InboundEndpoint) getTestEndpoint(null, null, null, null, null, context, builder -> {
      builder.setExchangePattern(mep);
      return getEndpointFactory(context.getRegistry()).getInboundEndpoint(builder);
    }, null);
  }

  public static InboundEndpoint getTestTransactedInboundEndpoint(final MessageExchangePattern mep, final MuleContext context)
      throws Exception {
    return (InboundEndpoint) getTestEndpoint(null, null, null, null, null, context, builder -> {
      builder.setExchangePattern(mep);
      TransactionConfig txConfig = new MuleTransactionConfig(TransactionConfig.ACTION_BEGIN_OR_JOIN);
      txConfig.setFactory(new TestTransactionFactory());
      builder.setTransactionConfig(txConfig);
      return getEndpointFactory(context.getRegistry()).getInboundEndpoint(builder);
    }, null);
  }

  private static ImmutableEndpoint getTestEndpoint(String name, String uri, List<Transformer> transformers, Filter filter,
                                                   Map<String, Serializable> properties, MuleContext context,
                                                   EndpointSource source, Connector connector)
      throws Exception {
    final Map<String, Serializable> props = new HashMap<>();
    props.put("name", name);
    props.put("endpointURI", new MuleEndpointURI("test://test", context));
    props.put("connector", "testConnector");
    if (connector == null) {
      // need to build endpoint this way to avoid depenency to any endpoint
      // jars
      connector = (Connector) ClassUtils.loadClass("org.mule.tck.testmodels.mule.TestConnector", AbstractMuleTestCase.class)
          .getConstructor(MuleContext.class).newInstance(context);
    }

    connector.setName("testConnector");
    context.getRegistry().applyLifecycle(connector);

    final String endpoingUri = uri == null ? "test://test" : uri;
    final EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(endpoingUri, context);
    endpointBuilder.setConnector(connector);
    endpointBuilder.setName(name);
    if (transformers != null) {
      endpointBuilder.setTransformers(transformers);
    }

    if (properties != null) {
      endpointBuilder.setProperties(properties);
    }
    endpointBuilder.addMessageProcessor(new MessageFilter(filter));
    return source.getEndpoint(endpointBuilder);
  }

  private interface EndpointSource {

    ImmutableEndpoint getEndpoint(EndpointBuilder builder) throws MuleException;
  }

  public static ImmutableEndpoint getTestSchemeMetaInfoInboundEndpoint(String name, String protocol, final MuleContext context)
      throws Exception {
    return getTestSchemeMetaInfoEndpoint(name, protocol, context,
                                         builder -> getEndpointFactory(context.getRegistry()).getInboundEndpoint(builder));
  }

  public static ImmutableEndpoint getTestSchemeMetaInfoOutboundEndpoint(String name, String protocol, final MuleContext context)
      throws Exception {
    return getTestSchemeMetaInfoEndpoint(name, protocol, context,
                                         builder -> getEndpointFactory(context.getRegistry()).getOutboundEndpoint(builder));
  }

  private static ImmutableEndpoint getTestSchemeMetaInfoEndpoint(String name, String protocol, MuleContext context,
                                                                 EndpointSource source)
      throws Exception {
    // need to build endpoint this way to avoid depenency to any endpoint jars
    final AbstractConnector connector = (AbstractConnector) ClassUtils
        .loadClass("org.mule.tck.testmodels.mule.TestConnector", AbstractMuleTestCase.class).newInstance();

    connector.setName("testConnector");
    context.getRegistry().applyLifecycle(connector);
    connector.registerSupportedProtocol(protocol);

    final EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder("test:" + protocol + "://test", context);
    endpointBuilder.setConnector(connector);
    endpointBuilder.setName(name);
    return source.getEndpoint(endpointBuilder);
  }

  /**
   * Supply endpoint but no service
   */
  public static MuleEvent getTestEvent(Object data, InboundEndpoint endpoint, MuleContext context) throws Exception {
    return getTestEvent(data, getTestFlow(context), endpoint, context);
  }

  public static MuleEvent getTestEvent(Object data, FlowConstruct flowConstruct, InboundEndpoint endpoint, MuleContext context)
      throws Exception {
    final MuleSession session = getTestSession(flowConstruct, context);

    final MuleMessageFactory factory = endpoint.getConnector().createMuleMessageFactory();
    final MuleMessage message = factory.create(data, endpoint.getEncoding());

    final MuleEvent event = MuleEvent.builder(DefaultMessageContext.create(flowConstruct, TEST_CONNECTOR)).message(message)
        .flow(flowConstruct).session(session).build();
    populateFieldsFromInboundEndpoint(event, endpoint);
    return event;
  }

  public static TestConnector getTestConnector(MuleContext context) throws Exception {
    final TestConnector testConnector = new TestConnector(context);
    testConnector.setName("testConnector");
    context.getRegistry().applyLifecycle(testConnector);
    return testConnector;
  }

  public static EndpointFactory getEndpointFactory(MuleRegistry registry) {
    return (EndpointFactory) registry.lookupObject(MuleEndpointProperties.OBJECT_MULE_ENDPOINT_FACTORY);
  }

  public static EndpointBuilder lookupEndpointBuilder(MuleRegistry registry, String name) {
    Object o = registry.lookupObject(name);
    if (o instanceof EndpointBuilder) {
      return (EndpointBuilder) o;
    } else {
      return null;
    }
  }
}
