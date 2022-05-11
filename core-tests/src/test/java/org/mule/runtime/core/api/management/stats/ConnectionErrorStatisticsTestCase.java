/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.management.stats;


import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.test.allure.AllureConstants.ReconnectionPolicyFeature.RECONNECTION_POLICIES;
import static org.mule.test.allure.AllureConstants.ReconnectionPolicyFeature.RetryTemplateStory.RETRY_TEMPLATE;

import java.util.concurrent.Executor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.management.stats.AllStatistics;
import org.mule.runtime.core.api.retry.RetryCallback;
import org.mule.runtime.core.api.retry.RetryContext;
import org.mule.runtime.core.api.retry.policy.AbstractPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.PolicyStatus;
import org.mule.runtime.core.api.retry.policy.RetryPolicy;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(RECONNECTION_POLICIES)
@Story(RETRY_TEMPLATE)
public class ConnectionErrorStatisticsTestCase {

  private static final Exception FAILURE_TO_CONNECT_EXCEPTION = new Exception("Failure to connect");
  private MuleContext muleContext;

  @Before
  public void setUp() throws Exception {
    muleContext = mock(MuleContext.class);
    AllStatistics allStatistics = new AllStatistics();
    allStatistics.setEnabled(true);
    when(muleContext.getStatistics()).thenReturn(allStatistics);
  }

  @Test
  public void connectionErrorsAreComputedAsExecutionErrors() throws Exception {
    RetryPolicyTemplate retryPolicyTemplate = addConnectorWithRetryPolicy();
    RetryCallback callback = mock(RetryCallback.class);
    Executor executor = mock(Executor.class);
    doThrow(FAILURE_TO_CONNECT_EXCEPTION).when(callback).doWork(any(RetryContext.class));

    try {
      retryPolicyTemplate.execute(callback, executor);
    } catch (RetryPolicyExhaustedException e) {
      // Nothing to do
    }

    assertThat(muleContext.getStatistics().getApplicationStatistics().getConnectionErrors(), equalTo(1l));
    assertThat(muleContext.getStatistics().getApplicationStatistics().getExecutionErrors(), equalTo(1l));
  }

  private RetryPolicyTemplate addConnectorWithRetryPolicy() {
    RetryPolicy retryPolicy = mock(RetryPolicy.class);
    PolicyStatus policyStatus = PolicyStatus.policyExhausted(FAILURE_TO_CONNECT_EXCEPTION);
    TestRetryPolicyTemplate retryPolicyTemplate = new TestRetryPolicyTemplate(retryPolicy, muleContext);
    when(retryPolicy.applyPolicy(Mockito.any(Throwable.class))).thenReturn(policyStatus);

    return retryPolicyTemplate;
  }

  private static class TestRetryPolicyTemplate extends AbstractPolicyTemplate {

    private RetryPolicy retryPolicy;

    public TestRetryPolicyTemplate(RetryPolicy retryPolicy, MuleContext muleContext) {
      this.retryPolicy = retryPolicy;
      this.muleContext = muleContext;
      this.setNotifier(null);
    }

    @Override
    public RetryPolicy createRetryInstance() {
      return retryPolicy;
    }

    @Override
    protected boolean computeConnectionErrorsInStats() {
      return true;
    }
  }

}
