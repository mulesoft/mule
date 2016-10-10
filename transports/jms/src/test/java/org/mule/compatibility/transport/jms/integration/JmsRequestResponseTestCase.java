/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms.integration;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.functional.functional.FlowAssert.verify;
import org.mule.runtime.core.api.message.InternalMessage;

import org.junit.Test;

public class JmsRequestResponseTestCase extends AbstractJmsFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "integration/jms-request-response.xml";
  }


  @Test
  public void testNotSendiningCorrelationIDWithTemporaryQueue() throws Exception {
    InternalMessage response =
        flowRunner("JMSNoCorrelationIDTemporaryQueue").withPayload(TEST_MESSAGE).run().getMessage();
    verify("JMSNoCorrelationIDTemporaryQueue");
    verify("JMSNoCorrelationIDTarget");
    assertEchoResponse(response);
  }

  @Test
  public void testNotSendiningCorrelationIDWithFixedQueue() throws Exception {
    InternalMessage response =
        flowRunner("JMSNoCorrelationIDFixedQueue").withPayload(TEST_MESSAGE).run().getMessage();
    verify("JMSNoCorrelationIDFixedQueue");
    verify("JMSNoCorrelationIDTarget");
    assertEchoResponse(response);
  }

  @Test
  public void testSendiningCorrelationIDWithTemporaryQueue() throws Exception {
    InternalMessage response =
        flowRunner("JMSCorrelationIDTemporaryQueue").withPayload(TEST_MESSAGE).run().getMessage();
    verify("JMSCorrelationIDTemporaryQueue");
    verify("JMSCorrelationIDTarget");
    assertFixedEchoResponse(response);
  }

  @Test
  public void testSendiningCorrelationIDWithFixedQueue() throws Exception {
    InternalMessage response =
        flowRunner("JMSCorrelationIDFixedQueue").withPayload(TEST_MESSAGE).run().getMessage();
    verify("JMSCorrelationIDFixedQueue");
    verify("JMSCorrelationIDTarget");
    assertFixedEchoResponse(response);
  }

  private void assertEchoResponse(InternalMessage response) throws Exception {
    assertThat(response.getPayload().getValue(), equalTo(TEST_MESSAGE + " " + "JMSNoCorrelationIDTarget"));
  }

  private void assertFixedEchoResponse(InternalMessage response) throws Exception {
    assertThat(response.getPayload().getValue(), equalTo(TEST_MESSAGE + " " + "JMSCorrelationIDTarget"));
  }
}
