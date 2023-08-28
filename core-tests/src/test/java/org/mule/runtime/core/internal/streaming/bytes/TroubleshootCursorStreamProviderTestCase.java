/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
    int bufferSize = 10;
    int maxBufferSize = 20;
    InputStream dataStream = new ByteArrayInputStream(DATA);

    bufferManager = new PoolingByteBufferManager();

    InMemoryCursorStreamConfig config =
        new InMemoryCursorStreamConfig(new DataSize(bufferSize, BYTE),
                                       new DataSize(bufferSize / 2, BYTE),
                                       new DataSize(maxBufferSize, BYTE));

    return new InMemoryCursorStreamProvider(dataStream, config, bufferManager, componentLocation, trackStackTrace);
  }

}
