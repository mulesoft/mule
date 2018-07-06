/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import static org.apache.commons.lang3.ArrayUtils.addAll;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static org.mule.functional.api.component.StreamProviderProcessor.BYTES;
import static org.mule.runtime.api.message.Message.of;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Test;

public class StreamConsumerProcessorTestCase extends AbstractMuleTestCase {

  private static final byte[] PAYLOAD = addAll(BYTES, BYTES);

  @Test
  public void returnsAllData() throws Exception {
    consumeAndValidate(false, PAYLOAD);
  }

  @Test
  public void returnsFixedSizedData() throws Exception {
    consumeAndValidate(true, BYTES);
  }

  private void consumeAndValidate(boolean chunked, byte[] expectedResult) throws MuleException {
    StreamConsumerProcessor consumer = new StreamConsumerProcessor(chunked);
    CloseableByteArrayInputStream stream = new CloseableByteArrayInputStream(PAYLOAD);
    CoreEvent event = CoreEvent.builder(testEvent()).message(of(stream)).build();
    byte[] result = (byte[]) consumer.process(event).getMessage().getPayload().getValue();
    assertArrayEquals(expectedResult, result);
    assertThat(stream.isClosed(), is(true));
  }

  private class CloseableByteArrayInputStream extends ByteArrayInputStream {

    private boolean closed = false;

    public CloseableByteArrayInputStream(byte[] buf) {
      super(buf);
    }

    @Override
    public void close() throws IOException {
      super.close();
      this.closed = true;
    }

    public boolean isClosed() {
      return closed;
    }
  }

}
