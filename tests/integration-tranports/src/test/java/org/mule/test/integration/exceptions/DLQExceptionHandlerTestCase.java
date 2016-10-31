/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.message.ExceptionMessage;

import org.junit.Test;

public class DLQExceptionHandlerTestCase extends CompatibilityFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-dlq-flow.xml";
  }

  @Test
  public void testDLQ() throws Exception {
    MuleClient client = muleContext.getClient();
    client.dispatch("jms://request.queue", "testing 1 2 3", null);

    assertThat(client.request("jms://out.queue", 3000).getRight().isPresent(), is(false));

    InternalMessage message = null;
    try {
      message = client.request("jms://DLQ", 20000).getRight().get();
    } catch (MuleException e) {
      e.printStackTrace(System.err);
    }
    assertNotNull(message);

    ExceptionMessage em = (ExceptionMessage) message.getPayload().getValue();
    assertEquals("testing 1 2 3", em.getPayload());
  }
}
