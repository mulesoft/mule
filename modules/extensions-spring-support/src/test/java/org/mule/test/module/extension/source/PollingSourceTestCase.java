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
import static org.mule.test.petstore.extension.NumberPetAdoptionSource.ALL_NUMBERS;
import static org.mule.test.petstore.extension.PetAdoptionSource.ALL_PETS;
import static org.mule.test.petstore.extension.PetAdoptionSource.FAILED_ADOPTION_COUNT;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.PetAdoptionSource;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

public class PollingSourceTestCase extends AbstractExtensionFunctionalTestCase {

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
    return "polling-source-config.xml";
  }

  @Test
  public void vanillaPoll() throws Exception {
    startFlow("vanilla");
    assertAllPetsAdopted();

    check(5000, 200, () -> {
      synchronized (ADOPTION_EVENTS) {
        return PetAdoptionSource.COMPLETED_POLLS > 1 &&
            PetAdoptionSource.ADOPTED_PET_COUNT >= ADOPTION_EVENTS.size();
      }
    });
  }

  @Test
  public void idempotentPoll() throws Exception {
    startFlow("idempotent");
    check(5000, 100, () -> {
      synchronized (ADOPTION_EVENTS) {
        return PetAdoptionSource.REJECTED_ADOPTIONS >= ALL_PETS.size() &&
            ALL_PETS.containsAll(ADOPTION_EVENTS.stream()
                .map(e -> e.getMessage().getPayload().getValue().toString())
                .collect(toList()));
      }
    });
    assertIdempotentAdoptions();
  }

  @Test
  public void idempotentLocksAreReleased() throws Exception {
    startFlow("idempotentLocksAreReleased");
    assertAllPetsAdopted();
    doTearDown();
    assertAllPetsAdopted();
  }

  @Test
  public void watermarkPoll() throws Exception {
    startFlow("watermark");
    assertAllPetsAdopted();

    assertIdempotentAdoptions();
  }

  @Test
  public void failingPoll() throws Exception {
    startFlow("failingPoll");
    check(5000, 100, () -> FAILED_ADOPTION_COUNT >= ALL_PETS.size());
  }

  @Test
  public void multiplePhasesOfWatermarkPoll() throws Exception {
    startFlow("multiplePhasesOfWaterMark");
    assertIdempotentAdoptions();
  }

  @Test
  public void multiplePhasesOfWatermarkWithIncreasingAndDecreasingWatermarksPoll() throws Exception {
    startFlow("multiplePhasesOfWatermarkWithIncreasingAndDecreasingWatermarks");
    assertAllNumbersAdoptedExactlyOnce();
  }

  /* This test checks that when a polling source with a fixed frequency scheduler with start delay is restarted, the
  start delay is not applied again. The polling source of this test is set to fail midway through populating the pet
  adoption list, which will provoke a restart. Without the changes made in MULE-16974, this test would fail, because the
  start delay would be re-applied on the restart and the probe would timeout. */
  @Test
  public void whenReconnectingAfterConnectionExceptionSchedulerRunsWithoutStartDelay() throws Exception {
    startFlow("fixedFrequencyReconnectingPoll");
    assertAllPetsAdopted();
  }

  private void assertIdempotentAdoptions() {
    checkNot(5000, 100, () -> {
      synchronized (ADOPTION_EVENTS) {
        return ADOPTION_EVENTS.size() > ALL_PETS.size();
      }
    });
  }

  private void assertAllPetsAdopted() {
    check(5000, 200, () -> {
      synchronized (ADOPTION_EVENTS) {
        return ADOPTION_EVENTS.size() >= ALL_PETS.size() &&
            ADOPTION_EVENTS.stream().map(e -> e.getMessage().getPayload().getValue().toString()).collect(toList())
                .containsAll(ALL_PETS);
      }
    });
  }

  private void assertAllNumbersAdoptedExactlyOnce() {
    check(5000, 200, () -> {
      synchronized (ADOPTION_EVENTS) {
        return ADOPTION_EVENTS.size() == ALL_NUMBERS.size() &&
            ADOPTION_EVENTS.stream().map(e -> e.getMessage().getPayload().getValue().toString()).collect(toList())
                .containsAll(ALL_NUMBERS);
      }
    });
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) getFlowConstruct(flowName)).start();
  }
}
