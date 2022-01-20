/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENABLE_POLLING_SOURCE_LIMIT_PARAMETER;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.tck.probe.PollingProber.checkNot;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ComponentLifeCycle.COMPONENT_LIFE_CYCLE;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SourcesStories.POLLING;
import static org.mule.test.petstore.extension.WatermarkingPetAdoptionSource.beginLatch;
import static org.mule.test.petstore.extension.WatermarkingPetAdoptionSource.resetSource;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;

@Feature(SOURCES)
@Stories({@Story(POLLING), @Story(COMPONENT_LIFE_CYCLE)})
public class PollingSourceRestartingTestCase extends AbstractExtensionFunctionalTestCase {

  private static int PROBER_TIMEOUT = 30000;
  private static int CHECK_NOT_PROBER_TIMEOUT = 5000;
  private static int PROBER_FREQUENCY = 500;

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
    return singletonMap(ENABLE_POLLING_SOURCE_LIMIT_PARAMETER, true);
  }

  // Since the ENABLE_POLLING_SOURCE_LIMIT_PARAMETER changes the extension model generator, we have to make the parsers cache
  // aware of this property so that each tests uses the expected parser with the expected extension model definition.
  @Override
  protected Map<String, String> artifactProperties() {
    return singletonMap(ENABLE_POLLING_SOURCE_LIMIT_PARAMETER, "true");
  }

  @Override
  protected String getConfigFile() {
    return "source/polling-source-restarting-config.xml";
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
    assertLimitIsApplied(3);
  }

  @Test
  public void processedItemsWithSameWatermarkAreNotReprocessedWhenSourceIsRestartedMidPollWithLimit() throws Exception {
    assertWatermarkingForStopStartScenario(asList("Anibal", "Barbara"),
                                           "processedItemsWithSameWatermarkAreNotReprocessedWhenSourceIsRestartedMidPollWithLimit");
    assertLimitIsApplied(2);
  }

  @Test
  public void processedItemsWithNewWatermarkAreReprocessedWhenSourceIsRestartedMidPollWithLimit() throws Exception {
    assertWatermarkingForStopStartScenario(asList("Anibal", "Barbara", "ANIBAL", "BARBARA", "Colonel Meow"),
                                           "processedItemsWithNewWatermarkAreReprocessedWhenSourceIsRestartedMidPollWithLimit");
    assertLimitIsApplied(2);
  }

  private void assertWatermarkingForStopStartScenario(List<String> expectedPets, String flowName) throws Exception {
    startFlow(flowName);
    beginLatch.await();
    stopFlow(flowName);
    check(PROBER_TIMEOUT, PROBER_FREQUENCY, () -> getFlowConstruct(flowName).getLifecycleState().isStopped());
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
