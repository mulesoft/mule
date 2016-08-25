/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.resolvers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.util.Base64;

import java.util.List;
import java.util.Random;

import org.junit.Test;

public class DefaultEntryPointResolverSetMultithreadingTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/resolvers/default-entry-point-resolver-multithreading-test-config.xml";
  }

  @Override
  public int getTestTimeoutSecs() {
    return 120;
  }

  @Test
  public void testMultithreaded() throws Exception {
    final int numberOfThreads = 50;
    final int requestCount = 100;
    ClientRequest[] clients = new ClientRequest[numberOfThreads];
    for (int i = 0; i < numberOfThreads; i++) {
      clients[i] = new ClientRequest(requestCount);
    }

    for (ClientRequest clientRequest : clients) {
      clientRequest.start();
      try {
        Thread.sleep(5);
      } catch (InterruptedException ie) {
        // ignore
      }
    }

    for (int i = 0; i < numberOfThreads; i++) {
      try {
        clients[i].join();
      } catch (InterruptedException ie) {
        // ignore
      }
    }
  }

  private class ClientRequest extends Thread {

    final MuleClient client;
    int requestCount;

    private ClientRequest(final int requestCount) throws MuleException {
      client = muleContext.getClient();
      this.requestCount = requestCount;
    }

    @Override
    public void run() {
      final byte[] payload = createPayload();

      while (--requestCount >= 0) {
        try {
          MuleEvent event = flowRunner("flowTestSync").withPayload(payload).run();
          final MuleMessage outbound = event.getMessage();
          assertNull(event.getError());
          assertNotNull(outbound.getPayload());
          byte[] bytes = null;
          if (outbound.getPayload() instanceof byte[]) {
            bytes = (byte[]) outbound.getPayload();
          } else if (outbound.getPayload() instanceof List) {
            final List<?> list = (List<?>) outbound.getPayload();
            assertEquals(1, list.size());
            assertTrue(list.get(0) instanceof byte[]);
            bytes = (byte[]) list.get(0);
          } else {
            fail("unexpected payload type");
          }
          assertEquals(Base64.encodeBytes(payload), Base64.encodeBytes(bytes));
        } catch (Exception e) {
          fail("failed with exception: " + e);
        }
      }
    }

    private byte[] createPayload() {
      Random random = new Random();
      final int size = 55;
      byte[] payload = new byte[size];
      random.nextBytes(payload);
      return payload;
    }
  }

  public static class EchoBytes {

    public byte[] echo(byte[] input) {
      return input;
    }
  }
}
