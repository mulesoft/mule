/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.processor.MessageProcessor;

import org.junit.Test;

public class OnErrorContinueEnricherTestCase extends AbstractIntegrationTestCase {

  public static class ErrorProcessor implements MessageProcessor {

    private static Throwable handled;

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException {
      handled = event.getError().getException();
      return event;
    }
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/on-error-continue-enricher.xml";
  }

  @Test
  public void testFlowRefHandlingException() throws Exception {
    MuleEvent event = flowRunner("enricherExceptionFlow").withPayload(getTestMuleMessage()).run();
    MuleMessage response = event.getMessage();
    assertThat(ErrorProcessor.handled, not(nullValue()));
    assertThat(event.getError(), nullValue());
  }
}
