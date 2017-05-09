/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.TestLegacyMessageUtils;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class ExceptionStrategyWithFlowExceptionTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-strategy-with-flow-exception.xml";
  }

  @Test
  public void testFlowExceptionExceptionStrategy() throws Exception {
    flowRunner("customException").withPayload(TEST_MESSAGE).dispatch();
    MuleClient client = muleContext.getClient();
    Message message = client.request("test://out", RECEIVE_TIMEOUT).getRight().get();

    assertThat(message, is(notNullValue()));
    assertThat(TestLegacyMessageUtils.getExceptionPayload(message), is(notNullValue()));
  }

  public static class ExceptionThrower implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      throw new MessagingException(event, null);
    }
  }
}
