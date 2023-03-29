/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.core.internal.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomByteArrayInputStream extends InputStream {

  private final InputStream inputStream;

  private AtomicInteger closeCount = new AtomicInteger();

  public CustomByteArrayInputStream(InputStream inputStream) {
    this.inputStream = inputStream;
  }

  @Override
  public int read() throws IOException {
    return inputStream.read();
  }

  public void close() throws IOException {
    closeCount.incrementAndGet();
    inputStream.close();
  }

  public int getCloseCount() {
    return closeCount.intValue();
  }
}
