/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.compatibility.transport.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class HttpContentTypeTestCase extends FunctionalTestCase {

  private static final String EXPECTED_CONTENT_TYPE = "application/json; charset=UTF-8";

  @Rule
  public DynamicPort httpPort = new DynamicPort("httpPort");

  @Override
  protected String getConfigFile() {
    return "http-content-type-config.xml";
  }

  @Test
  public void returnsContentTypeInResponse() throws Exception {
    MuleClient client = muleContext.getClient();
    String url = String.format("http://localhost:%s/testInput", httpPort.getNumber());

    MuleMessage response = client.send(url, MuleMessage.builder().payload(TEST_MESSAGE).build()).getRight();

    assertContentType(response);
  }

  @Test
  public void sendsContentTypeOnRequest() throws Exception {
    MuleClient client = muleContext.getClient();
    String url = String.format("http://localhost:%s/requestClient", httpPort.getNumber());

    MuleMessage response = client.send(url, MuleMessage.builder().payload(TEST_MESSAGE).build()).getRight();

    assertThat(getPayloadAsString(response), equalTo(EXPECTED_CONTENT_TYPE));
  }

  private void assertContentType(MuleMessage response) {
    assertThat(response.getDataType().getMediaType().toRfcString(), equalTo(EXPECTED_CONTENT_TYPE));
  }
}
