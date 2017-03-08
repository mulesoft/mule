/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.config;

import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import org.mule.runtime.api.el.ExpressionExecutor;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.service.http.api.HttpService;
import org.mule.tck.SimpleUnitTestSupportSchedulerService;

import com.mulesoft.weave.el.WeaveExpressionExecutor;

import java.util.List;

/**
 * Registers services instances into the {@link MuleRegistry} of a {@link MuleContext}.
 * <p>
 * This is to be used only in tests that do not leverage the service injection mechanism.
 *
 * @since 4.0
 */
public class TestServicesConfigurationBuilder extends AbstractConfigurationBuilder {

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
    MuleRegistry registry = muleContext.getRegistry();
    registry.registerObject(schedulerService.getName(), spy(schedulerService));

    if (mockExpressionExecutor) {
      ExpressionExecutor expressionExecutor = mock(ExpressionExecutor.class);
      registry.registerObject(MOCK_EXPR_EXECUTOR, expressionExecutor);
    } else {
      final WeaveExpressionExecutor exprExecutor = new WeaveExpressionExecutor();
      registry.registerObject(exprExecutor.getName(), exprExecutor);
    }

    if (mockHttpService) {
      registry.registerObject(MOCK_HTTP_SERVICE, mock(HttpService.class));
    }
  }

  public void stopServices() throws MuleException {
    final List<Scheduler> schedulers = schedulerService.getSchedulers();
    try {
      assertThat(schedulers, empty());
    } finally {
      schedulerService.stop();
    }
  }

}
