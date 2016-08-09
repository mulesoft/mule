/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Ignore;
import org.junit.Test;

@Ignore("Currently not supported: Builders meant to be replaced by DW.")
public class HttpRequestBuilderCompositionTestCase extends AbstractHttpRequestTestCase {

  @Override
  protected String getConfigFile() {
    return "http-request-builder-composition-config.xml";
  }

  @Test
  public void parameterOverrideInRequestBuilderComposition() throws Exception {
    flowRunner("testFlow").withPayload(TEST_MESSAGE).run();

    assertThat(uri,
               equalTo("/testPath?queryParam1=testValue1&queryParam2=testValue2&queryParam2=newTestValue2&queryParam3=testValue3"));
    assertThat(getFirstReceivedHeader("testHeader1"), equalTo("headerValue1"));
    assertThat(getFirstReceivedHeader("testHeader2"), equalTo("headerValue2"));
  }


}


