/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasItem;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_TYPE;

import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.test.module.http.functional.AbstractHttpTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class HttpRequestLaxContentTypeTestCase extends AbstractHttpTestCase {

  @Rule
  public DynamicPort httpPort = new DynamicPort("httpPort");

  @Override
  protected String getConfigFile() {
    return "http-request-lax-content-type-config.xml";
  }

  @Test
  public void sendsInvalidContentTypeOnRequest() throws Exception {
    MuleClient client = muleContext.getClient();
    final String url = String.format("http://localhost:%s/requestClientInvalid", httpPort.getNumber());

    MuleMessage response = client.send(url, TEST_MESSAGE, null).getRight();

    assertNoContentTypeProperty(response);
    assertThat(getPayloadAsString(response), equalTo("invalidMimeType"));
  }

  private void assertNoContentTypeProperty(MuleMessage response) {
    assertThat(response.getInboundPropertyNames(), not(hasItem(equalToIgnoringCase(CONTENT_TYPE))));
  }
}
