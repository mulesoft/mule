/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.internal.policy.PolicyPointcutParametersManager.POLICY_SOURCE_POINTCUT_PARAMETERS;
import static org.mule.tck.junit4.matcher.IsEmptyOptional.empty;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.policy.api.OperationPolicyPointcutParametersFactory;
import org.mule.runtime.policy.api.PolicyPointcutParameters;
import org.mule.runtime.policy.api.SourcePolicyPointcutParametersFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PolicyPointcutParametersManagerTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = none();

  private PolicyPointcutParametersManager parametersManager;

  private Collection<SourcePolicyPointcutParametersFactory> sourcePointcutFactories;
  private Collection<OperationPolicyPointcutParametersFactory> operationPointcutFactories;

  private BaseEventContext eventContext;
  private ComponentIdentifier identifier;
  private Component component;
  private InternalEvent event;

  @Before
  public void setUp() {
    sourcePointcutFactories = new ArrayList<>();
    operationPointcutFactories = new ArrayList<>();

    mockComponent();
    mockEvent();

    parametersManager = new PolicyPointcutParametersManager(sourcePointcutFactories, operationPointcutFactories);
  }

  @Test
  public void createSourceParametersWhenEmptyFactory() {

    PolicyPointcutParameters parameters =
        parametersManager.createSourcePointcutParameters(component, event.getMessage().getAttributes());
    Map singletonMap = singletonMap(POLICY_SOURCE_POINTCUT_PARAMETERS, parameters);
    when(event.getInternalParameters()).thenReturn(singletonMap);

    assertThat(parameters.getComponent(), is(component));
    assertThat(parameters.getSourceParameters(), empty());
  }

  @Test
  public void createSourceParametersWhenOneFactorySupportsIdentifier() {
    SourcePolicyPointcutParametersFactory factory = mockSourceFactory(true);
    sourcePointcutFactories.add(factory);

    PolicyPointcutParameters parameters =
        parametersManager.createSourcePointcutParameters(component, event.getMessage().getAttributes());
    Map singletonMap = singletonMap(POLICY_SOURCE_POINTCUT_PARAMETERS, parameters);
    when(event.getInternalParameters()).thenReturn(singletonMap);

    assertThat(parameters.getComponent(), is(component));
    assertThat(parameters.getSourceParameters(), empty());
    verify(factory).supportsSourceIdentifier(identifier);
    verify(factory).createPolicyPointcutParameters(component, event.getMessage().getAttributes());
  }

  @Test
  public void createSourceParametersWhenOneFactoryDoesNotSupportsIdentifier() {
    SourcePolicyPointcutParametersFactory factory = mockSourceFactory(false);
    sourcePointcutFactories.add(factory);

    PolicyPointcutParameters parameters =
        parametersManager.createSourcePointcutParameters(component, event.getMessage().getAttributes());
    Map singletonMap = singletonMap(POLICY_SOURCE_POINTCUT_PARAMETERS, parameters);
    when(event.getInternalParameters()).thenReturn(singletonMap);

    assertThat(parameters.getComponent(), is(component));
    assertThat(parameters.getSourceParameters(), empty());
    verify(factory).supportsSourceIdentifier(identifier);
    verify(factory, never()).createPolicyPointcutParameters(component, event.getMessage().getAttributes());
  }

  @Test
  public void createSourceParametersWhenOneFactorySupportsIdentifierAndOneNot() {
    SourcePolicyPointcutParametersFactory factory1 = mockSourceFactory(true);
    SourcePolicyPointcutParametersFactory factory2 = mockSourceFactory(false);
    sourcePointcutFactories.add(factory1);
    sourcePointcutFactories.add(factory2);

    PolicyPointcutParameters parameters =
        parametersManager.createSourcePointcutParameters(component, event.getMessage().getAttributes());
    Map singletonMap = singletonMap(POLICY_SOURCE_POINTCUT_PARAMETERS, parameters);
    when(event.getInternalParameters()).thenReturn(singletonMap);

    assertThat(parameters.getComponent(), is(component));
    assertThat(parameters.getSourceParameters(), empty());
    verify(factory1).supportsSourceIdentifier(identifier);
    verify(factory1).createPolicyPointcutParameters(component, event.getMessage().getAttributes());
    verify(factory2).supportsSourceIdentifier(identifier);
    verify(factory2, never()).createPolicyPointcutParameters(component, event.getMessage().getAttributes());
  }

  @Test
  public void throwExceptionWhenMoreThanOneSourceFactorySupportsIdentifier() {
    sourcePointcutFactories.add(mockSourceFactory(true));
    sourcePointcutFactories.add(mockSourceFactory(true));
    expectedException.expect(MuleRuntimeException.class);

    parametersManager.createSourcePointcutParameters(component, event.getMessage().getAttributes());
  }

  @Test
  public void createOperationParametersWhenEmptyFactory() {
    Map<String, Object> operationParameters = new HashMap<>();
    sourcePointcutFactories.add(mockSourceFactory(true));
    PolicyPointcutParameters sourceParameters =
        parametersManager.createSourcePointcutParameters(component, event.getMessage().getAttributes());
    Map singletonMap = singletonMap(POLICY_SOURCE_POINTCUT_PARAMETERS, sourceParameters);
    when(event.getInternalParameters()).thenReturn(singletonMap);

    PolicyPointcutParameters parameters =
        parametersManager.createOperationPointcutParameters(component, event, operationParameters);

    assertThat(parameters.getComponent(), is(component));
    assertThat(parameters.getSourceParameters(), is(of(sourceParameters)));
  }

  @Test
  public void createOperationParametersWhenEmptyFactoryAndEmptySourceParameters() {
    Map<String, Object> operationParameters = new HashMap<>();

    when(event.getInternalParameters()).thenReturn(emptyMap());
    PolicyPointcutParameters parameters =
        parametersManager.createOperationPointcutParameters(component, event, operationParameters);

    assertThat(parameters.getComponent(), is(component));
    assertThat(parameters.getSourceParameters(), empty());
  }

  @Test
  public void createOperationParametersWhenOneFactorySupportsIdentifier() {
    Map<String, Object> operationParameters = new HashMap<>();
    PolicyPointcutParameters sourceParameters =
        parametersManager.createSourcePointcutParameters(component, event.getMessage().getAttributes());
    Map singletonMap = singletonMap(POLICY_SOURCE_POINTCUT_PARAMETERS, sourceParameters);
    when(event.getInternalParameters()).thenReturn(singletonMap);

    OperationPolicyPointcutParametersFactory factory = mockOperationFactory(true, sourceParameters);
    operationPointcutFactories.add(factory);
    sourcePointcutFactories.add(mockSourceFactory(true));

    PolicyPointcutParameters parameters =
        parametersManager.createOperationPointcutParameters(component, event, operationParameters);

    assertThat(parameters.getComponent(), is(component));
    assertThat(parameters.getSourceParameters(), is(of(sourceParameters)));
    verify(factory).supportsOperationIdentifier(identifier);
    verify(factory).createPolicyPointcutParameters(any(), any(), any());
  }

  @Test
  public void createOperationParametersWhenOneFactorySupportsIdentifierMultipleTimes() {
    Map<String, Object> operationParameters = new HashMap<>();
    PolicyPointcutParameters sourceParameters =
        parametersManager.createSourcePointcutParameters(component, event.getMessage().getAttributes());
    Map singletonMap = singletonMap(POLICY_SOURCE_POINTCUT_PARAMETERS, sourceParameters);
    when(event.getInternalParameters()).thenReturn(singletonMap);

    OperationPolicyPointcutParametersFactory factory = mockOperationFactory(true, sourceParameters);
    operationPointcutFactories.add(factory);
    sourcePointcutFactories.add(mockSourceFactory(true));
    parametersManager.createOperationPointcutParameters(component, event, operationParameters);

    PolicyPointcutParameters parameters =
        parametersManager.createOperationPointcutParameters(component, event, operationParameters);

    assertThat(parameters.getComponent(), is(component));
    assertThat(parameters.getSourceParameters(), is(of(sourceParameters)));
    verify(factory, times(2)).supportsOperationIdentifier(identifier);
    verify(factory, times(2)).createPolicyPointcutParameters(any(), any(), any());
  }

  @Test
  public void createOperationParametersWhenOneFactoryDoesNotSupportsIdentifier() {
    Map<String, Object> operationParameters = new HashMap<>();
    PolicyPointcutParameters sourceParameters =
        parametersManager.createSourcePointcutParameters(component, event.getMessage().getAttributes());
    Map singletonMap = singletonMap(POLICY_SOURCE_POINTCUT_PARAMETERS, sourceParameters);
    when(event.getInternalParameters()).thenReturn(singletonMap);

    OperationPolicyPointcutParametersFactory factory = mockOperationFactory(false, sourceParameters);
    operationPointcutFactories.add(factory);
    sourcePointcutFactories.add(mockSourceFactory(true));

    PolicyPointcutParameters parameters =
        parametersManager.createOperationPointcutParameters(component, event, operationParameters);

    assertThat(parameters.getComponent(), is(component));
    assertThat(parameters.getSourceParameters(), is(of(sourceParameters)));
    verify(factory).supportsOperationIdentifier(identifier);
    verify(factory, never()).createPolicyPointcutParameters(any(), any(), any());
  }

  @Test
  public void createOperationParametersWhenOneFactorySupportsIdentifierAndOneNot() {
    Map<String, Object> operationParameters = new HashMap<>();
    PolicyPointcutParameters sourceParameters =
        parametersManager.createSourcePointcutParameters(component, event.getMessage().getAttributes());
    Map singletonMap = singletonMap(POLICY_SOURCE_POINTCUT_PARAMETERS, sourceParameters);
    when(event.getInternalParameters()).thenReturn(singletonMap);

    OperationPolicyPointcutParametersFactory factory1 = mockOperationFactory(true, sourceParameters);
    OperationPolicyPointcutParametersFactory factory2 = mockOperationFactory(false, sourceParameters);
    operationPointcutFactories.add(factory1);
    operationPointcutFactories.add(factory2);
    sourcePointcutFactories.add(mockSourceFactory(true));

    PolicyPointcutParameters parameters =
        parametersManager.createOperationPointcutParameters(component, event, operationParameters);

    assertThat(parameters.getComponent(), is(component));
    assertThat(parameters.getSourceParameters(), is(of(sourceParameters)));
    verify(factory1).supportsOperationIdentifier(identifier);
    verify(factory1).createPolicyPointcutParameters(any(), any(), any());
    verify(factory2).supportsOperationIdentifier(identifier);
    verify(factory2, never()).createPolicyPointcutParameters(any(), any(), any());
  }

  @Test
  public void throwExceptionWhenMoreThanOneOperationFactorySupportsIdentifier() {
    operationPointcutFactories.add(mockOperationFactory(true, null));
    operationPointcutFactories.add(mockOperationFactory(true, null));
    expectedException.expect(MuleRuntimeException.class);

    when(event.getInternalParameters()).thenReturn(emptyMap());
    parametersManager.createOperationPointcutParameters(component, event, new HashMap<>());
  }

  @Test
  public void createOperationParametersFallbacksToDeprecatedMethod() {
    Map<String, Object> operationParameters = new HashMap<>();
    PolicyPointcutParameters sourceParameters =
        parametersManager.createSourcePointcutParameters(component, event.getMessage().getAttributes());
    Map singletonMap = singletonMap(POLICY_SOURCE_POINTCUT_PARAMETERS, sourceParameters);
    when(event.getInternalParameters()).thenReturn(singletonMap);

    OperationPolicyPointcutParametersFactory factory = mockOperationFactory(true, sourceParameters);
    PolicyPointcutParameters parameters = mock(PolicyPointcutParameters.class);
    when(factory.createPolicyPointcutParameters(any(), any(), any())).thenThrow(new AbstractMethodError());
    when(factory.createPolicyPointcutParameters(component, operationParameters)).thenReturn(parameters);
    operationPointcutFactories.add(factory);

    PolicyPointcutParameters returnedParameters =
        parametersManager.createOperationPointcutParameters(component, event, operationParameters);

    assertThat(returnedParameters, is(parameters));
    verify(factory).supportsOperationIdentifier(identifier);
    verify(factory).createPolicyPointcutParameters(any(), any(), any());
    verify(factory).createPolicyPointcutParameters(component, operationParameters);
  }

  private void mockEvent() {
    event = mock(InternalEvent.class, RETURNS_DEEP_STUBS);
    eventContext = mock(BaseEventContext.class, RETURNS_DEEP_STUBS);
    when(event.getContext()).thenReturn(eventContext);
    when(eventContext.getRootContext()).thenReturn(eventContext);
    when(eventContext.getCorrelationId()).thenReturn("anId");
  }

  private void mockComponent() {
    component = mock(Component.class, RETURNS_DEEP_STUBS);
    identifier = mock(ComponentIdentifier.class);
    when(component.getLocation().getComponentIdentifier().getIdentifier()).thenReturn(identifier);
  }

  private SourcePolicyPointcutParametersFactory mockSourceFactory(boolean supportsIdentifier) {
    SourcePolicyPointcutParametersFactory factory = mock(SourcePolicyPointcutParametersFactory.class);
    when(factory.supportsSourceIdentifier(identifier)).thenReturn(supportsIdentifier);
    PolicyPointcutParameters parameters = new PolicyPointcutParameters(component);
    when(factory.createPolicyPointcutParameters(component, event.getMessage().getAttributes())).thenReturn(parameters);
    return factory;
  }

  private OperationPolicyPointcutParametersFactory mockOperationFactory(boolean supportsIdentifier,
                                                                        PolicyPointcutParameters sourceParameters) {
    OperationPolicyPointcutParametersFactory factory = mock(OperationPolicyPointcutParametersFactory.class);
    when(factory.supportsOperationIdentifier(identifier)).thenReturn(supportsIdentifier);
    PolicyPointcutParameters parameters = new PolicyPointcutParameters(component, sourceParameters);
    when(factory.createPolicyPointcutParameters(any(), any(), any())).thenReturn(parameters);
    return factory;
  }

}
