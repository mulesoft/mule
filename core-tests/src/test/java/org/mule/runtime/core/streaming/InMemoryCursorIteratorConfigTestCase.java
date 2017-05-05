/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.streaming;

import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.BYTES_STREAMING;
import org.mule.runtime.core.streaming.object.InMemoryCursorIteratorConfig;
import org.mule.tck.size.SmallTest;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@SmallTest
@Features(STREAMING)
@Stories(BYTES_STREAMING)
public class InMemoryCursorIteratorConfigTestCase extends InMemoryStreamingConfigContractTestCase {

  @Override
  protected void createConfig(int initialSize, int increment, int maxSize) {
    new InMemoryCursorIteratorConfig(initialSize, increment, maxSize);
  }
}
