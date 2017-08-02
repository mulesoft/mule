/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.mule.TestTransactionFactory;

import org.junit.Test;

public class MuleTransactionConfigTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testActionAndStringConversion() {
    MuleTransactionConfig c = new MuleTransactionConfig();
    c.setMuleContext(muleContext);

    c.setAction(MuleTransactionConfig.ACTION_ALWAYS_BEGIN);
    assertEquals(MuleTransactionConfig.ACTION_ALWAYS_BEGIN_STRING, c.getActionAsString());

    c.setAction(MuleTransactionConfig.ACTION_ALWAYS_JOIN);
    assertEquals(MuleTransactionConfig.ACTION_ALWAYS_JOIN_STRING, c.getActionAsString());

    c.setAction(MuleTransactionConfig.ACTION_BEGIN_OR_JOIN);
    assertEquals(MuleTransactionConfig.ACTION_BEGIN_OR_JOIN_STRING, c.getActionAsString());

    c.setAction(MuleTransactionConfig.ACTION_JOIN_IF_POSSIBLE);
    assertEquals(MuleTransactionConfig.ACTION_JOIN_IF_POSSIBLE_STRING, c.getActionAsString());

    c.setAction(MuleTransactionConfig.ACTION_NONE);
    assertEquals(MuleTransactionConfig.ACTION_NONE_STRING, c.getActionAsString());

    c.setAction(MuleTransactionConfig.ACTION_INDIFFERENT);
    assertEquals(MuleTransactionConfig.ACTION_INDIFFERENT_STRING, c.getActionAsString());
  }

  @Test
  public void testDefaults() throws Exception {
    MuleTransactionConfig c = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
    c.setMuleContext(muleContext);
    assertEquals("Wrong default TX timeout", 30000, c.getTimeout());
  }

  @Test
  public void testTransactionJoinIfPossible() throws TransactionException {
    MuleTransactionConfig txConfig = new MuleTransactionConfig(TransactionConfig.ACTION_JOIN_IF_POSSIBLE);
    txConfig.setMuleContext(muleContext);
    txConfig.setFactory(new TestTransactionFactory());
    assertFalse(txConfig.isTransacted());
  }

  @Test
  public void testFailNoFactory() {
    MuleTransactionConfig txConfig = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
    txConfig.setMuleContext(muleContext);
    // note how we don't set a factory here so the default in MTC is null

    try {
      txConfig.isTransacted();
      fail("isTransacted() must fail if no factory is set");
    } catch (RuntimeException re) {
      // this was expected
    }
  }

}
