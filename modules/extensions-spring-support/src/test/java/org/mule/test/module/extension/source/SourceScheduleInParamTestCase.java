/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static java.lang.Thread.sleep;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SourcesStories.POLLING;
import static org.mule.test.petstore.extension.PetAdoptionSchedulerInParamSource.ALL_PETS;
import static org.mule.test.petstore.extension.PetAdoptionSchedulerInParamSource.FAILED_ADOPTION_COUNT;
import static org.mule.test.petstore.extension.PetAdoptionSchedulerInParamSource.STARTED_POLLS;
import static org.mule.test.petstore.extension.PetFailingPollingSource.POLL_INVOCATIONS;
import static org.mule.test.petstore.extension.PetFailingPollingSource.STARTED_SOURCES;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.PetAdoptionSchedulerInParamSource;
import org.mule.test.petstore.extension.PetFailingPollingSource;

import java.util.LinkedList;
import java.util.List;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

import org.junit.Before;
import org.junit.Test;

@Feature(SOURCES)
@Story(POLLING)
@Issue("MULE-19757")
public class SourceScheduleInParamTestCase extends AbstractExtensionFunctionalTestCase {

  private static final int TIMEOUT = 5000;
  private static final int DELAY = 100;
  private static final List<CoreEvent> ADOPTION_EVENTS = new LinkedList<>();

  public static class AdoptionProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      synchronized (ADOPTION_EVENTS) {
        ADOPTION_EVENTS.add(event);
      }
      return event;
    }
  }

  @Override
  protected void doTearDown() throws Exception {
    ADOPTION_EVENTS.clear();
  }

  @Override
  protected String getConfigFile() {
    return "source/source-schedule-in-param-config.xml";
  }

  @Before
  public void resetCounters() throws Exception {
    PetFailingPollingSource.STARTED_POLLS = 0;
    POLL_INVOCATIONS.clear();
    STARTED_SOURCES.clear();
    STARTED_POLLS = 0;
  }

  @Test
  public void vanillaPoll() throws Exception {
    startFlow("vanilla");
    assertAllPetsAdopted();

    check(TIMEOUT, DELAY * 2, () -> {
      synchronized (ADOPTION_EVENTS) {
        return PetAdoptionSchedulerInParamSource.COMPLETED_POLLS > 1 &&
            PetAdoptionSchedulerInParamSource.ADOPTED_PET_COUNT >= ADOPTION_EVENTS.size();
      }
    });
  }

  @Test
  public void failingPoll() throws Exception {
    startFlow("failingPoll");
    check(TIMEOUT, DELAY, () -> FAILED_ADOPTION_COUNT >= ALL_PETS.size());
  }

  @Description("This test reflects a behavior that we must preserve, when a polling source is stopped and started the scheduler must be stopped and a new one must be started.")
  @Test
  public void whenSourceIsStopAndStartedSchedulerIsReset() throws Exception {
    startFlow("longFrequencyPoll");
    assertStartedPolls(1);
    stopFlow("longFrequencyPoll");
    sleep(1000);
    startFlow("longFrequencyPoll");
    assertStartedPolls(1);
  }

  private void assertStartedPolls(int polls) {
    check(TIMEOUT, DELAY * 2, () -> {
      assertThat(STARTED_POLLS, is(polls));
      return true;
    });
  }

  private void assertAllPetsAdopted() {
    check(TIMEOUT, DELAY * 2, () -> {
      synchronized (ADOPTION_EVENTS) {
        return ADOPTION_EVENTS.size() >= ALL_PETS.size() &&
            ADOPTION_EVENTS.stream().map(e -> e.getMessage().getPayload().getValue().toString()).collect(toList())
                .containsAll(ALL_PETS);
      }
    });
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) getFlowConstruct(flowName)).start();
  }

  private void stopFlow(String flowName) throws Exception {
    ((Stoppable) getFlowConstruct(flowName)).stop();
  }
}
