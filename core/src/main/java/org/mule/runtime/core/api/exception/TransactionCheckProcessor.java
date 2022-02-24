/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.exception;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.TransactionCoordination;

import java.util.ArrayList;
import java.util.Map;

/**
 * Add javadoc
 *
 * @since 4.5
 */
public class TransactionCheckProcessor implements Processor {

  public static final Boolean TRANSACTION_CHECK_ENABLED = Boolean.getBoolean("mule.transaction.check.states");
  public static final String TRANSACTION_STATES_VARIABLE = "__transaction_states";
  public static final String LAST_TRANSACTION_STATE = "__last_tx_finish_state";
  public static final String ROLLBACK_TX = "rollback";
  public static final String CONTINUE_WITH_TX = "continue";
  public static final String COMMIT_TX = "commit";


  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    Map<String, TypedValue<?>> variables = event.getVariables();
    ArrayList<TransactionState> states;
    if (variables.containsKey(TRANSACTION_STATES_VARIABLE)) {
      states = (ArrayList<TransactionState>) variables.get(TRANSACTION_STATES_VARIABLE).getValue();
    } else {
      states = new ArrayList<>();
      event = CoreEvent.builder(event).addVariable(TRANSACTION_STATES_VARIABLE, states).build();
    }

    String lastState = null;
    if (variables.containsKey(LAST_TRANSACTION_STATE)) {
      lastState = variables.get(LAST_TRANSACTION_STATE).getValue().toString();
      if (!lastState.equals("continue")) {
        event = CoreEvent.builder(event).removeVariable(LAST_TRANSACTION_STATE).build();
      }
    }

    states.add(new TransactionState(TransactionCoordination.isTransactionActive(), lastState));
    return event;
  }

  public static class TransactionState {

    public final boolean isActive;
    public final String lastState;

    public TransactionState(boolean active, String lastState) {
      this.isActive = active;
      this.lastState = lastState;
    }

  }
}
