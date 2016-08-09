/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.module.http.functional.requester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mule.runtime.core.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

public class HttpRequestContentTypeTestCase extends AbstractHttpRequestTestCase {

  private static final String EXPECTED_CONTENT_TYPE = "application/json; charset=UTF-8";

  @Rule
  public SystemProperty strictContentType =
      new SystemProperty(SYSTEM_PROPERTY_PREFIX + "strictContentType", Boolean.TRUE.toString());

  @Override
  protected String getConfigFile() {
    return "http-request-content-type-config.xml";
  }

  @Test
  public void sendsContentTypeOnRequest() throws Exception {
    verifyContentTypeForFlow("requesterContentType");
  }

  @Test
  public void sendsContentTypeOnRequestBuilder() throws Exception {
    verifyContentTypeForFlow("requesterBuilderContentType");
  }

  public void verifyContentTypeForFlow(String flowName) throws Exception {
    flowRunner(flowName).withPayload(TEST_MESSAGE).run().getMessage();

    assertThat(getFirstReceivedHeader(CONTENT_TYPE.toLowerCase()), equalTo(EXPECTED_CONTENT_TYPE));
  }
}
