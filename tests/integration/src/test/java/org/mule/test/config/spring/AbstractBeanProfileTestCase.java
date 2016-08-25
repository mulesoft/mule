/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

public abstract class AbstractBeanProfileTestCase extends AbstractIntegrationTestCase {

  protected String getConfigFile(String profile) {
    System.setProperty("spring.profiles.active", profile);
    return "org/mule/test/integration/spring/bean-profiles-config.xml";
  }

  public void profile(String appended) throws Exception {
    flowRunner("service").withPayload("Homero").run();
    MuleClient client = muleContext.getClient();
    MuleMessage response = client.request("test://out", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull("Response is null", response);
    assertEquals("Homero" + appended, response.getPayload());
  }
}
