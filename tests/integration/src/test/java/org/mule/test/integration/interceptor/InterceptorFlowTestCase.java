/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import org.junit.Test;

public class InterceptorFlowTestCase extends AbstractIntegrationTestCase {

  @Test
  public void testDefaultJavaComponentShortcut() throws Exception {
    flowRunner("interceptorFlow").withPayload(getTestMuleMessage(TEST_PAYLOAD)).asynchronously().run();
    MuleClient client = muleContext.getClient();
    MuleMessage message = client.request("test://out", 3000).getRight().get();
    assertNotNull(message);
    String payload = (String) message.getPayload();
    assertNotNull(payload);
    // note that there is an exclamation mark on the end that was added by the interceptor
    assertEquals(TEST_PAYLOAD + "!", payload);
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/interceptor-flow.xml";
  }
}
