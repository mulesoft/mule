/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import static org.mockito.Mockito.spy;

import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.api.component.JavaComponent;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.component.DefaultJavaComponent;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.object.SingletonObjectFactory;
import org.mule.runtime.core.session.DefaultMuleSession;
import org.mule.tck.testmodels.mule.TestAgent;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;

import java.util.ArrayList;
import java.util.Map;

/**
 * Utilities for creating test and Mock Mule objects
 */
public final class MuleTestUtils {

  public static final String APPLE_SERVICE = "appleService";
  public static final String APPLE_FLOW = "appleFlow";

  // public static Endpoint getTestEndpoint(String name, String type, MuleContext
  // context) throws Exception
  // {
  // Map props = new HashMap();
  // props.put("name", name);
  // props.put("type", type);
  // props.put("endpointURI", new MuleEndpointURI("test://test"));
  // props.put("connector", "testConnector");
  // // need to build endpoint this way to avoid depenency to any endpoint jars
  // AbstractConnector connector = null;
  // connector =
  // (AbstractConnector)ClassUtils.loadClass("org.mule.tck.testmodels.mule.TestConnector",
  // AbstractMuleTestCase.class).newInstance();
  //
  // connector.setName("testConnector");
  // connector.setMuleContext(context);
  // context.applyLifecycle(connector);
  //
  // EndpointBuilder endpointBuilder = new
  // EndpointURIEndpointBuilder("test://test", context);
  // endpointBuilder.setConnector(connector);
  // endpointBuilder.setName(name);
  // if (ImmutableEndpoint.ENDPOINT_TYPE_RECEIVER.equals(type))
  // {
  // return (Endpoint)
  // context.getEndpointFactory().getInboundEndpoint(endpointBuilder);
  // }
  // else if (ImmutableEndpoint.ENDPOINT_TYPE_SENDER.equals(type))
  // {
  // return (Endpoint)
  // context.getEndpointFactory().getOutboundEndpoint(endpointBuilder);
  // }
  // else
  // {
  // throw new IllegalArgumentException("The endpoint type: " + type +
  // "is not recognized.");
  //
  // }
  // }


  public static Injector spyInjector(MuleContext muleContext) {
    Injector spy = spy(muleContext.getInjector());
    ((DefaultMuleContext) muleContext).setInjector(spy);

    return spy;
  }

  // public static Endpoint getTestSchemeMetaInfoEndpoint(String name, String type,
  // String protocol, MuleContext context)
  // throws Exception
  // {
  // // need to build endpoint this way to avoid depenency to any endpoint jars
  // AbstractConnector connector = null;
  // connector = (AbstractConnector)
  // ClassUtils.loadClass("org.mule.tck.testmodels.mule.TestConnector",
  // AbstractMuleTestCase.class).newInstance();
  //
  // connector.setName("testConnector");
  // connector.setMuleContext(context);
  // context.applyLifecycle(connector);
  // connector.registerSupportedProtocol(protocol);
  //
  // EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder("test:" +
  // protocol + "://test", context);
  // endpointBuilder.setConnector(connector);
  // endpointBuilder.setName(name);
  // if (ImmutableEndpoint.ENDPOINT_TYPE_RECEIVER.equals(type))
  // {
  // return (Endpoint)
  // context.getEndpointFactory().getInboundEndpoint(endpointBuilder);
  // }
  // else if (ImmutableEndpoint.ENDPOINT_TYPE_SENDER.equals(type))
  // {
  // return (Endpoint)
  // context.getEndpointFactory().getOutboundEndpoint(endpointBuilder);
  // }
  // else
  // {
  // throw new IllegalArgumentException("The endpoint type: " + type +
  // "is not recognized.");
  //
  // }
  // }


  /**
   * Supply no service, no endpoint
   */
  public static MuleEvent getTestEvent(Object data, MuleContext context) throws Exception {
    return getTestEvent(data, getTestFlow(context), MessageExchangePattern.REQUEST_RESPONSE, context);
  }

  public static MuleEvent getTestEvent(MuleMessage message, MessageExchangePattern mep, MuleContext context) throws Exception {
    return getTestEvent(message, getTestFlow(context), mep, context);
  }

  public static MuleEvent getTestEvent(Object data, MessageExchangePattern mep, MuleContext context) throws Exception {
    return getTestEvent(data, getTestFlow(context), mep, context);
  }

  // public static MuleEvent getTestInboundEvent(Object data, MuleContext context) throws Exception
  // {
  // return getTestInboundEvent(data, getTestService(context), MessageExchangePattern.REQUEST_RESPONSE,
  // context);
  // }
  //
  // public static MuleEvent getTestInboundEvent(Object data, MessageExchangePattern mep, MuleContext context)
  // throws Exception
  // {
  // return getTestInboundEvent(data, getTestService(context), mep, context);
  // }


  public static MuleEvent getTestEvent(MuleMessage message, FlowConstruct flowConstruct, MessageExchangePattern mep,
                                       MuleContext context)
      throws Exception {
    final MuleSession session = getTestSession(flowConstruct, context);
    final DefaultMuleEvent event = new DefaultMuleEvent(message, mep, flowConstruct, session);
    return event;
  }

