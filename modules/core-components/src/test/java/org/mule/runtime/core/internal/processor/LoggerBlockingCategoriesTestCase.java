/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.test.allure.AllureConstants.Logging.LOGGING;
import static org.mule.test.allure.AllureConstants.Logging.LoggingStory.PROCESSING_TYPE;

import org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Set;

import org.slf4j.Logger;

import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(LOGGING)
@Story(PROCESSING_TYPE)
public class LoggerBlockingCategoriesTestCase extends AbstractMuleTestCase {

  private Set<String> oldBlockingCategories;

  @Test
  @Description("Blocking category type results in blocking processing type")
  @Issue("MULE-16414")
  public void processTypeOfBlockingCategoryIsBlocking() {
    testCategory("some.category", BLOCKING);
  }

  @Test
  @Description("Non Blocking category results in cpu light processing type")
  @Issue("MULE-16414")
  public void processTypeOfNonBlockingCategoryIsCpuLight() {
    testCategory("other.category", CPU_LITE);
  }

  private void testCategory(String category, ProcessingType expectedProcessingType) {
    LoggerMessageProcessor loggerMessageProcessor = loggerMessageProcessor(category);
    assertThat(loggerMessageProcessor.getProcessingType(), is(expectedProcessingType));
  }

  private LoggerMessageProcessor loggerMessageProcessor(String category) {
    LoggerMessageProcessor loggerMessageProcessor = new TestLoggerMessageProcessor();
    loggerMessageProcessor.setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));
    loggerMessageProcessor.initLogger();
    loggerMessageProcessor.initProcessingTypeIfPossible();
    loggerMessageProcessor.logger = mock(Logger.class);
    loggerMessageProcessor.setLevel("INFO");
    loggerMessageProcessor.setCategory(category);
    return loggerMessageProcessor;
  }

  /**
   * This class is used so that the the blocking categories does not depend on the order of classloading imposed by surefire.
   */
  private static class TestLoggerMessageProcessor extends LoggerMessageProcessor {

    @Override
    protected Set<String> getBlockingCategories() {
      return singleton("some.category");
    }
  }

}
