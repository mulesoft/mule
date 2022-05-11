/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mule.test.allure.AllureConstants.ReconnectionPolicyFeature.RECONNECTION_POLICIES;
import static org.mule.test.allure.AllureConstants.ReconnectionPolicyFeature.RetryTemplateStory.RETRY_TEMPLATE;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.internal.runtime.operation.retry.ComponentRetryPolicyTemplateResolver;
import org.mule.tck.junit4.AbstractMuleTestCase;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(RECONNECTION_POLICIES)
@Story(RETRY_TEMPLATE)
public class ComponentRetryPolicyTemplateResolverTestCase extends AbstractMuleTestCase {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock
  RetryPolicyTemplate retryPolicyTemplate;

  @Mock
  ConnectionManagerAdapter connectionManager;

  @Mock
  ConfigurationInstance configWithConnection;

  @Mock
  ConfigurationInstance configWithoutConnection;

  @Mock
  ConnectionProvider connectionProvider;

  @Mock
  RetryPolicyTemplate configRetryTemplate;

  @Before
  public void before() {
    when(configWithConnection.getConnectionProvider()).thenReturn(of(connectionProvider));
    when(configWithoutConnection.getConnectionProvider()).thenReturn(empty());
    when(connectionManager.getRetryTemplateFor(connectionProvider)).thenReturn(configRetryTemplate);
  }

  @Test
  @Description("The retry policy template set for a component is fetched if available.")
  public void whenRetryPolicyTemplateAvailableForComponentRetryPolicyTemplateIsFetched() {
    ComponentRetryPolicyTemplateResolver resolver =
        new ComponentRetryPolicyTemplateResolver(retryPolicyTemplate, connectionManager);

    assertThat(resolver.fetchRetryPolicyTemplate(of(configWithConnection)), sameInstance(retryPolicyTemplate));
  }

  @Test
  @Description("If the retry policy template is not available, retry policy template from connection provider is fetched.")
  public void whenRetryPolicyTemplateNotAvaialbleForComponentTheOneFromConnectionIsFetched() {
    ComponentRetryPolicyTemplateResolver resolver =
        new ComponentRetryPolicyTemplateResolver(null, connectionManager);

    assertThat(resolver.fetchRetryPolicyTemplate(of(configWithConnection)), sameInstance(configRetryTemplate));
  }

  @Test
  @Description("If retry policy template not available, and no connection provider in config, return fallback retry policy template.")
  public void whenRetryPolicyTemplateNotAvaialbleForComponentAndNoConnectionConfigThenFallback() {
    ComponentRetryPolicyTemplateResolver resolver =
        new ComponentRetryPolicyTemplateResolver(null, connectionManager);

    assertThat(resolver.fetchRetryPolicyTemplate(of(configWithoutConnection)),
               CoreMatchers.instanceOf(NoRetryPolicyTemplate.class));
  }

}
