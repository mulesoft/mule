/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.transaction.xa;

import org.mule.runtime.core.api.transaction.xa.ResourceManagerException;

/**
 * Base transaction context for XA transactions
 */
public abstract class AbstractXaTransactionContext extends AbstractTransactionContext {

  /**
   * Two phase commit prepare phase
   *
   * @throws ResourceManagerException
   */
  public abstract void doPrepare() throws ResourceManagerException;

}
