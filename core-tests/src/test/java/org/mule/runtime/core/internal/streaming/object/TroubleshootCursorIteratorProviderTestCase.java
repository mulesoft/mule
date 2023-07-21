/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.streaming.object;

import static java.util.Arrays.asList;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.TROUBLESHOOTING;

import java.util.List;

import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.core.api.streaming.object.InMemoryCursorIteratorConfig;
import org.mule.runtime.core.internal.streaming.AbstractTroubleshootCursorProviderTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(STREAMING)
@Story(TROUBLESHOOTING)
public class TroubleshootCursorIteratorProviderTestCase extends AbstractTroubleshootCursorProviderTestCase {

  private static final List<String> DATA = asList("1", "2");

  protected CursorProvider createCursorProvider() {

    InMemoryCursorIteratorConfig config = new InMemoryCursorIteratorConfig(1, 1, 10);

    return new InMemoryCursorIteratorProvider(DATA.iterator(), config, componentLocation, trackStackTrace);

  }

}
