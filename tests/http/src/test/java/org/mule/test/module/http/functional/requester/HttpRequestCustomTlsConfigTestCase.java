/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.requester;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class HttpRequestCustomTlsConfigTestCase extends AbstractHttpRequestTestCase {

  @Override
  protected String getConfigFile() {
    return "http-request-custom-tls-config.xml";
  }

  @Override
  protected boolean enableHttps() {
    return true;
  }

  @Test
  public void configureTlsFromGlobalContext() throws Exception {
    flowRunner("testFlowGlobalContext").withPayload(TEST_MESSAGE).run();
    assertThat(body, equalTo(TEST_MESSAGE));
  }

  @Test
  public void configureTlsFromNestedContext() throws Exception {
    flowRunner("testFlowNestedContext").withPayload(TEST_MESSAGE).run();
    assertThat(body, equalTo(TEST_MESSAGE));
  }

}
