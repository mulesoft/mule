/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transaction;

import static org.mule.runtime.core.internal.transaction.MuleTransactionConfig.ACTION_ALWAYS_BEGIN;
import static org.mule.runtime.core.internal.transaction.MuleTransactionConfig.ACTION_ALWAYS_BEGIN_STRING;
import static org.mule.runtime.core.internal.transaction.MuleTransactionConfig.ACTION_ALWAYS_JOIN;
import static org.mule.runtime.core.internal.transaction.MuleTransactionConfig.ACTION_ALWAYS_JOIN_STRING;
import static org.mule.runtime.core.internal.transaction.MuleTransactionConfig.ACTION_BEGIN_OR_JOIN;
import static org.mule.runtime.core.internal.transaction.MuleTransactionConfig.ACTION_BEGIN_OR_JOIN_STRING;
import static org.mule.runtime.core.internal.transaction.MuleTransactionConfig.ACTION_INDIFFERENT;
import static org.mule.runtime.core.internal.transaction.MuleTransactionConfig.ACTION_INDIFFERENT_STRING;
import static org.mule.runtime.core.internal.transaction.MuleTransactionConfig.ACTION_JOIN_IF_POSSIBLE;
import static org.mule.runtime.core.internal.transaction.MuleTransactionConfig.ACTION_JOIN_IF_POSSIBLE_STRING;
import static org.mule.runtime.core.internal.transaction.MuleTransactionConfig.ACTION_NONE;
import static org.mule.runtime.core.internal.transaction.MuleTransactionConfig.ACTION_NONE_STRING;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.privileged.transaction.TransactionFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.mule.TestTransactionFactory;

import org.junit.Test;

public class MuleTransactionConfigTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testActionAndStringConversion() {
    MuleTransactionConfig c = new MuleTransactionConfig();
    c.setMuleContext(muleContext);

    c.setAction(ACTION_ALWAYS_BEGIN);
    assertEquals(ACTION_ALWAYS_BEGIN_STRING, c.getActionAsString());

    c.setAction(ACTION_ALWAYS_JOIN);
    assertEquals(ACTION_ALWAYS_JOIN_STRING, c.getActionAsString());

    c.setAction(ACTION_BEGIN_OR_JOIN);
    assertEquals(ACTION_BEGIN_OR_JOIN_STRING, c.getActionAsString());

    c.setAction(ACTION_JOIN_IF_POSSIBLE);
    assertEquals(ACTION_JOIN_IF_POSSIBLE_STRING, c.getActionAsString());

    c.setAction(ACTION_NONE);
    assertEquals(ACTION_NONE_STRING, c.getActionAsString());

    c.setAction(ACTION_INDIFFERENT);
    assertEquals(ACTION_INDIFFERENT_STRING, c.getActionAsString());
  }

  @Test
  public void testDefaults() throws Exception {
    MuleTransactionConfig c = new MuleTransactionConfig(ACTION_ALWAYS_BEGIN);
    c.setMuleContext(muleContext);
    assertEquals("Wrong default TX timeout", 30000, c.getTimeout());
  }

  @Test
  public void testTransactionJoinIfPossible() throws TransactionException {
    MuleTransactionConfig txConfig = new MuleTransactionConfig(ACTION_JOIN_IF_POSSIBLE);
    txConfig.setMuleContext(muleContext);
    txConfig.setFactory(new TestTransactionFactory());
    assertFalse(txConfig.isTransacted());
  }

  @Test
  public void testFailNoFactory() {
    MuleTransactionConfig txConfig = new MuleTransactionConfig(ACTION_ALWAYS_BEGIN);
    txConfig.setMuleContext(muleContext);
    // note how we don't set a factory here so the default in MTC is null

    try {
      txConfig.isTransacted();
      fail("isTransacted() must fail if no factory is set");
    } catch (RuntimeException re) {
      // this was expected
    }
  }

  @Test
  public void testEquals() {
    MuleTransactionConfig config = new MuleTransactionConfig();
    assertThat(config.equals(config), is(true));
    assertThat(config.equals(null), is(false));
    assertThat(config.equals("foo"), is(false));
    MuleTransactionConfig config2 = new MuleTransactionConfig();

    TransactionFactory factory = new TestTransactionFactory();
    config.setFactory(factory);
    config2.setFactory(factory);
    config.setActionAsString(ACTION_ALWAYS_BEGIN_STRING);
    config2.setActionAsString(ACTION_ALWAYS_BEGIN_STRING);

    assertThat(config.equals(config2), is(true));
  }

  @Test(expected = MuleRuntimeException.class)
  public void failingWithoutFactory() {
    MuleTransactionConfig config = new MuleTransactionConfig();
    config.setActionAsString(ACTION_ALWAYS_BEGIN_STRING);
    config.isTransacted();
  }
}
