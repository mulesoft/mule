/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.source.scheduler;

import static java.lang.Boolean.valueOf;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.core.internal.source.scheduler.DefaultSchedulerMessageSource.setDisabled;
import static org.mule.test.allure.AllureConstants.SchedulerFeature.SCHEDULER;

import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;

@Feature(SCHEDULER)
@RunWith(Parameterized.class)
public class DefaultSchedulerMessageSourcePollingTestCase extends DefaultSchedulerMessageSourceTestCase {

  @Parameterized.Parameter
  public String disabled;

  @Parameterized.Parameter(1)
  public Predicate<SensingNullMessageProcessor> assertion;

  @Parameterized.Parameter(2)
  public String errorMessage;

  @Parameterized.Parameters(name = "{0}")
  public static Object[][] parameters() {
    return new Object[][] {
        {"true", (Predicate<SensingNullMessageProcessor>) p -> p.event == null, "unexpected event when scheduler is disabled"},
        {"false", (Predicate<SensingNullMessageProcessor>) p -> p.event != null, "flow event never set by the source flow"}
    };
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();
    // noinspection deprecation
    setDisabled(valueOf(disabled));
  }

  @Test
  @Description("Verifies that no events are processed when the scheduler is disabled")
  @Issue("MULE-18063")
  public void simplePoll() throws Exception {
    DefaultSchedulerMessageSource schedulerMessageSource = createMessageSource();

    SensingNullMessageProcessor flow = getSensingNullMessageProcessor();
    schedulerMessageSource.setListener(flow);
    schedulerMessageSource.setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));

    doAnswer(invocationOnMock -> {
      CoreEvent inputEvent = invocationOnMock.getArgument(0);
      flow.process(inputEvent);
      return null;
    }).when(sourcePolicy).process(any(CoreEvent.class), any(), any());

    schedulerMessageSource.trigger();

    // let some time before making the assertion to avoid assert before the scheduler start
    MILLISECONDS.sleep(100);

    new PollingProber(RECEIVE_TIMEOUT, 100).check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return assertion.test(flow);
      }

      @Override
      public String describeFailure() {
        return errorMessage;
      }
    });
  }

}
