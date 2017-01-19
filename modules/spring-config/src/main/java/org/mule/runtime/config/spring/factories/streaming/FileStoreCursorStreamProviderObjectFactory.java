/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.factories.streaming;

import org.mule.runtime.api.util.DataSize;
import org.mule.runtime.api.util.DataUnit;
import org.mule.runtime.core.api.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.core.api.streaming.bytes.FileStoreCursorStreamConfig;

public class FileStoreCursorStreamProviderObjectFactory
    extends AbstractCursorStreamProviderObjectFactory<CursorStreamProviderFactory> {

  private final int maxInMemorySize;
  private DataUnit dataUnit;

  public FileStoreCursorStreamProviderObjectFactory(int maxInMemorySize, DataUnit dataUnit) {
    this.maxInMemorySize = maxInMemorySize;
    this.dataUnit = dataUnit;
  }

  @Override
  public CursorStreamProviderFactory doGetObject() throws Exception {
    FileStoreCursorStreamConfig config = new FileStoreCursorStreamConfig(new DataSize(maxInMemorySize, dataUnit));
    return streamingManager.forBytes().getFileStoreCursorStreamProviderFactory(config);
  }
}
