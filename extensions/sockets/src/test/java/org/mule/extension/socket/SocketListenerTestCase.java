/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket;

import org.mule.runtime.core.construct.Flow;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketListenerTestCase extends ParameterizedProtocolTestCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(SocketListenerTestCase.class);

  @Override
  protected String getConfigFile() {
    return "tcp-send-config.xml";
  }

  @Test
  public void stopAndRestart() throws Exception {
    muleContext.getRegistry().lookupObjects(Flow.class).forEach(flow -> {
      try {
        flow.stop();
        PollingProber prober = new PollingProber(100, 5000);
        prober.check(new JUnitLambdaProbe(() -> {
          try {
            flow.start();
          } catch (Exception e) {
            LOGGER.debug("Prober failed", e);
            return false;
          }

          return true;
        }));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    flowRunner("tcp-send").withPayload(testPojo).run();
    assertPojo(receiveConnection(), testPojo);
  }
}
