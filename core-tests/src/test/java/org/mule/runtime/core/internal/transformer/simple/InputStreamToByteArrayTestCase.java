/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.memory.provider.type.ByteBufferType.HEAP;

import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.streaming.bytes.ByteBufferManager;
import org.mule.runtime.core.api.streaming.bytes.InMemoryCursorStreamConfig;
import org.mule.runtime.core.api.streaming.bytes.InMemoryCursorStreamProvider;
import org.mule.runtime.core.internal.streaming.bytes.SimpleByteBufferManager;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.inject.Inject;

import org.junit.Test;

@SmallTest
public class InputStreamToByteArrayTestCase extends AbstractMuleContextTestCase {

  private static final String DONKEY = "donkey";

  private InputStreamToByteArray transformer = new InputStreamToByteArray();

  @Inject
  private MemoryManagementService memoryManagementService;

  @Override
  protected boolean doTestClassInjection() {
    return true;
  }

  @Test
  public void transformInputStream() throws Exception {
    InputStream inputStream = new ByteArrayInputStream(DONKEY.getBytes());
    assertThat(transformer.transform(inputStream), equalTo(DONKEY.getBytes()));
  }

  @Test
  public void transformCursorStreamProvider() throws Exception {
    InputStream inputStream = new ByteArrayInputStream(DONKEY.getBytes());
    CursorStreamProvider provider =
        new InMemoryCursorStreamProvider(inputStream, InMemoryCursorStreamConfig.getDefault(), getBufferManager());
    assertThat(transformer.transform(provider), equalTo(DONKEY.getBytes()));

  }

  private ByteBufferManager getBufferManager() {
    ByteBufferManager byteBufferManager = new SimpleByteBufferManager();
    byteBufferManager.setByteBufferProvider(memoryManagementService.getByteBufferProvider(muleContext.getId(), HEAP));
    return byteBufferManager;
  }
}

