/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.tck.probe.PollingProber.checkNot;
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

import org.junit.Ignore;
import org.junit.Test;

@Ignore("MULE-19152")
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

    assertThat(PetAdoptionSource.COMPLETED_POLLS, is(greaterThan(1)));
    assertThat(PetAdoptionSource.ADOPTED_PET_COUNT, is(greaterThanOrEqualTo(ADOPTION_EVENTS.size())));
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

  private void assertIdempotentAdoptions() {
    checkNot(5000, 100, () -> {
      synchronized (ADOPTION_EVENTS) {
        return ADOPTION_EVENTS.size() > ALL_PETS.size();
      }
    });
  }

  private void assertAllPetsAdopted() {
    check(5000, 100, () -> {
      synchronized (ADOPTION_EVENTS) {
        return ADOPTION_EVENTS.size() >= ALL_PETS.size() &&
            ALL_PETS.containsAll(ADOPTION_EVENTS.stream()
                .map(e -> e.getMessage().getPayload().getValue().toString())
                .collect(toList()));
      }
    });
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) getFlowConstruct(flowName)).start();
  }
}
