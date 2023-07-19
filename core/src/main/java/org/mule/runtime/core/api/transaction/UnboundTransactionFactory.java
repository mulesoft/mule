/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.transaction;

import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.MuleContext;

/**
 * Create an unbound transaction, to be bound and started by other parts of the transaction framework.
 */
public interface UnboundTransactionFactory {

  Transaction createUnboundTransaction(MuleContext muleContext) throws TransactionException;
}
