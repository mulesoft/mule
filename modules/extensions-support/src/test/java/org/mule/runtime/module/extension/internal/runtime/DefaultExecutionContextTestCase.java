/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.component.location.ComponentLocation;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultExecutionContextTestCase extends AbstractMuleTestCase {

  private static final String CONFIG_NAME = "config";
  private static final String PARAM_NAME = "param1";
  private static final String VALUE = "Do you want to build a snowman?";

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
  private ComponentLocation location;

  @Mock
  private StreamingManager streamingManager;

  @Mock
  private ConfigurationState configurationState;

  @Mock
  private RetryPolicyTemplate retryPolicyTemplate;

  private Object configurationInstance = new Object();
  private ConfigurationInstance configuration;
  private DefaultExecutionContext<OperationModel> operationContext;


  @Before
  public void before() {
    configuration =
        new LifecycleAwareConfigurationInstance(CONFIG_NAME,
                                                configurationModel,
                                                configurationInstance,
                                                configurationState,
                                                emptyList(),
                                                empty());
    Map<String, Object> parametersMap = new HashMap<>();
    parametersMap.put(PARAM_NAME, VALUE);
    when(resolverSetResult.asMap()).thenReturn(parametersMap);

    operationContext =
        new DefaultExecutionContext<>(extensionModel, of(configuration), resolverSetResult.asMap(), operationModel,
                                      event, cursorProviderFactory, streamingManager, location, retryPolicyTemplate,
                                      muleContext);
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

  @Test(expected = IllegalArgumentException.class)
  public void setNullKeyVariable() {
    operationContext.setVariable(null, "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void setNullValueVariable() {
    operationContext.setVariable("key", null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void removeNullValueVariable() {
    operationContext.removeVariable(null);
  }

  @Test
  public void getRetryPolicyTemplate() {
    assertThat(operationContext.getRetryPolicyTemplate().get(), is(sameInstance(retryPolicyTemplate)));
  }
}
