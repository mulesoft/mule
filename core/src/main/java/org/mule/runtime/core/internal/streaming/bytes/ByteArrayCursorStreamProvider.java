/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;

/**
 * A {@link CursorStreamProvider} which is backed by a fixed {@link byte[]}.
 * <p>
 * Notice that since the {@link #content} data is already fully loaded into memory, this kind of
 * defeats the purpose of the cursor provider. The purpose of this method is to provide a way to
 * bridge the given data with the {@link CursorStreamProvider} abstraction. Possible use cases are
 * mainly deserialization and testing. <b>Think twice</b> before using this method. Most likely you're
 * doing something wrong.
 * <p>
 * Also consider that because the data is already in memory, the cursors will never buffer into disk.
 *
 * @since 4.0
 */
public class ByteArrayCursorStreamProvider implements CursorStreamProvider {

  private boolean closed = false;
  private byte[] content;

  public ByteArrayCursorStreamProvider(byte[] content) {
    this.content = content;
  }

  @Override
  public CursorStream openCursor() {
    return new ByteArrayCursorStream(this, content);
  }

  @Override
  public void close() {
    closed = true;
  }

  @Override
  public void releaseResources() {
    content = null;
  }

  @Override
  public boolean isClosed() {
    return closed;
  }
}