  public static MuleEvent getTestEvent(Object data, FlowConstruct flowConstruct, MessageExchangePattern mep, MuleContext context)
      throws Exception {
    final MuleSession session = getTestSession(flowConstruct, context);
    final DefaultMuleEvent event = new DefaultMuleEvent(MuleMessage.builder().payload(data).build(), mep, flowConstruct, session);
    return event;
  }


  /**
   * Supply endpoint but no service
   */
  public static MuleEvent getTestEvent(Object data, FlowConstruct flowConstruct, MuleContext context) throws Exception {
    final MuleSession session = getTestSession(flowConstruct, context);
    final DefaultMuleEvent event = new DefaultMuleEvent(MuleMessage.builder().payload(data).build(), flowConstruct, session);
    return event;
  }

  public static MuleEventContext getTestEventContext(Object data, MessageExchangePattern mep, MuleContext context)
      throws Exception {
    try {
      final MuleEvent event = getTestEvent(data, mep, context);
      RequestContext.setEvent(event);
      return RequestContext.getEventContext();
    } finally {
      RequestContext.setEvent(null);
    }
  }

  public static Transformer getTestTransformer() throws Exception {
    final Transformer t = new TestCompressionTransformer();
    t.initialise();
    return t;
  }

  public static MuleSession getTestSession(FlowConstruct flowConstruct, MuleContext context) {
    return new DefaultMuleSession();
  }

  public static MuleSession getTestSession(MuleContext context) {
    return getTestSession(null, context);
  }

  public static Flow getTestFlow(MuleContext context) throws Exception {
    return getTestFlow(APPLE_FLOW, context);
  }

  public static Flow getTestFlow(String name, Class<?> clazz, MuleContext context) throws Exception {
    return getTestFlow(name, clazz, null, context);
  }

  @Deprecated
  public static Flow getTestFlow(String name, Class<?> clazz, Map props, MuleContext context) throws Exception {
    return getTestFlow(name, clazz, props, context, true);
  }

  public static Flow getTestFlow(String name, MuleContext context) throws Exception {
    return getTestFlow(name, context, true);
  }

  public static Flow getTestFlow(String name, Class<?> clazz, Map props, MuleContext context, boolean initialize)
      throws Exception {
    final SingletonObjectFactory of = new SingletonObjectFactory(clazz, props);
    of.initialise();
    final JavaComponent component = new DefaultJavaComponent(of);
    ((MuleContextAware) component).setMuleContext(context);

    return getTestFlow(name, component, initialize, context);
  }

  public static Flow getTestFlow(String name, MuleContext context, boolean initialize) throws Exception {
    final Flow flow = new Flow(name, context);
    if (initialize) {
      context.getRegistry().registerFlowConstruct(flow);
    }

    return flow;
  }

  public static Flow getTestFlow(String name, Object component, boolean initialize, MuleContext context) throws Exception {
    final Flow flow = new Flow(name, context);
    flow.setMessageProcessors(new ArrayList<MessageProcessor>());
    if (component instanceof Component) {
      flow.getMessageProcessors().add((MessageProcessor) component);
    } else {
      flow.getMessageProcessors().add(new DefaultJavaComponent(new SingletonObjectFactory(component)));

    }
    if (initialize) {
      context.getRegistry().registerFlowConstruct(flow);
    }
    return flow;
  }

  public static TestAgent getTestAgent() throws Exception {
    final TestAgent t = new TestAgent();
    t.initialise();
    return t;
  }

  /**
   * Execute callback with a given system property set and replaces the system property with it's original value once done. Useful
   * for asserting behaviour that is dependent on the presence of a system property.
   * 
   * @param propertyName Name of system property to set
   * @param propertyValue Value of system property
   * @param callback Callback implementing the the test code and assertions to be run with system property set.
   * @throws Exception any exception thrown by the execution of callback
   */
  public static void testWithSystemProperty(String propertyName, String propertyValue, TestCallback callback) throws Exception {
    assert propertyName != null && callback != null;
    String originalPropertyValue = null;
    try {
      if (propertyValue == null) {
        originalPropertyValue = System.clearProperty(propertyName);
      } else {
        originalPropertyValue = System.setProperty(propertyName, propertyValue);
      }
      callback.run();
    } finally {
      if (originalPropertyValue == null) {
        System.clearProperty(propertyName);
      } else {
        System.setProperty(propertyName, originalPropertyValue);
      }
    }
  }

  public static interface TestCallback {

    void run() throws Exception;
  }

  /**
   * Returns a currently running {@link Thread} of the given {@code name}
   *
   * @param name the name of the {@link Thread} you're looking for
   * @return a {@link Thread} or {@code null} if none found
   */
  public static Thread getRunningThreadByName(String name) {
    for (Thread thread : Thread.getAllStackTraces().keySet()) {
      if (thread.getName().equals(name)) {
        return thread;
      }
    }

    return null;
  }
}
