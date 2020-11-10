/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;
import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_SDK_POLLING_SOURCE_LIMIT;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.tck.probe.PollingProber.checkNot;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.ClassRule;
import org.junit.Test;

public class PollingSourceLimitingTestCase extends AbstractExtensionFunctionalTestCase {

  private static int PROBER_TIMEOUT = 10000;
  private static int CHECK_NOT_PROBER_TIMEOUT = 2000;
  private static int PROBER_FREQUENCY = 500;
  private static int NUMBER_OF_PETS = 7;

  private static MultiMap<Integer, String> ADOPTIONS = new MultiMap<>();

  @ClassRule
  public static SystemProperty enableLimit = new SystemProperty(ENABLE_SDK_POLLING_SOURCE_LIMIT, "true");

  public static class AdoptionProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      String pet = (String) event.getMessage().getPayload().getValue();
      Integer poll = (Integer) event.getMessage().getAttributes().getValue();
      synchronized (ADOPTIONS) {
        ADOPTIONS.put(poll, pet);
      }
      return event;
    }
  }

  @Override
  protected void doTearDown() throws Exception {
    ADOPTIONS.clear();
  }

  @Override
  protected boolean mustRegenerateExtensionModels() {
    return true;
  }

  @Override
  protected String getConfigFile() {
    return "polling-source-limiting-config.xml";
  }

  @Test
  public void noLimit() throws Exception {
    startFlow("noLimit");
    waitForAllPetsToBeAdopted();
    checkNoMorePetsAdopted();
    assertLimitIsApplied(Integer.MAX_VALUE);
  }

  @Test
  public void limitOne() throws Exception {
    startFlow("limitOne");
    waitForAllPetsToBeAdopted();
    checkNoMorePetsAdopted();
    assertLimitIsApplied(1);
  }

  @Test
  public void limitThree() throws Exception {
    startFlow("limitThree");
    waitForAllPetsToBeAdopted();
    checkNoMorePetsAdopted();
    assertLimitIsApplied(3);
  }

  @Test
  public void ascendingWatermark() throws Exception {
    startFlow("ascendingWatermark");
    waitForAllPetsToBeAdopted();
    checkNoMorePetsAdopted();
  }

  @Test
  public void descendingWatermark() throws Exception {
    startFlow("descendingWatermark");
    waitForAllPetsToBeAdopted();
    checkNoMorePetsAdopted();
  }

  @Test
  public void mixedWatermark() throws Exception {
    startFlow("mixedWatermark");
    waitForAllPetsToBeAdopted();
    checkNoMorePetsAdopted();
  }

  private void waitForAllPetsToBeAdopted() {
    check(PROBER_TIMEOUT, PROBER_FREQUENCY,
          () -> getAdoptedPets() == NUMBER_OF_PETS);
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) getFlowConstruct(flowName)).start();
  }

  private void assertLimitIsApplied(int limit) {
    int adoptionPolls = ADOPTIONS.size();
    for (int i = 0; i < adoptionPolls - 2; i++) {
      assertThat(ADOPTIONS.getAll(i), hasSize(limit));
    }
    assertThat(ADOPTIONS.getAll(adoptionPolls - 1), hasSize(lessThanOrEqualTo(limit)));
  }

  private int getAdoptedPets() {
    return ADOPTIONS.keySet().stream().map(key -> ADOPTIONS.getAll(key).size()).reduce(0, Integer::sum);
  }

  private void checkNoMorePetsAdopted() {
    checkNot(CHECK_NOT_PROBER_TIMEOUT, PROBER_FREQUENCY, () -> getAdoptedPets() > NUMBER_OF_PETS);
  }

}
