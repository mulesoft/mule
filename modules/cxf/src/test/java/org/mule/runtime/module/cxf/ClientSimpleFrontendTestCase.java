/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class ClientSimpleFrontendTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "aegis-conf-flow-httpn.xml";
  }

  @Test
  public void testEchoWsdl() throws Exception {
    MuleMessage result = flowRunner("PopulateData").withPayload("some payload").run().getMessage();

    assertNotNull(result.getPayload());
    assertEquals("Hello some payload", result.getPayload());
  }
}


