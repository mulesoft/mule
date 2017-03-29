/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transactional.connection;

import org.mule.runtime.extension.api.connectivity.TransactionalConnection;

public interface TestTransactionalConnection extends TransactionalConnection {

  double getConnectionId();

  boolean isTransactionBegun();

  boolean isTransactionCommited();

  boolean isTransactionRolledback();

  void disconnect();

  boolean isConnected();
}
