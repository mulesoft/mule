/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.scripting.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.functional.junit4.FunctionalTestCase;

import org.junit.Test;

public class GroovyScriptFilterFunctionalTestCase extends FunctionalTestCase {

  public GroovyScriptFilterFunctionalTestCase() {
    // Groovy really hammers the startup time since it needs to create the
    // interpreter on every start
    setDisposeContextPerClass(true);

  }

  @Override
  protected String getConfigFile() {
    return "groovy-filter-config-flow.xml";
  }

  @Test
  public void testFilterScript() throws Exception {
    MuleClient client = muleContext.getClient();
    flowRunner("filterService").withPayload("hello").asynchronously().run();
    MuleMessage response = client.request("test://filterServiceTestOut", RECEIVE_TIMEOUT);
    assertNotNull(response);
    assertEquals("hello", response.getPayload());

    flowRunner("filterService").withPayload("1").asynchronously().run();
    response = client.request("test://filterServiceTestOut", RECEIVE_TIMEOUT);
    assertNull(response);
  }
}
