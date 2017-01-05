/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.execution.interception;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.dsl.api.component.ComponentIdentifier.ANNOTATION_NAME;
import static org.mule.runtime.dsl.api.component.ComponentIdentifier.ANNOTATION_PARAMETERS;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.interception.ProcessorInterceptionManager;
import org.mule.runtime.core.api.interception.ProcessorInterceptorCallback;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.xml.namespace.QName;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class InterceptorMessageProcessorTestCase {

  private InterceptorMessageProcessor interceptorMessageProcessor;
  @Rule
  public ExpectedException expectedException = none();
  @Mock
  private ComponentIdentifier componentIdentifier;
  @Mock
  private Map<String, Object> parameters;
  @Mock
  private MuleContext muleContext;
  @Mock
  private ProcessorInterceptionManager processorInterceptorManager;
  @Mock
  private Processor intercepted;

  @Before
  public void before() throws Exception {
    interceptorMessageProcessor = new InterceptorMessageProcessor();

    componentIdentifier = mock(ComponentIdentifier.class);
    Map<QName, Object> annotations = new HashMap<>();
    annotations.put(ANNOTATION_NAME, componentIdentifier);
    parameters = emptyMap();
    annotations.put(ANNOTATION_PARAMETERS, parameters);

    muleContext = mock(MuleContext.class);
    when(muleContext.getInjector()).thenReturn(mock(Injector.class));
    processorInterceptorManager = mock(ProcessorInterceptionManager.class);
    when(muleContext.getProcessorInterceptorManager()).thenReturn(processorInterceptorManager);
    intercepted = mock(Processor.class);
    interceptorMessageProcessor.setAnnotations(annotations);
    interceptorMessageProcessor.setIntercepted(intercepted);
    interceptorMessageProcessor.setMuleContext(muleContext);
    interceptorMessageProcessor.initialise();
  }

  @Test
  public void noProcessorSetToBeIntercepted() throws InitialisationException {
    expectedException.expect(InitialisationException.class);
    expectedException.expectMessage("processor has been set");
    new InterceptorMessageProcessor().initialise();
  }

  @Test
  public void initialiseWithoutAnnotations() throws InitialisationException {
    interceptorMessageProcessor = new InterceptorMessageProcessor();
    interceptorMessageProcessor.setIntercepted(mock(Processor.class));
    expectedException.expect(InitialisationException.class);
    expectedException.expectMessage("no annotations");
    interceptorMessageProcessor.initialise();
  }

  @Test
  public void noInterceptorCallbackShouldCallInterceptedProcessor() throws Exception {
    final Event inputEvent = mock(Event.class);
    when(processorInterceptorManager.retrieveInterceptorCallback(componentIdentifier)).thenReturn(empty());
    when(intercepted.process(inputEvent)).thenReturn(inputEvent);
    Event resultEvent = interceptorMessageProcessor.process(inputEvent);

    assertThat(resultEvent, sameInstance(inputEvent));

    verify(intercepted).process(inputEvent);
    verify(muleContext).getProcessorInterceptorManager();
    verify(processorInterceptorManager).retrieveInterceptorCallback(componentIdentifier);
    verify(muleContext).getInjector();
  }

  @Test
  public void interceptorCallbackSkipExecutionOfInterceptedProcessor() throws Exception {
    final Event inputEvent = mock(Event.class);
    ProcessorInterceptorCallback callback = mock(ProcessorInterceptorCallback.class);
    when(callback.shouldInterceptExecution(inputEvent, parameters)).thenReturn(true);
    Event callbackResultEvent = mock(Event.class);
    when(callback.getResult(inputEvent)).thenReturn(callbackResultEvent);
    Optional<ProcessorInterceptorCallback> callbackOptional = of(callback);
    when(processorInterceptorManager.retrieveInterceptorCallback(componentIdentifier)).thenReturn(callbackOptional);
    when(intercepted.process(inputEvent)).thenReturn(inputEvent);
    Event resultEvent = interceptorMessageProcessor.process(inputEvent);

    assertThat(resultEvent, not(sameInstance(inputEvent)));
    assertThat(resultEvent, sameInstance(callbackResultEvent));

    verify(callback).getResult(inputEvent);
    verify(callback).shouldInterceptExecution(inputEvent, parameters);
    verify(muleContext).getProcessorInterceptorManager();
    verify(processorInterceptorManager).retrieveInterceptorCallback(componentIdentifier);
    verify(muleContext).getInjector();
  }

  @Test
  public void interceptorCallbackThrowsExceptionOnBefore() throws Exception {
    final Event inputEvent = mock(Event.class);
    ProcessorInterceptorCallback callback = mock(ProcessorInterceptorCallback.class);
    when(callback.shouldInterceptExecution(inputEvent, parameters)).thenReturn(true);
    doThrow(new UnsupportedOperationException()).when(callback).before(inputEvent, parameters);
    Optional<ProcessorInterceptorCallback> callbackOptional = of(callback);
    when(processorInterceptorManager.retrieveInterceptorCallback(componentIdentifier)).thenReturn(callbackOptional);
    when(intercepted.process(inputEvent)).thenReturn(inputEvent);

    expectedException.expect(MuleRuntimeException.class);
    expectedException.expectCause(instanceOf(UnsupportedOperationException.class));
    interceptorMessageProcessor.process(inputEvent);
  }

  @Test
  public void interceptProcessorWithChain() throws Exception {
    final Event inputEvent = mock(Event.class);
    ProcessorInterceptorCallback callback = mock(ProcessorInterceptorCallback.class);
    when(callback.shouldInterceptExecution(inputEvent, parameters)).thenReturn(true);
    Event callbackResultEvent = mock(Event.class);
    when(callback.getResult(inputEvent)).thenReturn(callbackResultEvent);
    Optional<ProcessorInterceptorCallback> callbackOptional = of(callback);
    when(processorInterceptorManager.retrieveInterceptorCallback(componentIdentifier)).thenReturn(callbackOptional);
    when(intercepted.process(inputEvent)).thenReturn(inputEvent);

    Processor nextProcessor = mock(Processor.class);
    Event nextEvent = mock(Event.class);
    when(nextProcessor.process(callbackResultEvent)).thenReturn(nextEvent);
    interceptorMessageProcessor.setListener(nextProcessor);

    Event resultEvent = interceptorMessageProcessor.process(inputEvent);

    assertThat(resultEvent, not(sameInstance(inputEvent)));
    assertThat(resultEvent, not(sameInstance(callbackResultEvent)));
    assertThat(resultEvent, sameInstance(nextEvent));

    verify(nextProcessor).process(callbackResultEvent);
    verify(callback).getResult(inputEvent);
    verify(callback).shouldInterceptExecution(inputEvent, parameters);
    verify(muleContext).getProcessorInterceptorManager();
    verify(processorInterceptorManager).retrieveInterceptorCallback(componentIdentifier);
    verify(muleContext).getInjector();
  }

}
