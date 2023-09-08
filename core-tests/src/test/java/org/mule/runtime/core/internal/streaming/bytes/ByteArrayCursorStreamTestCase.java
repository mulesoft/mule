/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.BYTES_STREAMING;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(STREAMING)
@Story(BYTES_STREAMING)
public class ByteArrayCursorStreamTestCase extends AbstractMuleTestCase {

  private static final byte[] DATA = "hello".getBytes();

  private InputStream byteArrayCursorStream;

  private byte[] buffer;

  @Before
  public void prepareStream() {
    CursorStreamProvider provider = mock(ByteArrayCursorStreamProvider.class);
    byteArrayCursorStream = new ByteArrayCursorStream(provider, DATA);
    buffer = new byte[DATA.length];
  }

  @Test
  @Description("Simplest case, read the whole data at once")
  public void readCompleteStream() throws IOException {
    byteArrayCursorStream.read(buffer, 0, DATA.length);
    assertThat(buffer, is(DATA));
  }

  @Test
  @Issue("MULE-18236")
  @Description("According to javadoc, when reading zero bytes in a non empty InputStream, it should return 0")
  public void readZeroBytes() throws IOException {
    int read = byteArrayCursorStream.read(buffer, 0, 0);
    assertThat(read, is(0));
  }

  @Test
  @Issue("MULE-18236")
  @Description("When read one byte at time and it reach the end of the InputStream it should be consistent with javadoc")
  public void readOneByteAtTime() throws IOException {
    int count = 0;
    while (count < DATA.length) {
      int read = byteArrayCursorStream.read(buffer, count, 1);
      assertThat(read, is(1));
      assertThat(buffer[count], is(DATA[count]));
      count++;
    }
    assertThat(byteArrayCursorStream.available(), is(0));

    int read = byteArrayCursorStream.read(buffer, count, 0);
    assertThat(read, is(0));

    read = byteArrayCursorStream.read(buffer, count, 1);
    assertThat(read, is(-1));
  }
}
