/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static java.util.stream.Collectors.toList;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.tck.probe.PollingProber.checkNot;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SourcesStories.POLLING;
import static org.mule.test.petstore.extension.NumberPetAdoptionSource.ALL_NUMBERS;
import static org.mule.test.petstore.extension.PetAdoptionSource.ALL_PETS;
import static org.mule.test.petstore.extension.PetAdoptionSource.STARTED_POLLS;
import static org.mule.test.petstore.extension.PetFailingPollingSource.POLL_INVOCATIONS;
import static org.mule.test.petstore.extension.PetFailingPollingSource.STARTED_SOURCES;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.PetAdoptionSource;
import org.mule.test.petstore.extension.PetFailingPollingSource;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@Feature(SOURCES)
@Story(POLLING)
public class PollingSourceSequentialPollingTestCase extends AbstractExtensionFunctionalTestCase {

  private static final int TIMEOUT = 2000;
  private static final int LONG_TIMEOUT = 7000;
  private static final int DELAY = 100;
  private static final int WAIT_IN_SECONDS = 5;
  private static final int LONG_WAIT_IN_SECONDS = 30;
  private static final List<CoreEvent> ADOPTION_EVENTS = new LinkedList<>();
  private static final Lock WAIT_LOCK = new ReentrantLock();

  public static class AdoptionProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      synchronized (ADOPTION_EVENTS) {
        ADOPTION_EVENTS.add(event);
      }
      return event;
    }
  }

  public static class WaitProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      try {
        if (WAIT_LOCK.tryLock(WAIT_IN_SECONDS, TimeUnit.SECONDS)) {
          WAIT_LOCK.unlock();
        }
      } catch (InterruptedException e) {
      }
      return event;
    }
  }

  public static class LongWaitProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      try {
        if (WAIT_LOCK.tryLock(LONG_WAIT_IN_SECONDS, TimeUnit.SECONDS)) {
          WAIT_LOCK.unlock();
        }
      } catch (InterruptedException e) {
      }
      return event;
    }
  }

  @Override
  protected void doTearDown() throws Exception {
    ADOPTION_EVENTS.clear();
    super.doTearDown();
  }

  @Before
  public void lock() {
    WAIT_LOCK.lock();
  }

  @After
  public void unlock() {
    WAIT_LOCK.unlock();
  }

  @Override
  protected String getConfigFile() {
    return "source/polling-source-sequential-polls-config.xml";
  }

  @Before
  public void resetCounters() throws Exception {
    PetFailingPollingSource.STARTED_POLLS = 0;
    POLL_INVOCATIONS.clear();
    STARTED_SOURCES.clear();
    STARTED_POLLS = 0;
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return false;
  }

  @Test
  public void concurrentPollsRunByDefault() throws Exception {
    startFlow("defaultConcurrentPoll");
    assertAllPetsAdopted();

    check(TIMEOUT, DELAY * 2, () -> {
      synchronized (ADOPTION_EVENTS) {
        return PetAdoptionSource.STARTED_POLLS > 1;
      }
    });
    stopFlow("defaultConcurrentPoll");
  }

  @Test
  public void sequentialPolls() throws Exception {
    startFlow("secuentialPoll");

    checkNot(TIMEOUT, DELAY * 2, () -> {
      synchronized (ADOPTION_EVENTS) {
        return STARTED_POLLS > 1;
      }
    });

    check(TIMEOUT * 3, DELAY * 2, () -> {
      synchronized (ADOPTION_EVENTS) {
        return STARTED_POLLS > 1;
      }
    });
    stopFlow("secuentialPoll");
  }

  @Test
  public void sequentialPollsLongProcess() throws Exception {
    startFlow("secuentialPollLongProcess");

    checkNot(LONG_TIMEOUT, DELAY * 2, () -> {
      synchronized (ADOPTION_EVENTS) {
        return STARTED_POLLS > 1;
      }
    });
    stopFlow("secuentialPollLongProcess");
  }

  @Test
  public void concurrentPolls() throws Exception {
    startFlow("concurrentPoll");
    assertAllPetsAdopted();

    check(TIMEOUT, DELAY * 2, () -> {
      synchronized (ADOPTION_EVENTS) {
        return PetAdoptionSource.STARTED_POLLS > 1;
      }
    });
    stopFlow("concurrentPoll");
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
