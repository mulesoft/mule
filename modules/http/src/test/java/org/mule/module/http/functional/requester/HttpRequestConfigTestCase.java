/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static java.lang.String.valueOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.api.config.MuleProperties.MULE_HTTP_RESPONSE_BUFFER_SIZE;
import org.mule.module.http.internal.request.DefaultHttpRequesterConfig;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

public class HttpRequestConfigTestCase extends FunctionalTestCase
{

  private static final int RESPONSE_BUFFER_SIZE = 512;
  private static final int MAX_CONNECTIONS = 10;
  private static final int IDLE_TIMEOUT = 10000;

  @Rule
  public SystemProperty responseBufferSize = new SystemProperty(MULE_HTTP_RESPONSE_BUFFER_SIZE, valueOf(RESPONSE_BUFFER_SIZE));
  @Rule
  public SystemProperty maxConnections = new SystemProperty("maxConnections", valueOf(MAX_CONNECTIONS));
  @Rule
  public SystemProperty idleTimeout = new SystemProperty("idleTimeout", valueOf(IDLE_TIMEOUT));

  @Override
  protected String getConfigFile()
  {
    return "http-request-config.xml";
  }

  @Test
  public void verifyConfig() throws Exception
  {
    DefaultHttpRequesterConfig requestConfig = muleContext.getRegistry().lookupObject("requestConfig");
    assertThat(requestConfig.getResponseBufferSize(), equalTo(RESPONSE_BUFFER_SIZE));
    assertThat(requestConfig.getMaxConnections(), equalTo(MAX_CONNECTIONS));
    assertThat(requestConfig.getConnectionIdleTimeout(), equalTo(IDLE_TIMEOUT));
  }

}
