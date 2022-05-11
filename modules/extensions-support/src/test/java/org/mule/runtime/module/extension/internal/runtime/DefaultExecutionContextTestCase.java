/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.internal.util.rx.ImmediateScheduler.IMMEDIATE_SCHEDULER;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationState;
import org.mule.runtime.module.extension.internal.runtime.config.LifecycleAwareConfigurationInstance;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@SmallTest
public class DefaultExecutionContextTestCase extends AbstractMuleTestCase {

  private static final String CONFIG_NAME = "config";
  private static final String PARAM_NAME = "param1";
  private static final String VALUE = "Do you want to build a snowman?";

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock
  private ExtensionModel extensionModel;

  @Mock
  private ConfigurationModel configurationModel;

  @Mock
  private OperationModel operationModel;

  @Mock
  private ResolverSetResult resolverSetResult;

  @Mock
  private CoreEvent event;

  @Mock
  private MuleContext muleContext;

  @Mock
  private ExtensionManager extensionManager;

  @Mock
  private CursorProviderFactory<Object> cursorProviderFactory;

  @Mock
  private Component component;

  @Mock
  private StreamingManager streamingManager;

  @Mock
  private ConfigurationState configurationState;

  @Mock
  private RetryPolicyTemplate retryPolicyTemplate;

  private final Object configurationInstance = new Object();
  private ConfigurationInstance configuration;
  private DefaultExecutionContext<OperationModel> operationContext;


  @Before
  public void before() {
    configuration =
        new LifecycleAwareConfigurationInstance(CONFIG_NAME,
                                                configurationModel,
                                                configurationInstance,
                                                configurationState,
                                                empty());

    Map<String, Object> parametersMap = new HashMap<>();
    parametersMap.put(PARAM_NAME, VALUE);
    when(resolverSetResult.asMap()).thenReturn(parametersMap);

    operationContext =
        new DefaultExecutionContext<>(extensionModel, of(configuration), resolverSetResult.asMap(), operationModel,
                                      event, cursorProviderFactory, streamingManager, component,
                                      retryPolicyTemplate, IMMEDIATE_SCHEDULER, empty(), muleContext);
  }

  @Test
  public void getParameter() {
    assertThat(operationContext.hasParameter(PARAM_NAME), is(true));
    assertThat(operationContext.getParameter(PARAM_NAME), is(VALUE));
  }

  @Test(expected = NoSuchElementException.class)
  public void getNonExistentParameter() {
    assertThat(operationContext.hasParameter(PARAM_NAME + "_"), is(false));
    operationContext.getParameter(PARAM_NAME + "_");
  }

  @Test
  public void variables() {
    final String key = "foo";
    final String value = "bar";

    assertThat(operationContext.getVariable(key), is(nullValue()));
    assertThat(operationContext.setVariable(key, value), is(nullValue()));
    assertThat(operationContext.getVariable(key), is(value));
    assertThat(operationContext.setVariable(key, EMPTY), is(value));
    assertThat(operationContext.getVariable(key), is(EMPTY));
    assertThat(operationContext.removeVariable(key), is(EMPTY));
    assertThat(operationContext.getVariable(key), is(nullValue()));
  }

  @Test(expected = NullPointerException.class)
  public void setNullKeyVariable() {
    operationContext.setVariable(null, "");
  }

  @Test(expected = NullPointerException.class)
  public void setNullValueVariable() {
    operationContext.setVariable("key", null);
  }

  @Test(expected = NullPointerException.class)
  public void removeNullValueVariable() {
    operationContext.removeVariable(null);
  }

  @Test
  public void getRetryPolicyTemplate() {
    assertThat(operationContext.getRetryPolicyTemplate().get(), is(sameInstance(retryPolicyTemplate)));
  }
}
