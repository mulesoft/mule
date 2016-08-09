/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.listener;


import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.test.module.http.functional.AbstractHttpTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class HttpListenerCustomTlsConfigTestCase extends AbstractHttpTestCase {

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");
  @Rule
  public DynamicPort port2 = new DynamicPort("port2");
  @Rule
  public DynamicPort port3 = new DynamicPort("port3");

  @Override
  protected String getConfigFile() {
    return "http-listener-custom-tls-config.xml";
  }

  @Test
  public void customTlsGlobalContext() throws Exception {
    final MuleEvent res = flowRunner("testFlowGlobalContextClient").withPayload("data").run();
    assertThat(getPayloadAsString(res.getMessage()), is("ok"));
  }

  @Test
  public void customTlsNestedContext() throws Exception {
    final MuleEvent res = flowRunner("testFlowNestedContextClient").withPayload("data").run();
    assertThat(getPayloadAsString(res.getMessage()), is("all right"));
  }

}
