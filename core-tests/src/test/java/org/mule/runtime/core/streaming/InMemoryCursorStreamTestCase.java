/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.streaming;

import static org.mule.runtime.api.util.DataUnit.BYTE;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import org.mule.runtime.api.util.DataSize;
import org.mule.runtime.core.streaming.bytes.InMemoryCursorStreamConfig;
import org.mule.tck.size.SmallTest;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@SmallTest
@Features(STREAMING)
@Stories(STREAMING)
public class InMemoryCursorStreamTestCase extends InMemoryStreamingConfigContractTestCase {

  @Override
  protected void createConfig(int initialSize, int increment, int maxSize) {
    new InMemoryCursorStreamConfig(new DataSize(initialSize, BYTE),
                                   new DataSize(increment, BYTE),
                                   new DataSize(maxSize, BYTE));
  }
}
