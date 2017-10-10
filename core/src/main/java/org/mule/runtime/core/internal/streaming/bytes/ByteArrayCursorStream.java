/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static java.lang.Math.min;
import static java.lang.Math.toIntExact;
import static java.lang.System.arraycopy;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;

import java.io.IOException;

/**
 * A {@link CursorStream} which is backed by a fixed {@link byte[]}.
 * <p>
 * Notice that since the {@link #content} data is already fully loaded into memory, this kind of
 * defeats the purpose of the cursor provider. The purpose of this method is to provide a way to
 * bridge the given data with the {@link CursorStream} abstraction. Possible use cases are
 * mainly deserialization and testing. <b>Think twice</b> before using this method. Most likely you're
 * doing something wrong.
 * <p>
 * Also consider that because the data is already in memory, the cursors will never buffer into disk.
 *
 * @since 4.0
 */
public class ByteArrayCursorStream extends AbstractCursorStream {

  private byte[] content;

  public ByteArrayCursorStream(CursorStreamProvider provider, byte[] content) {
    super(provider);
    this.content = content;
  }

  @Override
  protected int doRead() throws IOException {
    final int position = toIntExact(getPosition());
    if (position >= content.length) {
      return -1;
    }

    int value = unsigned(content[position]);
    this.position++;

    return value;
  }

  @Override
  protected int doRead(byte[] b, int off, int len) throws IOException {
    final int position = toIntExact(getPosition());
    len = min(len, content.length - position);

    if (len <= 0) {
      return -1;
    }

    arraycopy(content, position, b, off, len);

    this.position += len;
    return len;
  }
}
