/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("Session properties are not supported anymore")
public class SessionPropertiesTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort dynamicPort1 = new DynamicPort("port1");

  @Rule
  public DynamicPort dynamicPort2 = new DynamicPort("port2");

  @Rule
  public DynamicPort dynamicPort3 = new DynamicPort("port3");

  @Rule
  public DynamicPort dynamicPort4 = new DynamicPort("port4");

  @Rule
  public DynamicPort dynamicPort5 = new DynamicPort("port5");

  @Rule
  public DynamicPort dynamicPort6 = new DynamicPort("port6");

  @Override
  protected String getConfigFile() {
    return "session-properties.xml";
  }

  @Test
  public void testHttp1ToHttp2ToHttp3SessionPropertiesTestCase() throws Exception {
    MuleClient client = muleContext.getClient();

    Map<String, Serializable> properties = Collections.emptyMap();
    MuleMessage response =
        client.send("http://localhost:" + dynamicPort1.getNumber() + "/Flow1s1", "some message", properties).getRight();
    assertNotNullAndNotExceptionResponse(response);
  }

  @Test
  public void testHttp1ToHttp2ThenHttp1ToHttp3SessionPropertiesTestCase() throws Exception {
    MuleClient client = muleContext.getClient();

    Map<String, Serializable> properties = Collections.emptyMap();
    MuleMessage response =
        client.send("http://localhost:" + dynamicPort4.getNumber() + "/Flow1s2", "some message", properties).getRight();
    assertNotNullAndNotExceptionResponse(response);
  }

  private void assertNotNullAndNotExceptionResponse(MuleMessage response) {
    assertNotNull(response);
  }
}
