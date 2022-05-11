/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.tck.probe.PollingProber.checkNot;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SourcesStories.POLLING;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SourcesStories.WATERMARK;
import static org.mule.test.petstore.extension.PetAdoptionSource.ALL_PETS;
import static org.mule.test.petstore.extension.WatermarkingPetAdoptionSource.resetSource;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;

@Feature(SOURCES)
@Stories({@Story(POLLING), @Story(WATERMARK)})
public class PollingSourceWatermarkTestCase extends AbstractExtensionFunctionalTestCase {

  private static int TEST_TIMEOUT = 120000;
  private static int SHORT_TIMEOUT = 5000;
  private static int LONG_TIMEOUT = 30000;
  private static int PROBER_FREQUENCY = 100;

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
  protected int getTimeoutSystemProperty() {
    return TEST_TIMEOUT;
  }

  @Override
  protected void doTearDown() throws Exception {
    ADOPTION_EVENTS.clear();
    resetSource();
  }

  @Override
  protected String getConfigFile() {
    return "source/polling-source-watermark-config.xml";
  }

  @Test
  public void watermarkPoll() throws Exception {
    startFlow("watermark");

    assertAllPetsAdopted(ALL_PETS);

    assertIdempotentAdoptions(ALL_PETS);
  }

  @Test
  public void repeatedItemInNewPollSetsUpdatedWatermark() throws Exception {
    List<String> expectedPets = asList("Anibal", "ANIBAL");

    startFlow("repeatedItemInNewPollSetsUpdatedWatermark");

    assertAllPetsAdopted(expectedPets);

    assertIdempotentAdoptions(expectedPets);
  }

  @Test
  public void repeatedItemInNewPollDoesNotSetUpdatedWatermark() throws Exception {
    List<String> expectedPets = asList("Anibal", "Barbara", "Colonel Meow", "BARBARA");

    startFlow("repeatedItemInNewPollDoesNotSetUpdatedWatermark");

    assertAllPetsAdopted(expectedPets);

    assertIdempotentAdoptions(expectedPets);
  }

  private void assertIdempotentAdoptions(List<String> pets) {
    checkNot(LONG_TIMEOUT, PROBER_FREQUENCY, () -> {
      synchronized (ADOPTION_EVENTS) {
        return ADOPTION_EVENTS.size() > pets.size();
      }
    });
  }

  private void assertAllPetsAdopted(List<String> pets) {
    check(SHORT_TIMEOUT, PROBER_FREQUENCY, () -> {
      synchronized (ADOPTION_EVENTS) {
        return ADOPTION_EVENTS.size() >= pets.size() &&
            ADOPTION_EVENTS.stream().map(e -> e.getMessage().getPayload().getValue().toString()).collect(toList())
                .containsAll(pets);
      }
    });
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) getFlowConstruct(flowName)).start();
  }
}
