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
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SourcesStories.POLLING;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SourcesStories.WATERMARK;

import static java.util.Collections.singletonMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.Map;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;

@Feature(SOURCES)
@Stories({@Story(POLLING), @Story(WATERMARK)})
public class PollingSourceLimitingTestCase extends AbstractExtensionFunctionalTestCase {

  private static int PROBER_TIMEOUT = 10000;
  private static int CHECK_NOT_PROBER_TIMEOUT = 2000;
  private static int PROBER_FREQUENCY = 500;
  private static int NUMBER_OF_PETS = 7;

  private static MultiMap<Integer, String> ADOPTIONS = new MultiMap<>();

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
    return "source/polling-source-limiting-config.xml";
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
          () -> ADOPTIONS.size() == NUMBER_OF_PETS);
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) getFlowConstruct(flowName)).start();
  }

  private void assertLimitIsApplied(int limit) {
    int adoptionPolls = ADOPTIONS.keySet().size();
    for (int i = 0; i < adoptionPolls - 1; i++) {
      assertThat(ADOPTIONS.getAll(i), hasSize(limit));
    }
    assertThat(ADOPTIONS.getAll(adoptionPolls - 1), hasSize(lessThanOrEqualTo(limit)));
  }

  private void checkNoMorePetsAdopted() {
    checkNot(CHECK_NOT_PROBER_TIMEOUT, PROBER_FREQUENCY, () -> ADOPTIONS.size() > NUMBER_OF_PETS);
  }

}
