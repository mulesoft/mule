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

import org.mule.compatibility.core.api.FutureMessageResult;
import org.mule.compatibility.module.client.MuleClient;
import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;

import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HttpConnectionTimeoutTestCase extends CompatibilityFunctionalTestCase {

  private MuleClient client;

  @Override
  protected String getConfigFile() {
    return "http-connection-timeout-config.xml";
  }

  @Before
  public void before() throws MuleException {
    client = new MuleClient(muleContext);
  }

  @After
  public void after() {
    client.dispose();
  }

  @Test
  public void usesConnectionTimeout() throws Exception {
    FutureMessageResult result = client.sendAsync("vm://testInput", TEST_MESSAGE, null);

    InternalMessage message = null;
    try {
      message = result.getResult(1000).getRight();
    } catch (TimeoutException e) {
      fail("Connection timeout not honored.");
    }

    assertThat(message.getPayload().getValue(), is(nullValue()));
    assertNotNull(message.getExceptionPayload());
  }
}
