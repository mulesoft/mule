/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.streaming;

import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class CloseStreamOnMuleExceptionTestCase extends AbstractIntegrationTestCase {

  private final int timeoutMs = 3000;
  private static Latch inputStreamLatch = new Latch();
  private static Latch streamReaderLatch;
  private String xmlText = "<test attribute=\"1\"/>";
  private TestByteArrayInputStream inputStream;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/streaming/close-stream-on-mule-exception-test-flow.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    inputStream = new TestByteArrayInputStream(xmlText.getBytes());
    streamReaderLatch = new Latch();
  }

  @Test
  public void testCloseStreamOnComponentException() throws Exception {
    flowRunner("echo").withPayload(inputStream).dispatch();

    streamReaderLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
    assertTrue(inputStream.isClosed());
  }

  @Test
  public void testCloseStreamOnInboundFilterException() throws Exception {
    flowRunner("inboundFilterExceptionBridge").withPayload(inputStream).dispatch();

    verifyInputStreamIsClosed(inputStream);
  }

  private void verifyInputStreamIsClosed(final ClosableInputStream is) {
    final PollingProber pollingProber = new PollingProber(timeoutMs, 100);
    pollingProber.check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return is.isClosed();
      }

      @Override
      public String describeFailure() {
        return "Input stream was never closed";
      }
    });
  }

  interface ClosableInputStream {

    boolean isClosed();
  }

  static class TestByteArrayInputStream extends ByteArrayInputStream implements ClosableInputStream {

    private boolean closed;

    @Override
    public boolean isClosed() {
      return closed;
    }

    public TestByteArrayInputStream(byte[] arg0) {
      super(arg0);
    }

    public TestByteArrayInputStream(byte[] buf, int offset, int length) {
      super(buf, offset, length);
    }

    @Override
    public void close() throws IOException {
      super.close();
      closed = true;
      inputStreamLatch.countDown();
    }
  }
}
