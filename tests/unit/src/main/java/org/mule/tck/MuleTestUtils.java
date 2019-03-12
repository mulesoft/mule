/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import static java.util.Collections.singletonMap;
import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.core.api.construct.Flow.builder;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.processor.strategy.StreamPerEventSink;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mockito.Mockito;

/**
 * Utilities for creating test and Mock Mule objects
 */
public final class MuleTestUtils {

  public static final String APPLE_SERVICE = "appleService";
  public static final String APPLE_FLOW = "appleFlow";

  /**
   * Creates an {@link Error} mock that will return the provided exception when calling {@link Error#getCause()}
   *
   * @param exception the exception to use to create the mock
   * @return a mocked {@link Error}
   */
  public static Error createErrorMock(Exception exception) {
    Error errorMock = mock(Error.class);
    when(errorMock.getCause()).thenReturn(exception);
    return errorMock;
  }

  public static Injector spyInjector(MuleContext muleContext) {
    Injector spy = spy(muleContext.getInjector());
    ((DefaultMuleContext) muleContext).setInjector(spy);

    return spy;
  }

  /**
   * Creates an empty flow named {@link MuleTestUtils#APPLE_FLOW}.
   */
  public static Flow getTestFlow(MuleContext context) throws MuleException {
    // Use direct processing strategy given flow used in test event is not used for processing.
    return createFlow(context, APPLE_FLOW);
  }

  /**
   * Creates an empty flow with the provided name.
   */
  public static Flow createFlow(MuleContext context, String flowName) throws MuleException {
    final Flow flow = builder(flowName, context).processingStrategyFactory((muleContext, schedulersNamePrefix) -> {
      return withContextClassLoader(MuleTestUtils.class.getClassLoader(), () -> spy(new TestDirectProcessingStrategy()));
    }).build();
    flow.setAnnotations(singletonMap(LOCATION_KEY, fromSingleComponent(flowName)));
    return flow;
  }

  /**
   * Creates a new flow and registers it in the given {@code mockComponentLocator}
   *
   * @param mockComponentLocator a {@link Mockito#mock(Class)} {@link ConfigurationComponentLocator}
   */
  public static Flow createAndRegisterFlow(MuleContext context, String flowName,
                                           ConfigurationComponentLocator mockComponentLocator)
      throws MuleException {
    Flow flow = createFlow(context, flowName);
    MuleRegistry registry = ((MuleContextWithRegistry) context).getRegistry();
    if (registry != null) {
      registry.registerFlowConstruct(flow);
    }
    when(mockComponentLocator.find(Location.builder().globalName(flowName).build())).thenReturn(of(flow));
    return flow;
  }

  /**
   * Executes callback with a given system property set and replaces the system property with it's original value once done.
   * Useful for asserting behaviour that is dependent on the presence of a system property.
   *
   * @param propertyName  Name of system property to set
   * @param propertyValue Value of system property
   * @param callback      Callback implementing the the test code and assertions to be run with system property set.
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

  /**
   * Executes callback with a given system properties set and replaces the system properties with their original values once done.
   * Useful for asserting behaviour that is dependent on the presence of a system property.
   *
   * @param properties {@link Map} of property name and property value to be set.
   * @param callback   Callback implementing the the test code and assertions to be run with system property set.
   * @throws Exception any exception thrown by the execution of callback
   */
  public static void testWithSystemProperties(Map<String, String> properties, TestCallback callback)
      throws Exception {
    assert properties != null && callback != null;
    Map<String, String> originalPropertyValues = new HashMap<>();
    properties.forEach((propertyName, propertyValue) -> {
      if (propertyValue == null) {
        originalPropertyValues.put(propertyName, System.clearProperty(propertyName));
      } else {
        originalPropertyValues.put(propertyName, System.setProperty(propertyName, propertyValue));
      }
    });
    try {
      callback.run();
    } finally {
      originalPropertyValues.forEach((propertyName, originalPropertyValue) -> {
        if (originalPropertyValue == null) {
          System.clearProperty(propertyName);
        } else {
          System.setProperty(propertyName, originalPropertyValue);
        }
      });
    }
  }

  public static class TestDirectProcessingStrategy implements ProcessingStrategy {

    @Override
    public boolean isSynchronous() {
      return true;
    }

    @Override
    public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor pipeline) {
      return new StreamPerEventSink(pipeline, event -> {
      });
    }
  }


  public interface TestCallback {

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

  /**
   * Returns the exception listener configured on a messaging exception handler
   * <p/>
   * Invokes {@code getExceptionListeners} method on the provided exception handler to avoid exposing that method on the public
   * API.
   *
   * @param exceptionHandler exception handler to inspect
   * @return the list of configured exception listeners
   * @throws IllegalStateException if the provided exception handler does not have the expect method or it cannot be invoked.
   */
  public static List<FlowExceptionHandler> getExceptionListeners(FlowExceptionHandler exceptionHandler) {
    try {
      Method getExceptionListenersMethod = exceptionHandler.getClass().getMethod("getExceptionListeners");
      Object exceptionListeners = getExceptionListenersMethod.invoke(exceptionHandler);
      return (List<FlowExceptionHandler>) exceptionListeners;
    } catch (Exception e) {
      throw new IllegalStateException("Cannot obtain exception listener for flow");
    }
  }

  /**
   * Returns the object message processors
   * <p/>
   * Invokes {@code getMessageProcessors} method on the provided object to avoid exposing that method on the public API.
   *
   * @param object the object owning the list of {@link Processor}s
   * @return the list of configured processors
   * @throws IllegalStateException if the provided exception handler does not have the expect method or it cannot be invoked.
   */
  public static List<Processor> getMessageProcessors(Object object) {
    try {
      Method getExceptionListenersMethod = object.getClass().getMethod("getMessageProcessors");
      Object processors = getExceptionListenersMethod.invoke(object);
      return (List<Processor>) processors;
    } catch (Exception e) {
      throw new IllegalStateException("Cannot obtain processors from the object");
    }
  }

  public static <M extends ComponentModel> CompletableComponentExecutor<M> mockComponentExecutor(Object returnValue) {
    return stubComponentExecutor(mock(CompletableComponentExecutor.class, withSettings().lenient()), returnValue);
  }

  public static <M extends ComponentModel> CompletableComponentExecutor<M> stubComponentExecutor(CompletableComponentExecutor<M> executor,
                                                                                                 Object returnValue) {
    doAnswer(invocation -> {
      CompletableComponentExecutor.ExecutorCallback callback = invocation.getArgument(1);
      callback.complete(returnValue);
      return null;
    }).when(executor).execute(any(), any());

    return executor;
  }

  public static <M extends ComponentModel> CompletableComponentExecutor<M> stubFailingComponentExecutor(CompletableComponentExecutor<M> executor,
                                                                                                        Throwable t) {
    doAnswer(invocation -> {
      CompletableComponentExecutor.ExecutorCallback callback = invocation.getArgument(1);
      callback.error(t);
      return null;
    }).when(executor).execute(any(), any());

    return executor;
  }
}
