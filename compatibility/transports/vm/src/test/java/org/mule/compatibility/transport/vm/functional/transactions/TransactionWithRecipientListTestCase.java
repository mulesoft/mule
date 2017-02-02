/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.vm.functional.transactions;

import static org.junit.Assert.assertEquals;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.message.InternalMessage;

import org.junit.Test;

public class TransactionWithRecipientListTestCase extends CompatibilityFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "vm/transaction-with-recipient-list-config.xml";
  }

  @Test
  public void testRecipientListRouterUseDefinedTransaction() throws Exception {
    MuleClient client = muleContext.getClient();

    InternalMessage response = client.send("vm://input", "test", null).getRight();
    assertEquals("test Received", getPayloadAsString(response));
  }
}
