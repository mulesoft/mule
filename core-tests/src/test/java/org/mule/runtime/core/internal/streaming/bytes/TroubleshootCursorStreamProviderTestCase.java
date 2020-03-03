/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.streaming.bytes;

import static org.mule.runtime.api.util.DataUnit.BYTE;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.TROUBLESHOOTING;

import org.junit.After;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.util.DataSize;
import org.mule.runtime.core.api.streaming.bytes.InMemoryCursorStreamConfig;
import org.mule.runtime.core.api.streaming.bytes.InMemoryCursorStreamProvider;
import org.mule.runtime.core.internal.streaming.AbstractTroubleshootCursorProviderTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Feature(STREAMING)
@Story(TROUBLESHOOTING)
public class TroubleshootCursorStreamProviderTestCase extends AbstractTroubleshootCursorProviderTestCase {

  private static final byte[] DATA = "Hello".getBytes();

  private PoolingByteBufferManager bufferManager;

  @After
  public void after() {
    super.after();
    bufferManager.dispose();
  }

  protected CursorStreamProvider createCursorProvider() {
    int bufferSize = 1;
    int maxBufferSize = 10;
    InputStream dataStream = new ByteArrayInputStream(DATA);

    bufferManager = new PoolingByteBufferManager();

    InMemoryCursorStreamConfig config =
        new InMemoryCursorStreamConfig(new DataSize(bufferSize, BYTE),
                                       new DataSize(bufferSize / 2, BYTE),
                                       new DataSize(maxBufferSize, BYTE));

    return new InMemoryCursorStreamProvider(dataStream, config, bufferManager, componentLocation, trackStackTrace);
  }

}
