/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class OnErrorContinueEnricherTestCase extends AbstractIntegrationTestCase {

  public static class ErrorProcessor implements Processor {

    private static Throwable handled;

    @Override
    public Event process(Event event) throws MuleException {
      handled = event.getError().get().getCause();
      return event;
    }
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/on-error-continue-enricher.xml";
  }

  @Test
  public void testFlowRefHandlingException() throws Exception {
    Event event = flowRunner("enricherExceptionFlow").withPayload(TEST_PAYLOAD).run();
    event.getMessage();
    assertThat(ErrorProcessor.handled, not(nullValue()));
    assertThat(event.getError().isPresent(), is(false));
  }
}
