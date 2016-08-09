/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket;

import static org.junit.Assert.assertEquals;
import org.mule.functional.junit4.runners.RunnerDelegateTo;
import org.mule.runtime.core.util.IOUtils;

import java.io.InputStream;

import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class TcpMultipleSendTestCase extends ParameterizedProtocolTestCase {

  @Override
  protected String getConfigFile() {
    return "tcp-multiple-send-config.xml";
  }

  @Test
  public void multipleSendString() throws Exception {
    // TODO repeat this test with different messages and test that they arrive in the same order
    for (int i = 0; i < REPETITIONS; i++) {
      InputStream payload = (InputStream) flowRunner("tcp-send").withPayload(TEST_STRING).run().getMessage().getPayload();

      assertEquals(RESPONSE_TEST_STRING, IOUtils.toString(payload));
    }
  }
}
