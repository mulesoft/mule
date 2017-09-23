/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.transaction.MuleTransactionConfig;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.privileged.transaction.xa.IllegalTransactionStateException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

@RunWith(Parameterized.class)
@SmallTest
public class ValidateTransactionalStateInterceptorTestCase extends AbstractMuleTestCase {

  public static final MuleTransactionConfig MULE_TRANSACTION_CONFIG_ALWAYS_BEGIN =
      new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
  public static final MuleTransactionConfig MULE_TRANSACTION_CONFIG_BEGIN_OR_JOIN =
      new MuleTransactionConfig(TransactionConfig.ACTION_BEGIN_OR_JOIN);
  public static final MuleTransactionConfig MULE_TRANSACTION_CONFIG_ALWAYS_JOIN =
      new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_JOIN);
  public static final MuleTransactionConfig MULE_TRANSACTION_CONFIG_INDIFFERENT =
      new MuleTransactionConfig(TransactionConfig.ACTION_INDIFFERENT);
  public static final MuleTransactionConfig MULE_TRANSACTION_CONFIG_JOIN_IF_POSSIBLE =
      new MuleTransactionConfig(TransactionConfig.ACTION_JOIN_IF_POSSIBLE);
  public static final MuleTransactionConfig MULE_TRANSACTION_CONFIG_NEVER =
      new MuleTransactionConfig(TransactionConfig.ACTION_NEVER);
  public static final MuleTransactionConfig MULE_TRANSACTION_CONFIG_NONE =
      new MuleTransactionConfig(TransactionConfig.ACTION_NONE);

  private static final Map<Boolean, Map<MuleTransactionConfig, Boolean>> resultMap =
      new HashMap<Boolean, Map<MuleTransactionConfig, Boolean>>();
  private boolean hasTransactionInContext;
  private TransactionConfig transactionConfig;
  private CoreEvent mockMuleEvent = Mockito.mock(CoreEvent.class);
  private Transaction mockTransaction = Mockito.mock(Transaction.class);

  @Parameterized.Parameters
  public static Collection<Object[]> parameters() {
    return Arrays
        .asList(new Object[][] {{false, MULE_TRANSACTION_CONFIG_ALWAYS_BEGIN}, {false, MULE_TRANSACTION_CONFIG_ALWAYS_JOIN},
            {false, MULE_TRANSACTION_CONFIG_BEGIN_OR_JOIN}, {false, MULE_TRANSACTION_CONFIG_INDIFFERENT},
            {false, MULE_TRANSACTION_CONFIG_JOIN_IF_POSSIBLE}, {false, MULE_TRANSACTION_CONFIG_NEVER},
            {false, MULE_TRANSACTION_CONFIG_NONE}, {true, MULE_TRANSACTION_CONFIG_ALWAYS_BEGIN},
            {true, MULE_TRANSACTION_CONFIG_ALWAYS_JOIN}, {true, MULE_TRANSACTION_CONFIG_BEGIN_OR_JOIN},
            {true, MULE_TRANSACTION_CONFIG_INDIFFERENT}, {true, MULE_TRANSACTION_CONFIG_JOIN_IF_POSSIBLE},
            {true, MULE_TRANSACTION_CONFIG_NEVER}, {true, MULE_TRANSACTION_CONFIG_NONE}});
  }

  static {
    HashMap<MuleTransactionConfig, Boolean> falseResultMap = new HashMap<MuleTransactionConfig, Boolean>();
    HashMap<MuleTransactionConfig, Boolean> trueResultMap = new HashMap<MuleTransactionConfig, Boolean>();
    resultMap.put(false, falseResultMap);
    resultMap.put(true, trueResultMap);
    falseResultMap.put(MULE_TRANSACTION_CONFIG_ALWAYS_BEGIN, false);
    falseResultMap.put(MULE_TRANSACTION_CONFIG_ALWAYS_JOIN, true);
    falseResultMap.put(MULE_TRANSACTION_CONFIG_BEGIN_OR_JOIN, false);
    falseResultMap.put(MULE_TRANSACTION_CONFIG_INDIFFERENT, false);
    falseResultMap.put(MULE_TRANSACTION_CONFIG_JOIN_IF_POSSIBLE, false);
    falseResultMap.put(MULE_TRANSACTION_CONFIG_NEVER, false);
    falseResultMap.put(MULE_TRANSACTION_CONFIG_NONE, false);

    trueResultMap.put(MULE_TRANSACTION_CONFIG_ALWAYS_BEGIN, false);
    trueResultMap.put(MULE_TRANSACTION_CONFIG_ALWAYS_JOIN, false);
    trueResultMap.put(MULE_TRANSACTION_CONFIG_BEGIN_OR_JOIN, false);
    trueResultMap.put(MULE_TRANSACTION_CONFIG_INDIFFERENT, false);
    trueResultMap.put(MULE_TRANSACTION_CONFIG_JOIN_IF_POSSIBLE, false);
    trueResultMap.put(MULE_TRANSACTION_CONFIG_NEVER, true);
    trueResultMap.put(MULE_TRANSACTION_CONFIG_NONE, false);
  }

  public ValidateTransactionalStateInterceptorTestCase(boolean hasTransactionInContext, TransactionConfig transactionConfig) {
    this.hasTransactionInContext = hasTransactionInContext;
    this.transactionConfig = transactionConfig;
  }

  @Before
  public void removeTransaction() {
    TransactionCoordination.getInstance().clear();
  }

  @Test
  public void testTransactionalState() throws Exception {
    boolean shouldThrowException = resultMap.get(hasTransactionInContext).get(transactionConfig);
    Exception thrownException = null;
    CoreEvent result = null;
    if (hasTransactionInContext) {
      TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    }
    ValidateTransactionalStateInterceptor<CoreEvent> interceptor =
        new ValidateTransactionalStateInterceptor<CoreEvent>(new ExecuteCallbackInterceptor<CoreEvent>(),
                                                             transactionConfig);
    try {
      result = interceptor.execute(new ExecutionCallback<CoreEvent>() {

        @Override
        public CoreEvent process() throws Exception {
          return mockMuleEvent;
        }
      }, new ExecutionContext());
    } catch (IllegalTransactionStateException e) {
      thrownException = e;
    }
    if (shouldThrowException) {
      assertThat(thrownException, IsNull.<Object>notNullValue());
      assertThat(thrownException, IsInstanceOf.instanceOf(IllegalTransactionStateException.class));
    } else {
      assertThat(result, Is.is(mockMuleEvent));
    }
  }
}
