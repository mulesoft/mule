/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.internal.processor.LoggerMessageProcessor.MULE_LOGGING_BLOCKING_CATEGORIES;

import org.junit.Rule;
import org.junit.Test;
import org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.slf4j.Logger;

public class LoggerBlockingCategoriesTestCase extends AbstractMuleTestCase {

  @Rule
  public SystemProperty muleEnableStatistics =
      new SystemProperty(MULE_LOGGING_BLOCKING_CATEGORIES, "some.category");


  @Test
  public void processTypeOfBlockingCategoryIsBlocking() {
    testCategory("some.category", BLOCKING);
  }

  @Test
  public void processTypeOfNonBlockingCategoryIsCpuLight() {
    testCategory("other.category", CPU_LITE);
  }

  private void testCategory(String category, ProcessingType expectedProcessingType) {
    LoggerMessageProcessor loggerMessageProcessor = loggerMessageProcessor(category);
    assertThat(loggerMessageProcessor.getProcessingType(), is(expectedProcessingType));
  }


  private LoggerMessageProcessor loggerMessageProcessor(String category) {
    LoggerMessageProcessor loggerMessageProcessor = new LoggerMessageProcessor();
    loggerMessageProcessor.setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));
    loggerMessageProcessor.initLogger();
    loggerMessageProcessor.initProcessingTypeIfPossible();
    loggerMessageProcessor.logger = mock(Logger.class);
    loggerMessageProcessor.setLevel("INFO");
    loggerMessageProcessor.setCategory(category);
    return loggerMessageProcessor;
  }

}
