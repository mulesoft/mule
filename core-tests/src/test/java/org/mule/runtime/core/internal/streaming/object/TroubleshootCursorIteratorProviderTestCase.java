/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.streaming.object;

import static java.util.Arrays.asList;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.TROUBLESHOOTING;

import java.util.List;

import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.core.api.streaming.iterator.ConsumerStreamingIterator;
import org.mule.runtime.core.api.streaming.object.InMemoryCursorIteratorConfig;
import org.mule.runtime.core.internal.streaming.AbstractTroubleshootCursorProviderTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(STREAMING)
@Story(TROUBLESHOOTING)
public class TroubleshootCursorIteratorProviderTestCase extends AbstractTroubleshootCursorProviderTestCase {

  private static final List<String> DATA = asList("1", "2");

  protected CursorProvider createCursorProvider() {
    return new InMemoryCursorIteratorProvider(new ConsumerStreamingIterator(new TestConsumer(DATA)),
                                              new InMemoryCursorIteratorConfig(1, 1, 10),
                                              setComponentLocation ? fromSingleComponent("log") : null);
  }

  @Override
  protected Class getCursorProviderImplementation() {
    return AbstractCursorIteratorProvider.class;
  }

  @Override
  protected String getCursorProviderTrackingCloseField() {
    return "TRACK_CURSOR_PROVIDER_CLOSE";
  }
}
