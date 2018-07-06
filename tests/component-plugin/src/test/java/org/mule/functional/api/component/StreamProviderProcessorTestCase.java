/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.copyOfRange;
import static org.apache.commons.lang3.ArrayUtils.addAll;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static org.mule.functional.api.component.StreamProviderProcessor.BYTES;
import static org.mule.functional.api.component.StreamProviderProcessor.BYTES_PART;
import static org.mule.functional.api.component.StreamProviderProcessor.END;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class StreamProviderProcessorTestCase extends AbstractMuleTestCase {

  @Test
  public void readSingleByteOnOneByteStream() throws Exception {
    StreamProviderProcessor provider = new StreamProviderProcessor(1);
    InputStream stream = (InputStream) provider.process(testEvent()).getMessage().getPayload().getValue();
    assertThat(stream.read(), is(42));
    assertThat(stream.read(), is(END));
  }

  @Test
  public void readMultipleBytesOnOneByteStream() throws Exception {
    int size = 1;
    StreamProviderProcessor provider = new StreamProviderProcessor(size);
    InputStream stream = (InputStream) provider.process(testEvent()).getMessage().getPayload().getValue();
    byte[] data = new byte[size];
    assertThat(stream.read(data), is(size));
    assertArrayEquals("1".getBytes(), data);
    assertThat(stream.read(new byte[size]), is(END));
  }

  @Test
  public void readMultipleBytesWithOffsetOnOneByteStream() throws Exception {
    int size = 1;
    StreamProviderProcessor provider = new StreamProviderProcessor(size);
    InputStream stream = (InputStream) provider.process(testEvent()).getMessage().getPayload().getValue();
    byte[] data = new byte[size];
    assertThat(stream.read(data, 0, size), is(size));
    assertArrayEquals("1".getBytes(), data);
    assertThat(stream.read(new byte[size]), is(END));
  }

  @Test
  public void readMultipleBytes() throws Exception {
    int size = 1024;
    StreamProviderProcessor provider = new StreamProviderProcessor(size);
    InputStream stream = (InputStream) provider.process(testEvent()).getMessage().getPayload().getValue();
    byte[] data = new byte[size];
    assertThat(stream.read(data), is(size));
    assertArrayEquals(BYTES, data);
    assertThat(stream.read(), is(END));
  }

  @Test
  public void readMultipleBytesWithOffset() throws Exception {
    int size = 1024;
    StreamProviderProcessor provider = new StreamProviderProcessor(size);
    InputStream stream = (InputStream) provider.process(testEvent()).getMessage().getPayload().getValue();
    byte[] data = new byte[size];
    assertThat(stream.read(data, 0, 16), is(16));
    assertThat(stream.read(data, 16, 496), is(496));
    assertThat(stream.read(data, 512, size), is(512));
    assertThat(stream.read(data, 0, 0), is(END));
    assertArrayEquals(BYTES, data);
  }

  @Test
  public void readMultipleBytesOverSize() throws Exception {
    int size = 512;
    StreamProviderProcessor provider = new StreamProviderProcessor(size);
    InputStream stream = (InputStream) provider.process(testEvent()).getMessage().getPayload().getValue();
    byte[] data = new byte[1024];
    assertThat(stream.read(data), is(size));
    assertThat(stream.read(), is(END));
    assertArrayEquals(StringUtils.repeat(BYTES_PART, 4).getBytes(UTF_8), copyOfRange(data, 0, size));
  }

  @Test
  public void readMultipleBytesUnderSize() throws Exception {
    StreamProviderProcessor provider = new StreamProviderProcessor(1024);
    InputStream stream = (InputStream) provider.process(testEvent()).getMessage().getPayload().getValue();
    int readSize = 512;
    byte[] data1 = new byte[readSize];
    assertThat(stream.read(data1), is(readSize));
    byte[] data2 = new byte[readSize];
    assertThat(stream.read(data2), is(readSize));
    assertThat(stream.read(), is(END));
    assertArrayEquals(BYTES, addAll(data1, data2));
  }

  @Test
  public void readMultipleBytesMixedSize() throws Exception {
    int size = 1024;
    StreamProviderProcessor provider = new StreamProviderProcessor(size);
    InputStream stream = (InputStream) provider.process(testEvent()).getMessage().getPayload().getValue();
    byte[] data1 = new byte[528];
    assertThat(stream.read(data1), is(528));
    byte[] data2 = new byte[size];
    assertThat(stream.read(data2), is(496));
    assertThat(stream.read(), is(END));
    assertArrayEquals(BYTES, addAll(data1, copyOfRange(data2, 0, 496)));
  }

}
