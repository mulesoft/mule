/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
