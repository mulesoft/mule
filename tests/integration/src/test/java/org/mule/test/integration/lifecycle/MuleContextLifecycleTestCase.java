/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.lifecycle;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LifecyclePhaseFailureStory.LIFECYCLE_PHASE_FAILURE_STORY;
import org.mule.functional.junit4.ApplicationContextBuilder;
import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.service.http.api.HttpService;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.List;
import java.util.function.Consumer;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Stories(LIFECYCLE_PHASE_FAILURE_STORY)
public class MuleContextLifecycleTestCase extends AbstractMuleTestCase {

  private static final String EXPECTED_A_CONTEXT_START_EXCEPTION_EXCEPTION = "Expected a ContextStartException exception";

  @Test
  public void failOnStartInvokesStopInOtherComponentsButNotInTheFailedOne() {
    testOnContextLifecycleFailure("org/mule/test/integration/lifecycle/component-failing-during-startup-config.xml",
                                  muleContext -> {
                                    LifecycleBean lifecycleBean = muleContext.getRegistry().get("lifecycleBean");
                                    LifecycleBean failOnStartLifecycleBean =
                                        muleContext.getRegistry().get("failOnStartLifecycleBean");
                                    muleContext.dispose();
                                    assertThat(lifecycleBean.getLifecycleInvocations(),
                                               contains(Initialisable.PHASE_NAME, Startable.PHASE_NAME, Stoppable.PHASE_NAME,
                                                        Disposable.PHASE_NAME));
                                    assertThat(failOnStartLifecycleBean.getLifecycleInvocations(),
                                               contains(Initialisable.PHASE_NAME, Startable.PHASE_NAME, Disposable.PHASE_NAME));
                                  });
  }

  @Test
  public void failOnInitialiseInvokesDisposeInOtherComponentsButNotInTheFailedOne() {
    testOnContextLifecycleFailure("org/mule/test/integration/lifecycle/component-failing-during-initialise-config.xml",
                                  muleContext -> {
                                    LifecycleBean lifecycleBean = muleContext.getRegistry().get("lifecycleBean");
                                    LifecycleBean failOnStartLifecycleBean =
                                        muleContext.getRegistry().get("failOnInitialiseLifecycleBean");
                                    muleContext.dispose();
                                    assertThat(lifecycleBean.getLifecycleInvocations(),
                                               contains(Initialisable.PHASE_NAME, Disposable.PHASE_NAME));
                                    assertThat(failOnStartLifecycleBean.getLifecycleInvocations(),
                                               contains(Initialisable.PHASE_NAME));
                                  });
  }

  private void testOnContextLifecycleFailure(String configFile, Consumer<MuleContext> muleContextConsumer) {
    try {
      new ApplicationContextBuilder() {

        @Override
        protected final void addBuilders(List<ConfigurationBuilder> builders) {
          builders.add(new AbstractConfigurationBuilder() {

            @Override
            protected void doConfigure(MuleContext muleContext) throws Exception {
              muleContext.getRegistry().registerObject("httpService", mock(HttpService.class, RETURNS_DEEP_STUBS.get()));
              muleContext.getRegistry().registerObject("schedulerService",
                                                       mock(SchedulerService.class, RETURNS_DEEP_STUBS.get()));
              muleContext.getRegistry().registerObject("elService", new DefaultExpressionLanguageFactoryService() {

                @Override
                public ExpressionLanguage create() {
                  return mock(ExpressionLanguage.class, RETURNS_DEEP_STUBS.get());
                }

                @Override
                public String getName() {
                  return "test-el";
                }
              });
            }
          });
        }
      }.setApplicationResources(new String[] {
          configFile
      }).build();
      fail(EXPECTED_A_CONTEXT_START_EXCEPTION_EXCEPTION);
    } catch (LifecycleException e) {
      LifecycleBean lifecycleBean = (LifecycleBean) e.getComponent();
      muleContextConsumer.accept(lifecycleBean.getMuleContext());
    } catch (Exception e) {
      fail(String.format("Expected a %s exception", LifecycleException.class.getName()));
    }
  }

}
