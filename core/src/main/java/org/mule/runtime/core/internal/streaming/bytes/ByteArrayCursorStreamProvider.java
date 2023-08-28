/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;

import java.io.Serializable;

/**
 * A {@link CursorStreamProvider} which is backed by a fixed {@code byte[]}.
 * <p>
 * Notice that since the {@link #content} data is already fully loaded into memory, this kind of defeats the purpose of the cursor
 * provider. The purpose of this method is to provide a way to bridge the given data with the {@link CursorStreamProvider}
 * abstraction. Possible use cases are mainly deserialization and testing. <b>Think twice</b> before using this method. Most
 * likely you're doing something wrong.
 * <p>
 * Also consider that because the data is already in memory, the cursors will never buffer into disk and will never be closed or
 * released. Resources are freed when this instance is garbage collected.
 *
 * @since 4.0
 */
public class ByteArrayCursorStreamProvider implements CursorStreamProvider, Serializable {

  private static final long serialVersionUID = -7152264489981618670L;

  private byte[] content;

  public ByteArrayCursorStreamProvider(byte[] content) {
    this.content = content;
  }

  @Override
  public CursorStream openCursor() {
    return new ByteArrayCursorStream(this, content);
  }

  @Override
  public void close() {}

  @Override
  public void releaseResources() {}

  @Override
  public boolean isClosed() {
    return false;
  }
}
