/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lifecycle;

import static java.util.Optional.of;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LifecyclePhaseStory.LIFECYCLE_PHASE_STORY;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.internal.lifecycle.phases.LifecyclePhase;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;

@Feature(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Story(LIFECYCLE_PHASE_STORY)
public class DefaultLifecycleInterceptorTestCase {

  public DefaultLifecycleInterceptor interceptor =
      new DefaultLifecycleInterceptor(Startable.PHASE_NAME, Stoppable.PHASE_NAME, Startable.class);

  private Object startableAndStoppableObject;
  private Object stoppableObject;
  private LifecyclePhase startLifecyclePhase;
  private LifecyclePhase stopLifecyclePhase;

  @Before
  public void before() {
    startableAndStoppableObject =
        mock(Object.class, withSettings().extraInterfaces(Startable.class, Stoppable.class));
    stoppableObject = mock(Object.class, withSettings().extraInterfaces(Stoppable.class));
    startLifecyclePhase = mock(LifecyclePhase.class);
    stopLifecyclePhase = mock(LifecyclePhase.class);
    when(startLifecyclePhase.getName()).thenReturn(Startable.PHASE_NAME);
    when(stopLifecyclePhase.getName()).thenReturn(Stoppable.PHASE_NAME);
  }

  @Test
  public void applyStart() {
    assertThat(interceptor.beforePhaseExecution(startLifecyclePhase, startableAndStoppableObject), is(true));
  }

  @Test
  public void stopIsNotAppliedIfStartWasNotCalled() {
    assertThat(interceptor.beforePhaseExecution(stopLifecyclePhase, startableAndStoppableObject), is(false));
  }

  @Test
  public void stopIsAppliedIfObjectIsNotStartable() {
    assertThat(interceptor.beforePhaseExecution(startLifecyclePhase, stoppableObject), is(true));
  }

  @Test
  public void stopIsNotInvokedIfStartFailed() {
    assertThat(interceptor.beforePhaseExecution(startLifecyclePhase, startableAndStoppableObject), is(true));
    interceptor.afterPhaseExecution(startLifecyclePhase, startableAndStoppableObject, of(new RuntimeException()));
    assertThat(interceptor.beforePhaseExecution(stopLifecyclePhase, startableAndStoppableObject), is(false));
  }

}
