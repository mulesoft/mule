/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static java.lang.Math.min;
import static java.lang.String.format;
import static java.lang.System.arraycopy;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;

public abstract class AbstractByteStreamingTestCase extends AbstractMuleTestCase {

  protected static final int KB_256 = 256 * 1024;
  protected static final int MB_1 = 1024 * 1024;
  protected static final int MB_2 = MB_1 * 2;

  protected String data;

  public AbstractByteStreamingTestCase(int dataSize) {
    data = randomAlphanumeric(dataSize);
  }

  protected String toString(byte[] dest) throws IOException {
    return IOUtils.toString(dest, Charset.defaultCharset().name());
  }

  protected String toString(byte[] dest, int offset, int limit) throws IOException {
    int len = min(dest.length - offset, limit);
    byte[] buffer = new byte[len];
    arraycopy(dest, offset, buffer, 0, len);

    return toString(buffer);
  }

  protected String toString(InputStream inputStream) throws IOException {
    return IOUtils.toString(inputStream);
  }

  protected String toString(ByteBuffer buffer) throws IOException {
    byte[] data = new byte[buffer.remaining()];
    buffer.get(data);

    return toString(data);
  }

  protected void assertEquals(String actual, String expect) {
    int i = 0;
    for (; i < expect.length(); i++) {
      if (i >= actual.length()) {
        fail(format("Actual value was shorter than expected. Was expecting %d but was", expect.length(), actual.length()));
      }

      if (actual.charAt(i) != expect.charAt(i)) {
        fail("Breaks at char " + i);
      }
    }

    assertThat(actual.length(), is(expect.length()));
  }
}
