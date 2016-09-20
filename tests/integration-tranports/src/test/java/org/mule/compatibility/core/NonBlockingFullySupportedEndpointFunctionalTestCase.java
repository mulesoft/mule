/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.functional.functional.FlowAssert.verify;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.Event;

import org.junit.Ignore;
import org.junit.Test;

public class NonBlockingFullySupportedEndpointFunctionalTestCase extends CompatibilityFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "non-blocking-fully-supported-endpoint-test-config.xml";
  }

  @Test
  @Ignore("MULE-9731")
  public void testTransportOutboundEndpoint() throws Exception {
    final Event result = flowRunner("testOutboundEndpoint").withPayload(TEST_MESSAGE)
        .withExchangePattern(getMessageExchnagePattern()).nonBlocking().run();
    verify("testOutboundEndpoint");
    assertThat(result.getMessageAsString(muleContext), is(equalTo(TEST_MESSAGE)));
  }

  @Test
  @Ignore("MULE-9731")
  public void testTransportOutboundEndpointError() throws Exception {
    Event result = flowRunner("testOutboundEndpointError").withPayload(TEST_MESSAGE)
        .withExchangePattern(getMessageExchnagePattern()).nonBlocking().run();
    verify("testOutboundEndpointError");
    assertThat(result.getMessageAsString(muleContext), is(equalTo(TEST_MESSAGE)));
  }

  protected MessageExchangePattern getMessageExchnagePattern() {
    return MessageExchangePattern.REQUEST_RESPONSE;
  }
}
