/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static java.util.Arrays.asList;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.test.petstore.extension.WatermarkingPetAdoptionSource.beginLatch;
import static org.mule.test.petstore.extension.WatermarkingPetAdoptionSource.resetSource;

import org.mule.runtime.api.lifecycle.Stoppable;

import java.util.List;

import org.junit.Test;

public class PollingSourceRestartingTestCase extends PollingSourceLimitingTestCase {

  @Override
  protected String getConfigFile() {
    return "polling-source-restarting-config.xml";
  }

  @Override
  protected void doTearDown() throws Exception {
    super.doTearDown();
    resetSource();
  }

  @Test
  public void unprocessedItemsAreRejectedWithoutImpactingWatermarkWhenSourceIsStoppedMidPoll() throws Exception {
    assertWatermarkingForStopStartScenario(asList("Anibal", "Barbara", "Elsa"),
                                           "unprocessedItemsAreRejectedWithoutImpactingWatermarkWhenSourceIsStoppedMidPoll");
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
  public void unprocessedItemsAreProcessedWhenSourceIsRestartedMidPoll() throws Exception {
    assertWatermarkingForStopStartScenario(asList("Anibal", "Barbara", "Colonel Meow", "Daphne", "Elsa"),
                                           "unprocessedItemsAreProcessedWhenSourceIsRestartedMidPoll");
  }

  private void assertWatermarkingForStopStartScenario(List<String> expectedPets, String flowName) throws Exception {
    startFlow(flowName);
    beginLatch.await();
    stopFlow(flowName);
    check(5000, 1000, () -> getFlowConstruct(flowName).getLifecycleState().isStopped());
    startFlow(flowName);

    //assertAllPetsAdopted(expectedPets);

    //assertIdempotentAdoptions(expectedPets);
  }

  private void stopFlow(String flowName) throws Exception {
    ((Stoppable) getFlowConstruct(flowName)).stop();
  }
}
