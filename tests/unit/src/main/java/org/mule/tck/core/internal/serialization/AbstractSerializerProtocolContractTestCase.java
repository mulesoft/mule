/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.core.internal.serialization;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.core.IsNull.nullValue;

import org.mule.runtime.api.serialization.SerializationProtocol;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public abstract class AbstractSerializerProtocolContractTestCase extends AbstractMuleTestCase {

  /**
   * This class is added to get rid of mockito spy, tried replacing it with mockito-inline, that still does not work in Java 17
   * and it still requires us to open java.base/java.io so that protected byte[] java.io.ByteArrayInputStream.buf can be accessed,
   * which we are trying to avoid at all costs"
   */
  public class CloseCounterInputStreamWrapper extends InputStream {

    private final InputStream inputStream;

    private final AtomicInteger closeCount = new AtomicInteger();

    public CloseCounterInputStreamWrapper(InputStream inputStream) {
      this.inputStream = inputStream;
    }

    @Override
    public int read() throws IOException {
      return inputStream.read();
    }

    @Override
    public void close() throws IOException {
      closeCount.incrementAndGet();
      inputStream.close();
    }

    public int getCloseCount() {
      return closeCount.intValue();
    }
  }

  private static final String STRING_MESSAGE = "Hello World";

  protected SerializationProtocol serializationProtocol;

  @Test(expected = NullPointerException.class)
  public final void nullBytes() throws Exception {
    serializationProtocol.deserialize((byte[]) null);
  }

  @Test(expected = NullPointerException.class)
  public final void nullStream() throws Exception {
    serializationProtocol.deserialize((InputStream) null);
  }

  @Test
  public final void nullObject() throws Exception {
    byte[] bytes = serializationProtocol.serialize(null);
    Object object = serializationProtocol.deserialize(bytes);
    assertThat(object, nullValue());
  }

  @Test
  public final void inputStreamClosed() throws Exception {
    final byte[] bytes = serializationProtocol.serialize(STRING_MESSAGE);

    CloseCounterInputStreamWrapper inputStream = new CloseCounterInputStreamWrapper(new ByteArrayInputStream(bytes));
    String output = serializationProtocol.deserialize(inputStream);

    assertThat(inputStream.getCloseCount(), greaterThanOrEqualTo(1));
    assertThat(output, equalTo(STRING_MESSAGE));
  }
}
