/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.streaming;

import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.BYTES_STREAMING;
import org.mule.runtime.core.api.streaming.object.InMemoryCursorIteratorConfig;
import org.mule.tck.size.SmallTest;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@SmallTest
@Feature(STREAMING)
@Story(BYTES_STREAMING)
public class InMemoryCursorIteratorConfigTestCase extends InMemoryStreamingConfigContractTestCase {

  @Override
  protected void createConfig(int initialSize, int increment, int maxSize) {
    new InMemoryCursorIteratorConfig(initialSize, increment, maxSize);
  }
}
