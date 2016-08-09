/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.compatibility.transport.http;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.mule.compatibility.module.client.MuleClient;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.FutureMessageResult;
import org.mule.runtime.core.api.MuleMessage;

import java.util.concurrent.TimeoutException;

import org.junit.Test;

public class HttpConnectionTimeoutTestCase extends FunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "http-connection-timeout-config.xml";
  }

  @Test
  public void usesConnectionTimeout() throws Exception {
    final MuleClient client = new MuleClient(muleContext);
    FutureMessageResult result = client.sendAsync("vm://testInput", TEST_MESSAGE, null);

    MuleMessage message = null;
    try {
      message = result.getMessage(1000);
    } catch (TimeoutException e) {
      fail("Connection timeout not honored.");
    }

    assertThat(message.getPayload(), is(nullValue()));
    assertNotNull(message.getExceptionPayload());
  }
}
