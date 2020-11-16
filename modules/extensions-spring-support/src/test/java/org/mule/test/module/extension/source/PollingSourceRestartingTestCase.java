/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENABLE_POLLING_SOURCE_LIMIT_PARAMETER;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.tck.probe.PollingProber.checkNot;
import static org.mule.test.petstore.extension.WatermarkingPetAdoptionSource.beginLatch;
import static org.mule.test.petstore.extension.WatermarkingPetAdoptionSource.resetSource;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class PollingSourceRestartingTestCase extends AbstractExtensionFunctionalTestCase {

  private static int PROBER_TIMEOUT = 30000;
  private static int CHECK_NOT_PROBER_TIMEOUT = 30000;
  private static int PROBER_FREQUENCY = 500;

  protected static final Map<String, Object> EXTENSION_LOADER_CONTEXT_ADDITIONAL_PARAMS = new HashMap<String, Object>() {

    {
      put(ENABLE_POLLING_SOURCE_LIMIT_PARAMETER, true);
    }
  };

  protected static MultiMap<Integer, String> ADOPTIONS = new MultiMap<>();

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
  protected boolean mustRegenerateExtensionModels() {
    return true;
  }

  @Override
  protected Map<String, Object> getExtensionLoaderContextAdditionalParameters() {
    return EXTENSION_LOADER_CONTEXT_ADDITIONAL_PARAMS;
  }

  @Override
  protected String getConfigFile() {
    return "polling-source-restarting-config.xml";
  }

  @Override
  protected void doTearDown() throws Exception {
    ADOPTIONS.clear();
    resetSource();
  }

  @Test
  public void unprocessedItemsAreProcessedWhenSourceIsRestartedMidPoll() throws Exception {
    assertWatermarkingForStopStartScenario(asList("Anibal", "Barbara", "Colonel Meow", "Daphne", "Elsa"),
                                           "unprocessedItemsAreProcessedWhenSourceIsRestartedMidPoll");
  }

  @Test
  public void processedItemsWithSameWatermarkAreNotReprocessedWhenSourceIsRestartedMidPoll() throws Exception {
    assertWatermarkingForStopStartScenario(asList("Anibal", "Barbara"),
                                           "processedItemsWithSameWatermarkAreNotReprocessedWhenSourceIsRestartedMidPoll");
  }

  @Test
  public void processedItemsWithNewWatermarkAreReprocessedWhenSourceIsRestartedMidPoll() throws Exception {
    assertWatermarkingForStopStartScenario(asList("Anibal", "Barbara", "ANIBAL", "BARBARA", "Colonel Meow"),
                                           "processedItemsWithNewWatermarkAreReprocessedWhenSourceIsRestartedMidPoll");
  }

  @Test
  public void unprocessedItemsAreProcessedWhenSourceIsRestartedMidPollWithLimit() throws Exception {
    assertWatermarkingForStopStartScenario(asList("Anibal", "Barbara", "Colonel Meow", "Daphne", "Elsa"),
                                           "unprocessedItemsAreProcessedWhenSourceIsRestartedMidPollWithLimit");
  }

  @Test
  public void processedItemsWithSameWatermarkAreNotReprocessedWhenSourceIsRestartedMidPollWithLimit() throws Exception {
    assertWatermarkingForStopStartScenario(asList("Anibal", "Barbara"),
                                           "processedItemsWithSameWatermarkAreNotReprocessedWhenSourceIsRestartedMidPollWithLimit");
  }

  @Test
  public void processedItemsWithNewWatermarkAreReprocessedWhenSourceIsRestartedMidPollWithLimit() throws Exception {
    assertWatermarkingForStopStartScenario(asList("Anibal", "Barbara", "ANIBAL", "BARBARA", "Colonel Meow"),
                                           "processedItemsWithNewWatermarkAreReprocessedWhenSourceIsRestartedMidPoll");
  }

  private void assertWatermarkingForStopStartScenario(List<String> expectedPets, String flowName) throws Exception {
    startFlow(flowName);
    beginLatch.await();
    stopFlow(flowName);
    check(5000, 1000, () -> getFlowConstruct(flowName).getLifecycleState().isStopped());
    startFlow(flowName);

    waitForAllPetsToBeAdopted(expectedPets);
    checkNoMorePetsAdopted(expectedPets);
    assertAdoptedPets(expectedPets);
  }

  private void waitForAllPetsToBeAdopted(List<String> pets) {
    check(PROBER_TIMEOUT, PROBER_FREQUENCY, () -> ADOPTIONS.size() == pets.size());
  }

  private void checkNoMorePetsAdopted(List<String> pets) {
    checkNot(CHECK_NOT_PROBER_TIMEOUT, PROBER_FREQUENCY, () -> ADOPTIONS.size() > pets.size());
  }

  private void assertAdoptedPets(List<String> pets) {
    List<String> adoptedPets = new ArrayList<>();
    for (Integer key : ADOPTIONS.keySet()) {
      adoptedPets.addAll(ADOPTIONS.getAll(key));
    }
    assertThat(adoptedPets, contains(pets.toArray()));
  }

  private void assertLimitIsApplied(int limit) {
    int adoptionPolls = ADOPTIONS.keySet().size();
    for (int i = 0; i < adoptionPolls - 1; i++) {
      assertThat(ADOPTIONS.getAll(i), hasSize(limit));
    }
    assertThat(ADOPTIONS.getAll(adoptionPolls - 1), hasSize(lessThanOrEqualTo(limit)));
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) getFlowConstruct(flowName)).start();
  }

  private void stopFlow(String flowName) throws Exception {
    ((Stoppable) getFlowConstruct(flowName)).stop();
  }
}
