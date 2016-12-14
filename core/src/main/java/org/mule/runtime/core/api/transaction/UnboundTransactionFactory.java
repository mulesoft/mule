/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
