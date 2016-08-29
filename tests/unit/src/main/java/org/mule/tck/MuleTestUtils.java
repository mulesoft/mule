/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.DefaultMuleEvent.setCurrentEvent;
import static org.mule.tck.junit4.AbstractMuleTestCase.TEST_CONNECTOR;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.DefaultMuleEventContext;
import org.mule.runtime.core.MessageExchangePattern;
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
import org.mule.runtime.core.component.DefaultJavaComponent;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.object.SingletonObjectFactory;
import org.mule.runtime.core.session.DefaultMuleSession;

import java.util.ArrayList;
import java.util.Map;

import org.mockito.Mockito;

/**
 * Utilities for creating test and Mock Mule objects
 */
public final class MuleTestUtils {

  public static final String APPLE_SERVICE = "appleService";
  public static final String APPLE_FLOW = "appleFlow";

  /**
   * Creates an {@link Error} mock that will return the provided exception when calling {@link Error#getException()}
   * 
   * @param exception the exception to use to create the mock
   * @return a mocked {@link Error}
   */
  public static Error createErrorMock(Exception exception) {
    Error errorMock = Mockito.mock(Error.class);
    when(errorMock.getException()).thenReturn(exception);
    return errorMock;
  }

  public static Injector spyInjector(MuleContext muleContext) {
    Injector spy = spy(muleContext.getInjector());
    ((DefaultMuleContext) muleContext).setInjector(spy);

    return spy;
  }

  /**
   * Supply no service, no endpoint
   */
  public static MuleEvent getTestEvent(Object data, MuleContext context) throws Exception {
    return getTestEvent(data, getTestFlow(context), MessageExchangePattern.REQUEST_RESPONSE, context);
  }

  public static MuleEvent getTestEvent(Object data, MessageExchangePattern mep, MuleContext context)
      throws Exception {
    return getTestEvent(data, getTestFlow(context), mep, context);
  }

  public static MuleEvent getTestEvent(Object data,
                                       FlowConstruct flowConstruct,
                                       MessageExchangePattern mep,
                                       MuleContext context)
      throws Exception {
    return MuleEvent.builder(DefaultMessageContext.create(flowConstruct, TEST_CONNECTOR))
        .message(MuleMessage.builder().payload(data).build()).exchangePattern(mep).flow(flowConstruct)
        .session(getTestSession(flowConstruct, context)).build();
  }


  public static MuleEventContext getTestEventContext(Object data,
                                                     MessageExchangePattern mep,
                                                     MuleContext context)
      throws Exception {
    try {
      final MuleEvent event = getTestEvent(data, mep, context);
      setCurrentEvent(event);
      return new DefaultMuleEventContext(getTestFlow(context), event);
    } finally {
      setCurrentEvent(null);
    }
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
  public static Flow getTestFlow(String name, Class<?> clazz, Map props, MuleContext context)
      throws Exception {
    return getTestFlow(name, clazz, props, context, true);
  }

  public static Flow getTestFlow(String name, MuleContext context) throws Exception {
    return getTestFlow(name, context, true);
  }

  public static Flow getTestFlow(String name,
                                 Class<?> clazz,
                                 Map props,
                                 MuleContext context,
                                 boolean initialize)
      throws Exception {
    final SingletonObjectFactory of = new SingletonObjectFactory(clazz, props);
    of.initialise();
    final JavaComponent component = new DefaultJavaComponent(of);
    ((MuleContextAware) component).setMuleContext(context);

    return getTestFlow(name, component, initialize, context);
  }

  public static Flow getTestFlow(String name, MuleContext context, boolean initialize)
      throws Exception {
    final Flow flow = new Flow(name, context);
    if (initialize) {
      context.getRegistry().registerFlowConstruct(flow);
    }

    return flow;
  }

  public static Flow getTestFlow(String name, Object component, boolean initialize, MuleContext context)
      throws Exception {
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

  /**
   * Execute callback with a given system property set and replaces the system property with it's original value once done. Useful
   * for asserting behaviour that is dependent on the presence of a system property.
   * 
   * @param propertyName Name of system property to set
   * @param propertyValue Value of system property
   * @param callback Callback implementing the the test code and assertions to be run with system property set.
   * @throws Exception any exception thrown by the execution of callback
   */
  public static void testWithSystemProperty(String propertyName, String propertyValue, TestCallback callback)
      throws Exception {
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
