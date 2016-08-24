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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.message.ExceptionMessage;

import org.hamcrest.core.Is;
import org.junit.Test;

public class DLQExceptionHandlerTestCase extends FunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-dlq-flow.xml";
  }

  @Test
  public void testDLQ() throws Exception {
    MuleClient client = muleContext.getClient();
    client.dispatch("jms://request.queue", "testing 1 2 3", null);

    assertThat(client.request("jms://out.queue", 3000).getRight().isPresent(), is(false));

    MuleMessage message = null;
    try {
      message = client.request("jms://DLQ", 20000).getRight().get();
    } catch (MuleException e) {
      e.printStackTrace(System.err);
    }
    assertNotNull(message);

    ExceptionMessage em = (ExceptionMessage) message.getPayload();
    assertEquals("testing 1 2 3", em.getPayload());
  }
}
