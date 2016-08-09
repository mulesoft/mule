/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.module.http.functional.requester;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONNECTION;
import static org.mule.runtime.module.http.api.HttpHeaders.Values.CLOSE;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

public class HttpRequestTlsConnectionCloseTestCase extends AbstractHttpRequestTestCase {

  @Override
  protected String getConfigFile() {
    return "http-request-connection-close-config.xml";
  }

  @Override
  protected boolean enableHttps() {
    return true;
  }

  @Override
  protected void writeResponse(HttpServletResponse response) throws IOException {
    super.writeResponse(response);
    response.addHeader(CONNECTION, CLOSE);
  }

  @Test
  public void handlesRequest() throws Exception {
    MuleEvent response = flowRunner("testFlowHttps").withPayload(TEST_PAYLOAD).run();
    assertThat(IOUtils.toString((InputStream) response.getMessage().getPayload()), is(DEFAULT_RESPONSE));
  }
}
