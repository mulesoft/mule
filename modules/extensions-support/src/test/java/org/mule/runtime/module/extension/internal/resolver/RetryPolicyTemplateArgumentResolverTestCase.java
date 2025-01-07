/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resolver;

import static java.util.Collections.emptyList;
import static java.util.Optional.of;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.execution.MethodArgumentResolverDelegate;
import org.mule.runtime.module.extension.internal.runtime.resolver.ArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.RetryPolicyTemplateArgumentResolver;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Issue;

public class RetryPolicyTemplateArgumentResolverTestCase extends AbstractMuleTestCase {

  private RetryPolicyTemplateArgumentResolver resolver = new RetryPolicyTemplateArgumentResolver();
  private ExecutionContextAdapter executionContext = mock(ExecutionContextAdapter.class);

  @Before
  public void setUp() {
    resolver = new RetryPolicyTemplateArgumentResolver();
    executionContext = mock(ExecutionContextAdapter.class);

  }

  @Test
  @Issue("W-17553127")
  public void resolveCoreRetryPolicy() {
    final org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate retryPolicyTemplate =
        mock(org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate.class);
    when(executionContext.getRetryPolicyTemplate()).thenReturn(of(retryPolicyTemplate));

    final RetryPolicyTemplate resolved = resolver.resolve(executionContext);

    assertThat(resolved, sameInstance(retryPolicyTemplate));
  }

  @Test
  public void resolveApiRetryPolicy() {
    final RetryPolicyTemplate retryPolicyTemplate = mock(RetryPolicyTemplate.class);
    when(executionContext.getRetryPolicyTemplate()).thenReturn(of(retryPolicyTemplate));

    final RetryPolicyTemplate resolved = resolver.resolve(executionContext);

    assertThat(resolved, sameInstance(retryPolicyTemplate));
  }

  @Test
  @Issue("W-17553127")
  public void resolveCoreRetryPolicyDelegate() throws Exception {
    final MethodArgumentResolverDelegate delegate = new MethodArgumentResolverDelegate(emptyList(), RetryPolicyAware.class
        .getDeclaredMethod("core", org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate.class));

    delegate.initialise();
    final ArgumentResolver<?> resolver = delegate.getArgumentResolvers()[0];

    assertThat(resolver, instanceOf(RetryPolicyTemplateArgumentResolver.class));
  }

  @Test
  public void resolveApiRetryPolicyDelegate() throws Exception {
    final MethodArgumentResolverDelegate delegate = new MethodArgumentResolverDelegate(emptyList(),
                                                                                       RetryPolicyAware.class
                                                                                           .getDeclaredMethod("api",
                                                                                                              RetryPolicyTemplate.class));

    delegate.initialise();
    final ArgumentResolver<?> resolver = delegate.getArgumentResolvers()[0];

    assertThat(resolver, instanceOf(RetryPolicyTemplateArgumentResolver.class));
  }

  public static class RetryPolicyAware {

    public void core(org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate retry) {

    }

    public void api(RetryPolicyTemplate retry) {

    }

  }
}
