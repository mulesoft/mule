/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.config;

import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mule.runtime.api.scheduler.SchedulerConfig.config;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SCHEDULER_BASE_CONFIG;

import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.SchedulerView;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.http.api.HttpService;
import org.mule.tck.SimpleUnitTestSupportSchedulerService;
import org.mule.weave.v2.el.WeaveDefaultExpressionLanguageFactoryService;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.List;

/**
 * Registers services instances into the {@link MuleRegistry} of a {@link MuleContext}.
 * <p>
 * This is to be used only in tests that do not leverage the service injection mechanism.
 *
 * @since 4.0
 */
public class TestServicesConfigurationBuilder extends AbstractConfigurationBuilder implements TestRule {

  private static final String MOCK_HTTP_SERVICE = "mockHttpService";
  private static final String MOCK_EXPR_EXECUTOR = "mockExpressionExecutor";

  private final SimpleUnitTestSupportSchedulerService schedulerService = new SimpleUnitTestSupportSchedulerService();
  private boolean mockHttpService;
  private boolean mockExpressionExecutor;

  public TestServicesConfigurationBuilder() {
    this(true, true);
  }

  public TestServicesConfigurationBuilder(boolean mockHttpService, boolean mockExpressionExecutor) {
    this.mockHttpService = mockHttpService;
    this.mockExpressionExecutor = mockExpressionExecutor;
  }

  @Override
  public void doConfigure(MuleContext muleContext) throws Exception {
    MuleRegistry registry = ((MuleContextWithRegistries) muleContext).getRegistry();
    registry.registerObject(schedulerService.getName(), spy(schedulerService));
    registry.registerObject(OBJECT_SCHEDULER_BASE_CONFIG, config());

    if (mockExpressionExecutor) {
      DefaultExpressionLanguageFactoryService expressionExecutor =
          mock(DefaultExpressionLanguageFactoryService.class, RETURNS_DEEP_STUBS);
      registry.registerObject(MOCK_EXPR_EXECUTOR, expressionExecutor);
    } else {
      final DefaultExpressionLanguageFactoryService exprExecutor = new WeaveDefaultExpressionLanguageFactoryService();
      registry.registerObject(exprExecutor.getName(), exprExecutor);
    }

    if (mockHttpService) {
      registry.registerObject(MOCK_HTTP_SERVICE, mock(HttpService.class));
    }
  }

  public void stopServices() throws MuleException {
    final List<SchedulerView> schedulers = schedulerService.getSchedulers();
    try {
      assertThat(schedulers, empty());
    } finally {
      schedulerService.stop();
    }
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        try {
          base.evaluate();
        } finally {
          stopServices();
        }
      }
    };
  }

}
