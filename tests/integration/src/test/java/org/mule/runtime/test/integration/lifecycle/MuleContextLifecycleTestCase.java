/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.test.integration.lifecycle;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LifecyclePhaseFailureStory.LIFECYCLE_PHASE_FAILURE_STORY;

import org.mule.functional.api.component.LifecycleObject;
import org.mule.functional.junit4.ApplicationContextBuilder;
import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.SimpleConfigurationBuilder;
import org.mule.runtime.http.api.HttpService;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Story(LIFECYCLE_PHASE_FAILURE_STORY)
public class MuleContextLifecycleTestCase extends AbstractMuleTestCase {

  private static final String EXPECTED_A_CONTEXT_START_EXCEPTION_EXCEPTION = "Expected a ContextStartException exception";

  @Test
  public void failOnStartInvokesStopInOtherComponentsButNotInTheFailedOne() {
    testOnContextLifecycleFailure("lifecycle/component-failing-during-startup-config.xml",
                                  (failOnStartLifecycleBean) -> {
                                    LifecycleObject lifecycleBean = failOnStartLifecycleBean.getOtherLifecycleObject();
                                    lifecycleBean.getMuleContext().dispose();
                                    assertThat(lifecycleBean.getLifecycleInvocations(),
                                               contains(Initialisable.PHASE_NAME, Startable.PHASE_NAME, Stoppable.PHASE_NAME,
                                                        Disposable.PHASE_NAME));
                                    assertThat(failOnStartLifecycleBean.getLifecycleInvocations(),
                                               contains(Initialisable.PHASE_NAME, Startable.PHASE_NAME, Disposable.PHASE_NAME));
                                  });
  }

  @Test
  public void failOnInitialiseInvokesDisposeInOtherComponentsButNotInTheFailedOne() {
    testOnContextLifecycleFailure("lifecycle/component-failing-during-initialise-config.xml",
                                  (failOnStartLifecycleBean) -> {
                                    LifecycleObject lifecycleBean = failOnStartLifecycleBean.getOtherLifecycleObject();
                                    assertThat(lifecycleBean.getLifecycleInvocations(),
                                               contains(Initialisable.PHASE_NAME, Disposable.PHASE_NAME));
                                    assertThat(failOnStartLifecycleBean.getLifecycleInvocations(),
                                               contains(Initialisable.PHASE_NAME));
                                  });
  }

  private void testOnContextLifecycleFailure(String configFile, Consumer<LifecycleObject> failureLifecycleBeanConsumer) {
    try {
      new ApplicationContextBuilder() {

        @Override
        protected final void addBuilders(List<ConfigurationBuilder> builders) {
          Map<String, Object> baseRegistry = new HashMap<>();
          baseRegistry.put("httpService", mock(HttpService.class, RETURNS_DEEP_STUBS.get()));
          baseRegistry.put("schedulerService", mock(SchedulerService.class, RETURNS_DEEP_STUBS.get()));
          baseRegistry.put("elService", new DefaultExpressionLanguageFactoryService() {

            @Override
            public ExpressionLanguage create() {
              return mock(ExpressionLanguage.class, RETURNS_DEEP_STUBS.get());
            }

            @Override
            public String getName() {
              return "test-el";
            }
          });
          builders.add(new SimpleConfigurationBuilder(baseRegistry));
        }
      }.setApplicationResources(new String[] {
          configFile
      }).build();
      fail(EXPECTED_A_CONTEXT_START_EXCEPTION_EXCEPTION);
    } catch (LifecycleException e) {
      LifecycleObject lifecycleBean = (LifecycleObject) e.getComponent();
      failureLifecycleBeanConsumer.accept(lifecycleBean);
    } catch (Exception e) {
      fail(String.format("Expected a %s exception", LifecycleException.class.getName()));
    }
  }

}
