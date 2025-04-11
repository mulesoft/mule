/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transaction.xa;

import org.mule.runtime.core.api.transaction.xa.ResourceManagerException;
import org.mule.runtime.core.api.util.UUID;

import jakarta.transaction.Status;

public abstract class AbstractTransactionContext {

  protected String id = UUID.getUUID();
  protected long timeout;
  protected int status;
  private boolean readOnly;
  protected boolean finished;

  protected AbstractTransactionContext() {
    status = Status.STATUS_NO_TRANSACTION;
    finished = false;
    readOnly = true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(id).append("[");
    sb.append(getStatusString());
    if (readOnly) {
      sb.append(", readonly");
    }
    if (finished) {
      sb.append(", finished");
    }
    sb.append("]");
    return sb.toString();
  }

  private String getStatusString() {return switch(status){case Status.STATUS_ACTIVE->"active";case Status.STATUS_MARKED_ROLLBACK->"marked rollback";case Status.STATUS_PREPARED->"prepared";case Status.STATUS_COMMITTED->"committed";case Status.STATUS_ROLLEDBACK->"rolled back";case Status.STATUS_UNKNOWN->"unknown";case Status.STATUS_NO_TRANSACTION->"no transaction";case Status.STATUS_PREPARING->"preparing";case Status.STATUS_COMMITTING->"committing";case Status.STATUS_ROLLING_BACK->"rolling back";default->"undefined status";};}

  public synchronized void finalCleanUp() throws ResourceManagerException {
    // nothing to do (yet?)
  }

  public synchronized void notifyFinish() {
    finished = true;
    notifyAll();
  }

  public abstract void doCommit() throws ResourceManagerException;

  public abstract void doRollback() throws ResourceManagerException;

  public void setStatus(int status) {
    this.status = status;
  }
}
